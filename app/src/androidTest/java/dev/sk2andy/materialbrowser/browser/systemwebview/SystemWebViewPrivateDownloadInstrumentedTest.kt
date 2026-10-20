package dev.sk2andy.materialbrowser.browser.systemwebview

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadFailure
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadTransferListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadTransferStart
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
import dev.sk2andy.materialbrowser.data.DownloadRuntimeRegistry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import java.io.OutputStream
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A private tab's WebView download that failed starts again from memory while the tab lives (V22). */
@RunWith(AndroidJUnit4::class)
class SystemWebViewPrivateDownloadInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val body = ByteArray(64 * 1024) { index -> (index % 251).toByte() }
    private val transfers = mutableListOf<SystemWebViewPrivateDownloadTransfer>()
    private val rows = mutableListOf<Long>()

    @After
    fun cleanUp() {
        transfers.forEach(SystemWebViewPrivateDownloadTransfer::close)
        DownloadRuntimeRegistry.clear(rows)
        context.contentResolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(FILE_NAME),
        )
    }

    @Test
    fun failedPrivateDownloadStartsAgainWithTheSameRequestAndSavesTheFile() {
        FixtureServer(::answerCutShort, ::answerInFull).use { server ->
            val transfer = newTransfer()
            val failedId = startAndFail(transfer, server)
            val failedRow = checkNotNull(DownloadRuntimeRegistry.entryForTransfer(failedId))
            assertEquals(DownloadStatus.Failed, failedRow.status)
            assertEquals(DownloadRetry.Available, failedRow.retry)

            assertTrue(DownloadRuntimeRegistry.retry(failedRow.id))

            assertTrue(
                "retried file was not saved",
                waitFor { storedDownload()?.contentEquals(body) == true },
            )
            assertTrue(
                "the failed row stayed",
                waitFor { DownloadRuntimeRegistry.entryForTransfer(failedId) == null },
            )
            assertTrue(
                "the retried row stayed",
                waitFor { DownloadRuntimeRegistry.snapshot().none { row -> row.name == FILE_NAME } },
            )
            val (first, second) = server.requests
            assertTrue(second.requestLine, second.requestLine.startsWith("GET /file "))
            assertEquals(USER_AGENT, second.headers["user-agent"])
            assertEquals(first.headers["cookie"], second.headers["cookie"])
            assertEquals(COOKIE, second.headers["cookie"])
        }
    }

    @Test
    fun closedPrivateTabsRowNoLongerStartsAgainAndNamesNoPage() {
        FixtureServer(::answerCutShort).use { server ->
            val transfer = newTransfer()
            val failedId = startAndFail(transfer, server)

            transfer.close()

            val row = checkNotNull(DownloadRuntimeRegistry.entryForTransfer(failedId))
            assertEquals(
                DownloadRetry.Unavailable(DownloadRetryUnavailable.PrivateClosed, pageUrl = null),
                row.retry,
            )
            assertFalse(DownloadRuntimeRegistry.retry(row.id))
            assertEquals(1, server.requests.size)
        }
    }

    private fun newTransfer() = SystemWebViewPrivateDownloadTransfer(context).also(transfers::add)

    /** Starts the download of [server]'s file, which breaks off; answers the failed transfer's id. */
    private fun startAndFail(transfer: SystemWebViewPrivateDownloadTransfer, server: FixtureServer): Int {
        val startedId = AtomicReference<Int>()
        val failed = CountDownLatch(1)
        transfer.start(
            SystemWebViewPrivateDownloadRequest(
                url = "http://127.0.0.1:${server.port}/file",
                fileName = FILE_NAME,
                mimeType = "application/octet-stream",
                pageUrl = "http://127.0.0.1:${server.port}/page",
                userAgent = USER_AGENT,
                cookiesFor = { COOKIE },
            ),
            object : GeckoDownloadTransferListener {
                override fun onStarted(start: GeckoDownloadTransferStart) = startedId.set(start.id)

                override fun onFailed(reason: GeckoDownloadFailure) = failed.countDown()
            },
        )
        assertTrue("download did not fail", failed.await(20, TimeUnit.SECONDS))
        val id = checkNotNull(startedId.get()) { "download failed before it started writing" }
        DownloadRuntimeRegistry.entryForTransfer(id)?.let { row -> rows += row.id }
        return id
    }

    /** Promises the whole file and sends a quarter of it. */
    private fun answerCutShort(output: OutputStream) {
        output.write(headers(body.size))
        output.write(body, 0, body.size / 4)
        output.flush()
    }

    private fun answerInFull(output: OutputStream) {
        output.write(headers(body.size))
        output.write(body)
        output.flush()
    }

    private fun headers(length: Int): ByteArray = (
        "HTTP/1.1 200 OK\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-Length: $length\r\n" +
            "Connection: close\r\n\r\n"
        ).encodeToByteArray()

    private fun waitFor(timeoutMillis: Long = 20_000L, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    private fun storedDownload(): ByteArray? = context.contentResolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Downloads._ID),
        "${MediaStore.Downloads.DISPLAY_NAME} = ?",
        arrayOf(FILE_NAME),
        null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) {
            null
        } else {
            val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0))
            context.contentResolver.openInputStream(uri)?.use { input -> input.readBytes() }
        }
    }

    private class FixtureRequest(val requestLine: String, val headers: Map<String, String>)

    /** Answers one connection after another with [responders], each on its own thread. */
    private class FixtureServer(vararg responders: (OutputStream) -> Unit) : AutoCloseable {
        private val socket = ServerSocket(0)
        val port: Int = socket.localPort
        val requests: MutableList<FixtureRequest> = CopyOnWriteArrayList()

        init {
            Thread {
                runCatching {
                    responders.forEach { respond ->
                        val client = socket.accept()
                        Thread {
                            client.use {
                                client.soTimeout = 30_000
                                val input = client.getInputStream().bufferedReader()
                                val requestLine = input.readLine().orEmpty()
                                val headers = linkedMapOf<String, String>()
                                while (true) {
                                    val line = input.readLine()
                                    if (line.isNullOrEmpty()) break
                                    val separator = line.indexOf(':')
                                    if (separator > 0) {
                                        headers[line.substring(0, separator).lowercase()] =
                                            line.substring(separator + 1).trim()
                                    }
                                }
                                requests += FixtureRequest(requestLine, headers)
                                respond(client.getOutputStream())
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

    private companion object {
        const val FILE_NAME = "candy-private-retry.bin"
        const val USER_AGENT = "CandyPrivateRetryTest/1.0"
        const val COOKIE = "candy=private"
    }
}
