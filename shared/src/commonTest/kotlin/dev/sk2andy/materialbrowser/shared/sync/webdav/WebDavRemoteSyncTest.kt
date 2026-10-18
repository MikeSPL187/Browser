package dev.sk2andy.materialbrowser.shared.sync.webdav

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebDavRemoteSyncTest {
    private val server = FakeWebDavServer()
    private val crypto = FakeSyncCrypto()
    private val runner = QueueRunner()
    private var now = 1_800_000_000_000L

    /** Background and main work in two queues, run by [drain] in order. */
    private class QueueRunner : WebDavSyncRunner {
        val background = ArrayDeque<() -> Unit>()
        val main = ArrayDeque<() -> Unit>()

        override fun background(block: () -> Unit) {
            background += block
        }

        override fun main(block: () -> Unit) {
            main += block
        }

        fun drain() {
            while (background.isNotEmpty() || main.isNotEmpty()) {
                main.removeFirstOrNull()?.invoke()
                background.removeFirstOrNull()?.invoke()
            }
        }
    }

    /** Keeps the files as the adapter would: through the codec, so it round-trips too. */
    private class MemoryStore : WebDavSyncStore {
        var account: String? = null
        var memory: String? = null

        override fun loadAccount() = account?.let(WebDavSyncStateCodec::decodeAccount)

        override fun saveAccount(stored: WebDavStoredAccount): Boolean {
            account = WebDavSyncStateCodec.encodeAccount(stored)
            return true
        }

        override fun loadMemory() = memory?.let(WebDavSyncStateCodec::decodeMemory)

        override fun saveMemory(memory: WebDavSyncMemory): Boolean {
            this.memory = WebDavSyncStateCodec.encodeMemory(memory)
            return true
        }

        override fun clear() {
            account = null
            memory = null
        }
    }

    private class Library(val name: String) : WebDavSyncLibrary {
        val bookmarks = ArrayList<LocalTransferBookmark>()
        val tabs = ArrayList<LocalTransferTab>()

        override fun currentScope() = "profile-1"

        override fun local(scope: String) = WebDavLocalState(name, tabs.toList(), bookmarks.toList())

        override fun apply(scope: String, changes: WebDavBookmarkChanges): List<LocalTransferBookmark> {
            bookmarks.removeAll { it.url in changes.remove }
            changes.update.forEach { changed -> bookmarks.replaceAll { if (it.url == changed.url) changed else it } }
            bookmarks += changes.add
            return bookmarks.toList()
        }
    }

    private inner class Phone(name: String, val store: MemoryStore = MemoryStore()) {
        val library = Library(name)
        val port = WebDavRemoteSync(WebDavSyncEngine(server, crypto), store, runner, { WebDavSyncFormat.newDeviceId(crypto) }, { now })
            .also { it.attach(library) }
        val states = ArrayList<RemoteSyncState>()

        init {
            port.addListener { states += it }
            runner.drain()
        }

        fun connect(phrase: String = PHRASE): RemoteSyncConnectResult {
            var result: RemoteSyncConnectResult? = null
            port.connect(ADDRESS, LOGIN, PASSWORD, phrase) { result = it }
            runner.drain()
            return assertNotNull(result)
        }

        fun sync() {
            now += 60_000
            port.syncNow()
            runner.drain()
        }
    }

    @Test
    fun connectingSyncsAndTheStateSaysSo() {
        val phone = Phone("Pixel 8").apply { library.bookmarks += LocalTransferBookmark("https://example.org/", "Example") }

        assertEquals(RemoteSyncConnectResult.Connected, phone.connect())

        val state = phone.port.state
        assertEquals("cloud.example.com", state.server)
        assertFalse(state.syncing)
        assertNull(state.lastError)
        assertEquals(now, state.lastSyncedAtMillis)
        assertTrue(phone.states.any { it.syncing }, "the screen saw the sync run")
        assertNotNull(phone.store.account)
        assertFalse(phone.store.account!!.contains(PHRASE), "the phrase is never stored")
    }

    @Test
    fun twoPhonesSeeEachOther() {
        val first = Phone("Pixel 8").apply {
            library.tabs += LocalTransferTab("https://example.com/a", "Page A")
            library.bookmarks += LocalTransferBookmark("https://example.org/", "Example")
        }
        first.connect()
        val second = Phone("Tablet")
        second.connect()
        first.sync()

        val seen = second.port.state.devices.single()
        assertEquals(listOf("Pixel 8", "1"), listOf(seen.name, seen.tabCount.toString()))
        assertEquals(listOf(RemoteSyncTab("https://example.com/a", "Page A")), second.port.deviceTabs(seen.id))
        assertEquals(listOf("https://example.org/"), second.library.bookmarks.map { it.url })
        assertEquals("Tablet", first.port.state.devices.single().name)
        assertTrue(second.port.deviceTabs("unknown").isEmpty())
    }

    @Test
    fun aPhoneOpenedAgainRemembersTheServerAndTheDevices() {
        val first = Phone("Pixel 8")
        first.connect()
        val store = MemoryStore()
        Phone("Tablet", store).connect()
        first.sync()

        val reopened = Phone("Tablet", store)

        assertEquals("cloud.example.com", reopened.port.state.server)
        assertEquals(listOf("Pixel 8"), reopened.port.state.devices.map { it.name })
        reopened.port.onAppOpened()
        runner.drain()
        assertNull(reopened.port.state.lastError)
    }

    @Test
    fun aMismatchCanStartOverWithTheTypedPhrase() {
        val first = Phone("Pixel 8")
        first.connect()
        val second = Phone("Tablet")

        assertEquals(RemoteSyncConnectResult.Failed(RemoteSyncFailure.PhraseMismatch), second.connect("another long phrase"))
        assertNull(second.port.state.server)
        var result: RemoteSyncConnectResult? = null
        second.port.startOver { result = it }
        runner.drain()

        assertEquals(RemoteSyncConnectResult.Connected, result)
        assertEquals("cloud.example.com", second.port.state.server)
        first.sync()
        assertEquals(RemoteSyncFailure.PhraseMismatch, first.port.state.lastError)
        assertEquals("cloud.example.com", first.port.state.server, "still connected, asked for the phrase")
    }

    @Test
    fun startingOverWithNothingToStartFromFails() {
        var result: RemoteSyncConnectResult? = null
        Phone("Pixel 8").port.startOver { result = it }

        assertEquals(RemoteSyncConnectResult.Failed(RemoteSyncFailure.Unreachable), result)
    }

    @Test
    fun aSyncAskedForDuringOneRunsOnceAfterIt() {
        val phone = Phone("Pixel 8")
        phone.connect()
        val before = server.requests.count { it.method == "PUT" }

        phone.port.syncNow()
        phone.port.syncNow()
        phone.port.syncNow()
        runner.drain()

        assertEquals(2, server.requests.count { it.method == "PUT" } - before)
    }

    @Test
    fun disconnectingForgetsEverythingAndCanDeleteTheServerData() {
        val first = Phone("Pixel 8")
        first.connect()
        val second = Phone("Tablet")
        second.connect()

        var removed: Boolean? = null
        second.port.disconnect(deleteServerData = false) { removed = it }
        runner.drain()

        assertEquals(true, removed)
        assertEquals(RemoteSyncState(), second.port.state)
        assertNull(second.store.account)
        assertNull(second.store.memory)
        first.sync()
        assertTrue(first.port.state.devices.isEmpty())

        first.port.disconnect(deleteServerData = true) { removed = it }
        runner.drain()
        assertTrue(server.files.isEmpty())
    }

    @Test
    fun deletingTheServerDataWhileItCannotBeReachedKeepsThePhoneConnected() {
        val phone = Phone("Pixel 8")
        phone.connect()
        val connected = phone.port.state
        server.offline = true

        var removed: Boolean? = null
        phone.port.disconnect(deleteServerData = true) { removed = it }
        runner.drain()

        assertEquals(false, removed)
        assertEquals(connected, phone.port.state)
        assertNotNull(phone.store.account, "the app password stays, so the deletion can be asked again")
        assertTrue(server.files.isNotEmpty())

        server.offline = false
        phone.port.disconnect(deleteServerData = true) { removed = it }
        runner.drain()

        assertEquals(true, removed)
        assertEquals(RemoteSyncState(), phone.port.state)
        assertNull(phone.store.account)
        assertTrue(server.files.isEmpty())
    }

    @Test
    fun anUnreachableServerIsReportedAndChangesNothing() {
        val phone = Phone("Pixel 8").apply { library.bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        phone.connect()
        val synced = phone.port.state.lastSyncedAtMillis
        server.offline = true

        phone.sync()

        assertEquals(RemoteSyncFailure.Unreachable, phone.port.state.lastError)
        assertEquals(synced, phone.port.state.lastSyncedAtMillis)
        assertEquals(1, phone.library.bookmarks.size)
        assertFalse(phone.port.state.syncing)
    }

    @Test
    fun withoutALibraryNothingConnects() {
        val port = WebDavRemoteSync(WebDavSyncEngine(server, crypto), MemoryStore(), runner, { "a".repeat(22) }, { now })
        var result: RemoteSyncConnectResult? = null

        port.connect(ADDRESS, LOGIN, PASSWORD, PHRASE) { result = it }
        runner.drain()

        assertEquals(RemoteSyncConnectResult.Failed(RemoteSyncFailure.Unreachable), result)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun theStoredStateRoundTrips() {
        val (header, key) = WebDavSyncFormat.newHeader(PHRASE.encodeToByteArray(), crypto)
        val account = WebDavAccount("https://c.example/dav/", "c.example", "anna", "pw", key, header, "a".repeat(22))
        val decoded = assertNotNull(WebDavSyncStateCodec.decodeAccount(WebDavSyncStateCodec.encodeAccount(WebDavStoredAccount(account, "p"))))

        assertEquals(listOf(account.root, account.host, account.login, account.appPassword, account.deviceId, "p"),
            listOf(decoded.account.root, decoded.account.host, decoded.account.login, decoded.account.appPassword, decoded.account.deviceId, decoded.scope))
        assertTrue(decoded.account.key.contentEquals(key))
        assertEquals(header, decoded.account.header)
        assertFalse(account.toString().contains("pw"))

        val memory = WebDavSyncMemory(
            remembered = mapOf(WebDavBookmarkEntry("https://example.org/", "E", "", 3).let { it.key to it }),
            sequence = 4,
            ownEtag = "\"e1\"",
            devices = mapOf("b".repeat(22) to WebDavCachedDevice(null, WebDavDeviceRecord("b".repeat(22), "Tab", 2, 9, emptyList(), emptyMap()))),
        )
        assertEquals(memory, WebDavSyncStateCodec.decodeMemory(WebDavSyncStateCodec.encodeMemory(memory)))
        assertEquals(WebDavSyncMemory(), WebDavSyncStateCodec.decodeMemory(WebDavSyncStateCodec.encodeMemory(WebDavSyncMemory())))
        assertNull(WebDavSyncStateCodec.decodeMemory("{}"))
        assertNull(WebDavSyncStateCodec.decodeAccount("{}"))
    }

    private companion object {
        const val ADDRESS = "https://cloud.example.com"
        const val LOGIN = "anna"
        const val PASSWORD = "app-password"
        const val PHRASE = "correct horse battery"
    }
}
