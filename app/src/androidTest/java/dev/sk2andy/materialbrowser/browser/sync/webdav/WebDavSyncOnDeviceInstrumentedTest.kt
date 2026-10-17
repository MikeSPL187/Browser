package dev.sk2andy.materialbrowser.browser.sync.webdav

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.data.sync.webdav.AndroidWebDavSyncStore
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncFailure
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavAccount
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavConnectOutcome
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavLocalState
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavPullOutcome
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavRequest
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavResponse
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavStoredAccount
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncEngine
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncFormat
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncMemory
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavTransport
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The WebDAV sync (V27) end to end on a real Android: the phrase's key is the real Argon2id, the
 * files are sealed with the platform's AES-GCM, and the account goes through the Keystore. The
 * server is a small WebDAV in memory, so no network is needed.
 */
@RunWith(AndroidJUnit4::class)
class WebDavSyncOnDeviceInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val server = MemoryWebDav()
    private val engine = WebDavSyncEngine(server, AndroidWebDavSyncCrypto)
    private val store = AndroidWebDavSyncStore(context)
    private var now = 1_800_000_000_000L

    @After
    fun tearDown() = store.clear()

    @Test
    fun twoPhonesShareBookmarksAndTabsWithTheRealCrypto() {
        val first = connect("Pixel 8")
        val firstMemory = sync(
            first,
            WebDavLocalState("Pixel 8", listOf(LocalTransferTab("https://example.com/a", "Page A")), listOf(EXAMPLE)),
            WebDavSyncMemory(),
        )
        val second = connect("Tablet")

        val pulled = engine.pull(second, WebDavLocalState("Tablet", emptyList(), emptyList()), WebDavSyncMemory(), ++now)

        val plan = (pulled as WebDavPullOutcome.Pulled).plan
        assertEquals(listOf(EXAMPLE), plan.changes.add)
        val device = plan.devices.single()
        assertEquals("Pixel 8", device.name)
        assertEquals(listOf(LocalTransferTab("https://example.com/a", "Page A")), device.tabs)
        assertTrue(firstMemory.remembered.orEmpty().isNotEmpty())
        assertTrue("the server sees no address", server.files.values.none { "example.org" in it.decodeToString() })
    }

    @Test
    fun theAccountSurvivesTheKeystoreAndStillOpensTheServer() {
        val account = connect("Pixel 8")
        assertTrue(store.saveAccount(WebDavStoredAccount(account, "profile-1")))

        val restored = requireNotNull(store.loadAccount()).account

        val pulled = engine.pull(restored, WebDavLocalState("Pixel 8", emptyList(), listOf(EXAMPLE)), WebDavSyncMemory(), ++now)
        assertTrue(pulled is WebDavPullOutcome.Pulled)
    }

    @Test
    fun anotherPhraseDoesNotOpenTheData() {
        connect("Pixel 8")

        val outcome = engine.connect(ADDRESS, LOGIN, PASSWORD, "another long phrase".toByteArray(), WebDavSyncFormat.newDeviceId(AndroidWebDavSyncCrypto))

        assertEquals(WebDavConnectOutcome.Failed(RemoteSyncFailure.PhraseMismatch), outcome)
        assertFalse(server.files.keys.any { it.contains("/devices/") })
    }

    private fun connect(name: String): WebDavAccount {
        val outcome = engine.connect(ADDRESS, LOGIN, PASSWORD, PHRASE.toByteArray(), WebDavSyncFormat.newDeviceId(AndroidWebDavSyncCrypto))
        return (outcome as? WebDavConnectOutcome.Connected)?.account ?: throw AssertionError("$name did not connect: $outcome")
    }

    private fun sync(account: WebDavAccount, local: WebDavLocalState, memory: WebDavSyncMemory): WebDavSyncMemory {
        val plan = (engine.pull(account, local, memory, ++now) as WebDavPullOutcome.Pulled).plan
        val pushed = engine.push(account, plan, local.bookmarks, ++now)
        assertEquals(null, pushed.failure)
        return pushed.memory
    }

    /** Just enough WebDAV for the sync: Basic auth, collections, conditional PUT and ETags. */
    private class MemoryWebDav : WebDavTransport {
        val files = LinkedHashMap<String, ByteArray>()
        private val etags = HashMap<String, String>()
        private val collections = linkedSetOf(ROOT)
        private var version = 0

        override fun send(request: WebDavRequest): WebDavResponse {
            if (request.headers["Authorization"] != WebDavSyncEngine.authorization(LOGIN, PASSWORD)) return WebDavResponse(401)
            val url = request.url
            if (!url.startsWith(ROOT)) return WebDavResponse(404)
            return when (request.method) {
                "PROPFIND" -> propfind(url, request.headers["Depth"] == "1")
                "GET" -> files[url]?.let { WebDavResponse(200, etags[url], it) } ?: WebDavResponse(404)
                "MKCOL" -> if (url in collections) WebDavResponse(405) else WebDavResponse(201).also { collections += url }
                "PUT" -> {
                    val existing = etags[url]
                    when {
                        request.headers["If-None-Match"] == "*" && existing != null -> WebDavResponse(412)
                        request.headers["If-Match"]?.let { it != existing } == true -> WebDavResponse(412)
                        else -> {
                            files[url] = request.body ?: ByteArray(0)
                            val etag = "\"${++version}\"".also { etags[url] = it }
                            WebDavResponse(if (existing == null) 201 else 204, etag)
                        }
                    }
                }
                "DELETE" -> {
                    files.keys.removeAll { it.startsWith(url) }
                    collections.removeAll { it.startsWith(url) && it != ROOT }
                    WebDavResponse(204)
                }
                else -> WebDavResponse(405)
            }
        }

        private fun propfind(url: String, children: Boolean): WebDavResponse {
            if (url !in collections && url !in files) return WebDavResponse(404)
            val entries = mutableListOf(url to (url in collections))
            if (children) {
                collections.filter { it != url && it.removePrefix(url).trimEnd('/').let { rest -> rest.isNotEmpty() && '/' !in rest } }
                    .forEach { entries += it to true }
                files.keys.filter { it.startsWith(url) && '/' !in it.removePrefix(url) }.forEach { entries += it to false }
            }
            val body = entries.joinToString("") { (path, isCollection) ->
                val type = if (isCollection) "<d:resourcetype><d:collection/></d:resourcetype>" else "<d:resourcetype/>"
                val etag = etags[path]?.let { "<d:getetag>$it</d:getetag>" }.orEmpty()
                "<d:response><d:href>${path.removePrefix("https://cloud.example.com")}</d:href><d:propstat><d:prop>$type$etag</d:prop></d:propstat></d:response>"
            }
            return WebDavResponse(207, body = "<d:multistatus xmlns:d=\"DAV:\">$body</d:multistatus>".toByteArray())
        }
    }

    private companion object {
        const val ADDRESS = "https://cloud.example.com/dav"
        const val ROOT = "https://cloud.example.com/dav/"
        const val LOGIN = "anna"
        const val PASSWORD = "app-password"
        const val PHRASE = "correct horse battery"
        val EXAMPLE = LocalTransferBookmark("https://example.org/", "Example", "Work")
    }
}
