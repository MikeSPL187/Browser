package dev.sk2andy.materialbrowser.data

import android.app.DownloadManager
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.actions.BrowserDownloadManager
import dev.sk2andy.materialbrowser.browser.actions.DownloadActionResult
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var resolver: android.content.ContentResolver
    private lateinit var repository: DownloadRepository
    private val mediaUris = mutableListOf<Uri>()
    private val managerIds = mutableListOf<Long>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        resolver = context.contentResolver
        repository = DownloadRepository(context)
    }

    @After
    fun cleanFixtures() {
        DownloadRuntimeRegistry.completed(42)
        mediaUris.forEach { uri -> runCatching { resolver.delete(uri, null, null) } }
        if (managerIds.isNotEmpty()) {
            context.getSystemService(DownloadManager::class.java)
                .remove(*managerIds.toLongArray())
        }
    }

    @Test
    fun readsAndClearsCompletedGeckoMediaStoreDownload() {
        val name = "candy-gecko-${UUID.randomUUID()}.txt"
        val uri = requireNotNull(
            resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                },
            ),
        )
        mediaUris += uri
        resolver.openOutputStream(uri, "w")!!.use { output ->
            output.write("Candy download".encodeToByteArray())
        }

        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
            null,
            null,
        )
        repository = DownloadRepository(context, ownerPackageName = ownerPackage(uri))
        val completed = repository.snapshot().single { entry -> entry.name == name }
        assertEquals(DownloadStatus.Successful, completed.status)
        assertNotNull(repository.contentUri(completed))
        assertTrue(repository.clear(listOf(completed)))
        assertFalse(repository.snapshot().any { entry -> entry.name == name })
        mediaUris.remove(uri)
    }

    @Test
    fun readsAndroidDownloadManagerEntryWithSourceAndNamespacedId() {
        val name = "candy-system-${UUID.randomUUID()}.bin"
        val source = "https://downloads.example.invalid/$name"
        val manager = context.getSystemService(DownloadManager::class.java)
        val id = manager.enqueue(
            DownloadManager.Request(Uri.parse(source))
                .setTitle(name)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name),
        )
        managerIds += id

        val entry = repository.snapshot().single { candidate -> candidate.id == id }
        assertEquals(name, entry.name)
        assertEquals(source, entry.source)
        assertTrue(entry.id >= 0L)
    }

    @Test
    fun cancelsPendingSystemDownloadButRejectsTerminalSnapshot() {
        val manager = context.getSystemService(DownloadManager::class.java)
        val name = "candy-cancel-${UUID.randomUUID()}.bin"
        val id = manager.enqueue(
            DownloadManager.Request(Uri.parse("https://downloads.example.invalid/$name"))
                .setTitle(name)
                .setAllowedNetworkTypes(0),
        )
        managerIds += id
        val entry = repository.snapshot().single { it.id == id }

        assertTrue(repository.cancel(entry))
        assertFalse(repository.snapshot().any { it.id == id })
        assertFalse(repository.cancel(entry))
        managerIds.remove(id)
    }

    @Test
    fun failedSystemDownloadStartsAgainAndGivesWayToTheNewRow() {
        val name = "candy-retry-${UUID.randomUUID()}.bin"
        val body = ByteArray(16 * 1024) { index -> (index % 251).toByte() }
        // Android's download manager gives up on a 404 at once; the second request gets the file.
        val server = FixtureServer(
            "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".encodeToByteArray(),
            (
                "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n" +
                    "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                ).encodeToByteArray() + body,
        )
        server.use {
            val result = BrowserDownloadManager(context).enqueue(
                BrowserDownloadRequest(
                    url = "http://127.0.0.1:${server.port}/$name",
                    fileName = name,
                    mimeType = "application/octet-stream",
                    userAgent = "CandyRetryTest/1.0",
                ),
            )
            val failedId = (result as DownloadActionResult.Enqueued).id
            managerIds += failedId
            val failed = awaitEntry { entry -> entry.id == failedId && entry.status == DownloadStatus.Failed }
            assertEquals(DownloadRetry.Available, failed.retry)

            assertTrue(repository.retry(failed))

            val saved = awaitEntry { entry -> entry.name == name && entry.status == DownloadStatus.Successful }
            managerIds += saved.id
            assertFalse("the failed row stayed", repository.snapshot().any { entry -> entry.id == failedId })
            managerIds.remove(failedId)
            assertEquals(body.size.toLong(), saved.bytes)
            assertEquals(2, server.requests.size)
            assertTrue(server.requests.all { request -> request.startsWith("GET /$name ") })
            assertFalse("a saved row does not start again", repository.retry(saved))
        }
    }

    @Test
    fun systemDownloadFromBeforeThisRunDoesNotStartAgain() {
        val manager = context.getSystemService(DownloadManager::class.java)
        val name = "candy-unknown-${UUID.randomUUID()}.bin"
        val server = FixtureServer(
            "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".encodeToByteArray(),
        )
        server.use {
            // Enqueued around the browser, as rows from before a restart look to it.
            val id = manager.enqueue(
                DownloadManager.Request(Uri.parse("http://127.0.0.1:${server.port}/$name"))
                    .setTitle(name)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name),
            )
            managerIds += id
            val failed = awaitEntry { entry -> entry.id == id && entry.status == DownloadStatus.Failed }

            assertEquals(DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered), failed.retry)
            assertFalse(repository.retry(failed))
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun readsLiveGeckoProgressFromProcessRegistry() {
        DownloadRuntimeRegistry.started(
            id = 42,
            name = "active-gecko.bin",
            source = "https://downloads.example/active-gecko.bin",
            mime = "application/octet-stream",
            total = 1_000L,
            startedAt = 100L,
        )
        DownloadRuntimeRegistry.progress(id = 42, bytes = 400L, total = 1_000L, updatedAt = 200L)

        val active = repository.snapshot().single { entry -> entry.name == "active-gecko.bin" }
        assertEquals(DownloadStatus.Running, active.status)
        assertEquals(400L, active.bytes)
        assertEquals(1_000L, active.total)

        DownloadRuntimeRegistry.completed(42)
    }

    @Test
    fun mergesLiveRegistryProgressWithItsPendingMediaStoreRow() {
        val name = "active-gecko-${UUID.randomUUID()}.bin"
        val uri = requireNotNull(
            resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                },
            ),
        )
        mediaUris += uri
        val mediaStoreId = ContentUris.parseId(uri)
        repository = DownloadRepository(context, ownerPackageName = ownerPackage(uri))
        DownloadRuntimeRegistry.started(
            id = 42,
            name = name,
            source = "https://downloads.example/$name",
            mime = "application/octet-stream",
            total = 1_000L,
            startedAt = System.currentTimeMillis(),
            mediaStoreId = mediaStoreId,
        )
        DownloadRuntimeRegistry.progress(
            id = 42,
            bytes = 400L,
            total = 1_000L,
            updatedAt = System.currentTimeMillis(),
        )

        val matches = repository.snapshot().filter { entry -> entry.name == name }
        assertEquals(1, matches.size)
        assertEquals(DownloadStatus.Running, matches.single().status)
        assertEquals(400L, matches.single().bytes)

        DownloadRuntimeRegistry.failed(42, cancelled = true, updatedAt = System.currentTimeMillis())
        assertTrue(repository.clear(DownloadRuntimeRegistry.snapshot()))
        assertFalse(repository.snapshot().any { entry -> entry.name == name })
        mediaUris.remove(uri)
    }

    private fun awaitEntry(condition: (DownloadEntry) -> Boolean): DownloadEntry {
        val deadline = System.currentTimeMillis() + 30_000L
        while (System.currentTimeMillis() < deadline) {
            repository.snapshot().firstOrNull(condition)?.let { entry -> return entry }
            Thread.sleep(200)
        }
        return checkNotNull(repository.snapshot().firstOrNull(condition)) {
            "no such download: ${repository.snapshot()}"
        }
    }

    /** Answers one connection after another with [responses], each on its own thread. */
    private class FixtureServer(vararg responses: ByteArray) : AutoCloseable {
        private val socket = ServerSocket(0)
        val port: Int = socket.localPort
        val requests: MutableList<String> = CopyOnWriteArrayList()

        init {
            Thread {
                runCatching {
                    responses.forEach { response ->
                        val client = socket.accept()
                        Thread {
                            client.use {
                                client.soTimeout = 30_000
                                val input = client.getInputStream().bufferedReader()
                                requests += input.readLine().orEmpty()
                                while (true) {
                                    if (input.readLine().isNullOrEmpty()) break
                                }
                                client.getOutputStream().apply {
                                    write(response)
                                    flush()
                                }
                            }
                        }.apply { isDaemon = true }.start()
                    }
                }
            }.apply { isDaemon = true }.start()
        }

        override fun close() {
            runCatching(socket::close)
        }
    }

    private fun ownerPackage(uri: Uri): String = requireNotNull(
        resolver.query(
            uri,
            arrayOf(MediaStore.Downloads.OWNER_PACKAGE_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        },
    )
}
