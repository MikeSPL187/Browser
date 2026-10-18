package dev.sk2andy.materialbrowser.shared.sync.webdav

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebDavSyncEngineTest {
    private val server = FakeWebDavServer()
    private val crypto = FakeSyncCrypto()
    private val engine = WebDavSyncEngine(server, crypto)
    private var now = 1_800_000_000_000L

    /** One phone: its bookmarks and tabs, the account and the memory the adapter would keep. */
    private inner class Phone(val name: String, phrase: String = PHRASE, val limit: Int = Int.MAX_VALUE) {
        val deviceId = WebDavSyncFormat.newDeviceId(crypto)
        val bookmarks = ArrayList<LocalTransferBookmark>()
        val tabs = ArrayList<LocalTransferTab>()
        var memory = WebDavSyncMemory()
        var devices: List<WebDavDeviceRecord> = emptyList()
        var account: WebDavAccount = connected(engine.connect(ADDRESS, LOGIN, PASSWORD, phrase.encodeToByteArray(), deviceId))

        fun sync(): RemoteSyncFailure? {
            now += 60_000
            val pulled = engine.pull(account, WebDavLocalState(name, tabs.toList(), bookmarks.toList()), memory, now)
            if (pulled is WebDavPullOutcome.Failed) return pulled.reason
            val plan = (pulled as WebDavPullOutcome.Pulled).plan
            devices = plan.devices
            bookmarks.removeAll { it.url in plan.changes.remove }
            plan.changes.update.forEach { changed -> bookmarks.replaceAll { if (it.url == changed.url) changed else it } }
            plan.changes.add.forEach { if (bookmarks.size < limit) bookmarks += it }
            val pushed = engine.push(account, plan, bookmarks.toList(), now)
            memory = pushed.memory
            return pushed.failure ?: if (pushed.again) sync() else null
        }
    }

    @Test
    fun twoPhonesShareTabsAndBookmarksThroughTheServer() {
        val first = Phone("Pixel 8").apply {
            tabs += LocalTransferTab("https://example.com/a", "Page A")
            bookmarks += LocalTransferBookmark("https://example.org/", "Example", "Work")
        }
        assertNull(first.sync())
        val second = Phone("Tablet").apply { bookmarks += LocalTransferBookmark("https://example.net/", "Net") }

        assertNull(second.sync())
        assertNull(first.sync())

        assertEquals(setOf("https://example.org/", "https://example.net/"), first.bookmarks.map { it.url }.toSet())
        assertEquals(first.bookmarks.toSet(), second.bookmarks.toSet())
        assertEquals("Work", second.bookmarks.single { it.url == "https://example.org/" }.folder)
        val seen = second.devices.single()
        assertEquals(listOf("Pixel 8", first.deviceId), listOf(seen.name, seen.deviceId))
        assertEquals(listOf(LocalTransferTab("https://example.com/a", "Page A")), seen.tabs)
        assertEquals("Tablet", first.devices.single().name)
        assertTrue(server.files.keys.none { "example.org" in it || "Pixel" in it }, "file names give nothing away")
        assertTrue(server.files.values.none { "example.org" in it.first.decodeToString() }, "the server sees no address")
    }

    @Test
    fun aDeletionReachesTheOtherPhoneAndDoesNotComeBack() {
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        first.sync()
        val second = Phone("B")
        second.sync()
        assertEquals(1, second.bookmarks.size)

        first.bookmarks.clear()
        first.sync()
        second.sync()
        first.sync()

        assertTrue(second.bookmarks.isEmpty())
        assertTrue(first.bookmarks.isEmpty())
    }

    @Test
    fun aPhoneSyncingForTheFirstTimeDoesNotBringBackWhatWasDeleted() {
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        first.sync()
        first.bookmarks.clear()
        first.sync()

        val late = Phone("B").apply {
            bookmarks += LocalTransferBookmark("https://example.org/", "Example")
            bookmarks += LocalTransferBookmark("https://only-here.example/", "Mine")
        }
        late.sync()
        first.sync()

        assertEquals(listOf("https://only-here.example/"), late.bookmarks.map { it.url })
        assertEquals(listOf("https://only-here.example/"), first.bookmarks.map { it.url })
    }

    @Test
    fun aRenameOrMoveTravelsAndTheLaterChangeWins() {
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        first.sync()
        val second = Phone("B")
        second.sync()

        second.bookmarks[0] = LocalTransferBookmark("https://example.org/", "Renamed", "Work")
        second.sync()
        first.sync()

        assertEquals(LocalTransferBookmark("https://example.org/", "Renamed", "Work"), first.bookmarks.single())
    }

    @Test
    fun aBookmarkThatDidNotFitIsNotTakenForADeletion() {
        val first = Phone("A").apply {
            bookmarks += LocalTransferBookmark("https://one.example/", "One")
            bookmarks += LocalTransferBookmark("https://two.example/", "Two")
        }
        first.sync()
        val small = Phone("B", limit = 1)
        small.sync()
        small.sync()
        first.sync()

        assertEquals(1, small.bookmarks.size)
        assertEquals(2, first.bookmarks.size, "the phone that is full must not delete on the others")
    }

    @Test
    fun theNextcloudFilesRootIsFoundFromTheBareAddress() {
        val nextcloud = FakeWebDavServer(nextcloudUser = LOGIN)
        val outcome = WebDavSyncEngine(nextcloud, crypto).connect("https://cloud.example.com", LOGIN, PASSWORD, PHRASE.encodeToByteArray(), "a".repeat(22))

        val account = connected(outcome)
        assertEquals("https://cloud.example.com/remote.php/dav/files/anna/", account.root)
        assertEquals("cloud.example.com", account.host)
        assertTrue(nextcloud.files.containsKey("${account.root}Vola/vola.json"))
    }

    @Test
    fun aWrongPhraseTouchesNothing() {
        Phone("A").sync()
        val before = server.requests.size

        val outcome = engine.connect(ADDRESS, LOGIN, PASSWORD, "another long phrase".encodeToByteArray(), "b".repeat(22))

        assertEquals(WebDavConnectOutcome.Failed(RemoteSyncFailure.PhraseMismatch), outcome)
        assertTrue(server.requests.drop(before).all { it.method == "GET" || it.method == "PROPFIND" })
    }

    @Test
    fun aDamagedHeaderIsAMismatchNotANewStart() {
        Phone("A")
        server.write("${server.root}Vola/vola.json", "{\"v\":1}")

        assertEquals(
            WebDavConnectOutcome.Failed(RemoteSyncFailure.PhraseMismatch),
            engine.connect(ADDRESS, LOGIN, PASSWORD, PHRASE.encodeToByteArray(), "b".repeat(22)),
        )
    }

    @Test
    fun aHeaderOfANewerVolaAsksForAnUpdateNotForThePhrase() {
        Phone("A")
        server.write("${server.root}Vola/vola.json", "{\"v\":2,\"space\":\"x\",\"kdf\":{},\"next\":true}")

        assertEquals(
            WebDavConnectOutcome.Failed(RemoteSyncFailure.NewerFormat),
            engine.connect(ADDRESS, LOGIN, PASSWORD, PHRASE.encodeToByteArray(), "b".repeat(22)),
        )
    }

    @Test
    fun whatTheServerAnswersIsToldApart() {
        val phone = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }

        server.answer = { if (it.method == "PUT") WebDavResponse(507) else null }
        assertEquals(RemoteSyncFailure.ServerFull, phone.sync())
        server.answer = { if (it.method == "PUT") WebDavResponse(423) else null }
        assertEquals(RemoteSyncFailure.ServerBusy, phone.sync())
        server.answer = { WebDavResponse(429) }
        assertEquals(RemoteSyncFailure.ServerBusy, phone.sync())
        server.answer = { WebDavResponse(503) }
        assertEquals(RemoteSyncFailure.ServerBusy, phone.sync())
        server.answer = { WebDavResponse(500) }
        assertEquals(RemoteSyncFailure.ServerError, phone.sync())
        server.answer = { WebDavResponse(404) }
        assertEquals(RemoteSyncFailure.FolderNotFound, phone.sync())
        server.answer = { null }
        assertNull(phone.sync())
        assertEquals(1, phone.bookmarks.size)
    }

    @Test
    fun connectionProblemsAreToldApart() {
        fun connect(address: String = ADDRESS, password: String = PASSWORD, transport: WebDavTransport = server) =
            WebDavSyncEngine(transport, crypto).connect(address, LOGIN, password, PHRASE.encodeToByteArray(), "c".repeat(22))

        assertEquals(WebDavConnectOutcome.Failed(RemoteSyncFailure.LoginRejected), connect(password = "wrong"))
        assertEquals(WebDavConnectOutcome.Failed(RemoteSyncFailure.NotHttps), connect(address = "http://cloud.example.com"))
        assertEquals(
            WebDavConnectOutcome.Failed(RemoteSyncFailure.NotWebDav),
            connect(transport = { WebDavResponse(200, body = "<html>Hello</html>".encodeToByteArray()) }),
        )
        assertEquals(WebDavConnectOutcome.Failed(RemoteSyncFailure.Unreachable), connect(transport = { error("no network") }))
    }

    @Test
    fun twoFirstPhonesAtOnceEndInOneSpace() {
        val racer = WebDavSyncEngine(server, crypto)
        server.beforeRequest = { request ->
            if (request.method == "PUT" && request.url.endsWith("vola.json")) {
                server.beforeRequest = {}
                connected(racer.connect(ADDRESS, LOGIN, PASSWORD, PHRASE.encodeToByteArray(), "d".repeat(22)))
            }
        }

        val account = Phone("A").account

        assertEquals(WebDavSyncFormat.decodeHeader(server.files.getValue("${server.root}Vola/vola.json").first.decodeToString()), account.header)
    }

    @Test
    fun anUnchangedDeviceIsNotDownloadedAgain() {
        Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }.sync()
        val second = Phone("B")
        second.sync()
        val before = server.requests.size

        second.sync()

        val gets = server.requests.drop(before).filter { it.method == "GET" && "/devices/" in it.url }
        assertTrue(gets.isEmpty(), "fetched again: ${gets.map { it.url }}")
    }

    @Test
    fun anOlderCopyOfADeviceFileIsIgnored() {
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://old.example/", "Old") }
        first.sync()
        val path = "${server.root}Vola/devices/${first.deviceId}.json"
        val old = server.files.getValue(path).first.decodeToString()
        first.bookmarks[0] = LocalTransferBookmark("https://new.example/", "New")
        first.sync()
        val second = Phone("B")
        second.sync()

        server.write(path, old)
        second.sync()

        assertEquals(listOf("https://new.example/"), second.bookmarks.map { it.url })
        assertEquals("https://new.example/", second.devices.single().bookmarks.values.single { !it.deleted }.url)
    }

    @Test
    fun aDeviceFileCopiedUnderAnotherIdDoesNotOpen() {
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        first.sync()
        val copy = "${server.root}Vola/devices/${"e".repeat(22)}.json"
        server.write(copy, server.files.getValue("${server.root}Vola/devices/${first.deviceId}.json").first.decodeToString())
        val second = Phone("B")

        second.sync()

        assertEquals(listOf(first.deviceId), second.devices.map { it.deviceId })
    }

    @Test
    fun startingOverFromOnePhoneAsksTheOthersForThePhraseAgain() {
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        first.sync()
        val second = Phone("B")
        second.sync()

        first.account = connected(engine.startOver(first.account))
        first.memory = WebDavSyncMemory()
        assertNull(first.sync())

        assertEquals(RemoteSyncFailure.PhraseMismatch, second.sync())
        assertEquals(1, second.bookmarks.size, "a mismatch changes nothing on the phone")
        second.account = connected(engine.connect(ADDRESS, LOGIN, PASSWORD, PHRASE.encodeToByteArray(), second.deviceId))
        second.memory = WebDavSyncMemory()
        assertNull(second.sync())
        assertEquals(listOf("A"), second.devices.map { it.name })
    }

    @Test
    fun startingOverAfterAMismatchUsesTheNewPhrase() {
        val first = Phone("A")
        first.sync()
        val phrase = "a brand new phrase".encodeToByteArray()
        assertEquals(
            WebDavConnectOutcome.Failed(RemoteSyncFailure.PhraseMismatch),
            engine.connect(ADDRESS, LOGIN, PASSWORD, phrase, "f".repeat(22)),
        )

        connected(engine.startOver(ADDRESS, LOGIN, PASSWORD, phrase, "f".repeat(22)))

        assertEquals(RemoteSyncFailure.PhraseMismatch, first.sync())
        assertTrue(server.files.keys.none { "/devices/${first.deviceId}" in it })
    }

    @Test
    fun dataDeletedFromAnotherPhoneIsWrittenAgainByOneStillConnected() {
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        first.sync()
        val second = Phone("B")
        second.sync()

        assertTrue(engine.remove(second.account, everything = true))
        assertTrue(server.files.isEmpty())
        assertNull(first.sync())

        assertTrue(server.files.containsKey("${server.root}Vola/vola.json"))
        assertTrue(server.files.containsKey("${server.root}Vola/devices/${first.deviceId}.json"))
    }

    @Test
    fun disconnectingTakesOnlyThisPhoneOffTheServer() {
        val first = Phone("A")
        first.sync()
        val second = Phone("B")
        second.sync()

        assertTrue(engine.remove(second.account, everything = false))
        first.sync()

        assertTrue(first.devices.isEmpty())
        assertTrue(server.files.containsKey("${server.root}Vola/vola.json"))
        server.offline = true
        assertFalse(engine.remove(first.account, everything = false))
    }

    @Test
    fun aWriteThePhoneHasNotSeenIsNotOverwritten() {
        val first = Phone("A")
        first.sync()
        val path = "${server.root}Vola/devices/${first.deviceId}.json"
        server.beforeRequest = { request ->
            if (request.method == "PUT" && request.url == path) {
                server.beforeRequest = {}
                server.write(path, server.files.getValue(path).first.decodeToString())
            }
        }

        first.bookmarks += LocalTransferBookmark("https://example.org/", "Example")
        assertNull(first.sync())

        val second = Phone("B")
        second.sync()
        assertEquals(listOf("https://example.org/"), second.bookmarks.map { it.url })
        assertTrue(server.requests.count { it.method == "PUT" && it.url == path } >= 3, "the second upload came after a fresh read")
    }

    @Test
    fun withoutAnEtagOnUploadTheNextSyncStillWorks() {
        server.returnsEtagOnPut = false
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        assertNull(first.sync())
        first.bookmarks += LocalTransferBookmark("https://example.net/", "Net")
        assertNull(first.sync())

        val second = Phone("B")
        second.sync()
        assertEquals(2, second.bookmarks.size)
    }

    @Test
    fun anUnreachableServerKeepsThePhoneAsItWas() {
        val first = Phone("A").apply { bookmarks += LocalTransferBookmark("https://example.org/", "Example") }
        first.sync()
        server.offline = true

        assertEquals(RemoteSyncFailure.Unreachable, first.sync())
        assertEquals(1, first.bookmarks.size)
    }

    private fun connected(outcome: WebDavConnectOutcome): WebDavAccount = assertIs<WebDavConnectOutcome.Connected>(outcome).account

    private companion object {
        const val ADDRESS = "https://cloud.example.com"
        const val LOGIN = "anna"
        const val PASSWORD = "app-password"
        const val PHRASE = "correct horse battery"
    }
}
