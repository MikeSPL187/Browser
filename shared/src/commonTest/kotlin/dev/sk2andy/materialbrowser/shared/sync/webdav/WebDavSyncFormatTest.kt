package dev.sk2andy.materialbrowser.shared.sync.webdav

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebDavSyncFormatTest {
    private val crypto = FakeSyncCrypto()
    private val phrase = "correct horse battery".encodeToByteArray()
    private val deviceId = "A".repeat(22)
    private val record = WebDavDeviceRecord(
        deviceId = deviceId,
        name = "Pixel 8",
        sequence = 7,
        updatedAtMillis = 1_800_000_000_000,
        tabs = listOf(LocalTransferTab("https://example.com/a", "Page A")),
        bookmarks = listOf(
            WebDavBookmarkEntry("https://example.org/", "Example", "Работа/Docs", 5),
            WebDavBookmarkEntry("https://gone.example/", "", "", 9, deleted = true),
        ).associateBy { it.key },
    )

    @Test
    fun aHeaderRoundTripsAndTellsAWrongPhraseApart() {
        val (header, key) = WebDavSyncFormat.newHeader(phrase, crypto)
        val read = assertNotNull(WebDavSyncFormat.decodeHeader(WebDavSyncFormat.encodeHeader(header)))

        assertEquals(header, read)
        assertTrue(WebDavSyncFormat.opens(read, WebDavSyncFormat.deriveKey(phrase, read, crypto), crypto))
        assertFalse(WebDavSyncFormat.opens(read, WebDavSyncFormat.deriveKey("another phrase".encodeToByteArray(), read, crypto), crypto))
        assertFalse(WebDavSyncFormat.encodeHeader(header).contains(key.decodeToString()))
    }

    @Test
    fun aHeaderWithAnotherCostOrExtraFieldsIsRefused() {
        val json = WebDavSyncFormat.encodeHeader(WebDavSyncFormat.newHeader(phrase, crypto).first)

        assertNull(WebDavSyncFormat.decodeHeader(json.replace("\"m\":65536", "\"m\":8")), "a weaker Argon2 cost")
        assertNull(WebDavSyncFormat.decodeHeader(json.replace("argon2id", "argon2i")))
        assertNull(WebDavSyncFormat.decodeHeader(json.replace("\"v\":1", "\"v\":2")))
        assertNull(WebDavSyncFormat.decodeHeader(json.replace("{\"v\":1", "{\"x\":0,\"v\":1")))
        assertNull(WebDavSyncFormat.decodeHeader("[]"))
    }

    @Test
    fun aRecordOpensOnlyForItsSpaceDeviceAndKey() {
        val (header, key) = WebDavSyncFormat.newHeader(phrase, crypto)
        val sealed = WebDavSyncFormat.sealRecord(record, header, key, crypto)

        assertEquals(record, WebDavSyncFormat.openRecord(sealed, deviceId, header, key, crypto))
        assertNull(WebDavSyncFormat.openRecord(sealed, "B".repeat(22), header, key, crypto), "another device's name")
        val (otherSpace, _) = WebDavSyncFormat.newHeader(phrase, crypto)
        assertNull(WebDavSyncFormat.openRecord(sealed, deviceId, otherSpace, key, crypto), "another space")
        assertNull(WebDavSyncFormat.openRecord(sealed, deviceId, header, ByteArray(32), crypto), "another key")
        assertFalse("example.org" in sealed)
    }

    @Test
    fun aRecordIsReadStrictly() {
        val json = WebDavSyncFormat.encodeRecord(record)

        assertEquals(record, WebDavSyncFormat.decodeRecord(json))
        assertNull(WebDavSyncFormat.decodeRecord(json.replace("https://example.com/a", "javascript:alert(1)")))
        assertNull(WebDavSyncFormat.decodeRecord(json.replace("\"seq\":7", "\"seq\":-1")))
        assertNull(WebDavSyncFormat.decodeRecord(json.replace("\"seq\":7", "\"seq\":\"7\"")))
        assertNull(WebDavSyncFormat.decodeRecord(json.replace(deviceId, "short")))
        val twice = json.replace("https://gone.example/", "HTTPS://Example.org")
        assertNull(WebDavSyncFormat.decodeRecord(twice), "one bookmark twice")
    }

    @Test
    fun addressesAreHttpsOnlyAndNextcloudIsTriedFirst() {
        assertEquals(WebDavAddressCheck.NotHttps, WebDavSyncAddressRules.check("http://cloud.example.com"))
        assertEquals(WebDavAddressCheck.Invalid, WebDavSyncAddressRules.check("cloud.example.com"))
        assertEquals(WebDavAddressCheck.Invalid, WebDavSyncAddressRules.check("https://anna:secret@cloud.example.com"))
        assertEquals(WebDavAddressCheck.Invalid, WebDavSyncAddressRules.check("https://cloud.example.com/dav?x=1"))
        assertEquals(WebDavAddressCheck.Invalid, WebDavSyncAddressRules.check("https://cloud.example.com/../etc"))
        assertEquals(WebDavAddressCheck.Invalid, WebDavSyncAddressRules.check("https://cloud.example.com:99999"))
        // Encoded dots are dots to the server (ЯС-7); a bracketed host is IPv6, with or without a port (ЯС-5).
        assertEquals(WebDavAddressCheck.Invalid, WebDavSyncAddressRules.check("https://cloud.example.com/a/%2E%2e/b"))
        assertEquals(WebDavAddressCheck.Valid("https://[::1]:8443/", "[::1]"), WebDavSyncAddressRules.check("https://[::1]:8443"))
        assertEquals(
            WebDavAddressCheck.Valid("https://[2001:db8::1]/dav/", "[2001:db8::1]"),
            WebDavSyncAddressRules.check("https://[2001:db8::1]/dav"),
        )
        assertEquals(WebDavAddressCheck.Invalid, WebDavSyncAddressRules.check("https://[::1]x"))
        assertEquals(WebDavAddressCheck.Invalid, WebDavSyncAddressRules.check("https://[zz]/"))
        assertEquals(
            WebDavAddressCheck.Valid("https://cloud.example.com:8443/dav/", "cloud.example.com"),
            WebDavSyncAddressRules.check("  HTTPS://Cloud.Example.com:8443/dav  "),
        )
        assertEquals(
            listOf("https://cloud.example.com/remote.php/dav/files/anna%40mail.ru/", "https://cloud.example.com/"),
            WebDavSyncAddressRules.roots("https://cloud.example.com/", "anna@mail.ru"),
        )
        assertEquals(
            listOf("https://c.example/remote.php/dav/files/anna/"),
            WebDavSyncAddressRules.roots("https://c.example/remote.php/dav/files/anna/", "anna"),
        )
        assertEquals("%D0%90%20b", WebDavSyncAddressRules.encodePathSegment("А b"))
    }

    @Test
    fun aMultistatusIsReadWhateverItsPrefixes() {
        val xml = """
            <?xml version="1.0"?>
            <D:multistatus xmlns:D="DAV:"><D:response><D:href>/dav/Vola/devices/</D:href>
            <D:propstat><D:prop><D:resourcetype><D:collection/></D:resourcetype></D:prop></D:propstat></D:response>
            <D:response><D:href>/dav/Vola/devices/abc.json</D:href><D:propstat><D:prop><D:resourcetype/>
            <D:getetag>&quot;123&quot;</D:getetag></D:prop></D:propstat></D:response></D:multistatus>
        """.trimIndent()

        assertEquals(
            listOf(WebDavResource("devices", null, isCollection = true), WebDavResource("abc.json", "\"123\"", isCollection = false)),
            WebDavMultistatus.parse(xml),
        )
        assertEquals(
            listOf(WebDavResource("x", null, isCollection = false)),
            WebDavMultistatus.parse("<multistatus xmlns=\"DAV:\"><response><href>https://h/x</href></response></multistatus>"),
        )
        assertNull(WebDavMultistatus.parse("<html>Hello</html>"))
        assertNull(WebDavMultistatus.parse("<!DOCTYPE x [<!ENTITY e \"e\">]><d:multistatus/>"))
    }

    @Test
    fun aHostileMultistatusCostsLinearTime() {
        val unclosed = "<d:multistatus>" + "<d:response><d:href>".repeat(200_000)
        val many = "<d:multistatus>" + "<d:response><d:href>/x</d:href></d:response>".repeat(WebDavMultistatus.MAX_RESOURCES + 1)

        assertEquals(emptyList(), WebDavMultistatus.parse(unclosed))
        assertNull(WebDavMultistatus.parse(many), "too many resources")
        assertEquals(
            listOf(WebDavResource("a", null, isCollection = false)),
            WebDavMultistatus.parse("<multistatus><response><responsedescription/><href>/a</href></response></multistatus>"),
        )
    }

    @Test
    fun anotherDevicesNameCannotHideItsText() {
        val json = WebDavSyncFormat.encodeRecord(record.copy(name = "Pixel\u202E8\u0007"))

        assertEquals("Pixel8", WebDavSyncFormat.decodeRecord(json)?.name)
    }

    @Test
    fun theMergeIsTheSameInAnyOrder() {
        val now = 1_000_000L
        val a = mapOf(entry("https://a.example/", "A", 10), entry("https://b.example/", "B", 20))
        val b = mapOf(entry("https://a.example/", "A2", 15), entry("https://b.example/", "", 20, deleted = true))
        val c = mapOf(entry("https://c.example/", "C", 1))

        val merged = WebDavBookmarkRules.merge(listOf(a, b, c), now)

        assertEquals(merged, WebDavBookmarkRules.merge(listOf(c, b, a, b), now))
        assertEquals("A2", merged.getValue("https://a.example").title)
        assertTrue(merged.getValue("https://b.example").deleted, "at the same time a deletion wins")
    }

    @Test
    fun oldTombstonesAreForgotten() {
        val tombstone = mapOf(entry("https://a.example/", "", 0, deleted = true))

        assertEquals(tombstone, WebDavBookmarkRules.merge(listOf(tombstone), WebDavBookmarkRules.TOMBSTONE_RETENTION_MILLIS))
        assertTrue(WebDavBookmarkRules.merge(listOf(tombstone), WebDavBookmarkRules.TOMBSTONE_RETENTION_MILLIS + 1).isEmpty())
    }

    @Test
    fun theJournalStampsOnlyWhatChanged() {
        val remembered = mapOf(entry("https://a.example/", "A", 10), entry("https://b.example/", "B", 10))
        val current = listOf(LocalTransferBookmark("https://a.example/", "A"), LocalTransferBookmark("https://c.example/", "C"))

        val journal = WebDavBookmarkRules.journal(remembered, current, nowMillis = 5)

        assertEquals(10, journal.getValue("https://a.example").modifiedAtMillis, "unchanged keeps its time")
        assertEquals(5, journal.getValue("https://c.example").modifiedAtMillis)
        val gone = journal.getValue("https://b.example")
        assertTrue(gone.deleted)
        assertEquals(11, gone.modifiedAtMillis, "a clock behind still moves forward")
        assertTrue(WebDavBookmarkRules.journal(null, current, nowMillis = 5).values.all { it.modifiedAtMillis == 0L })
    }

    @Test
    fun changesNameWhatToAddRemoveAndUpdate() {
        val merged = mapOf(
            entry("https://keep.example/", "Keep", 1),
            entry("https://gone.example/", "", 2, deleted = true),
            entry("https://moved.example/", "Moved", 3, folder = "Work"),
            entry("https://new.example/", "New", 4),
        )
        val current = listOf(
            LocalTransferBookmark("https://keep.example/", "Keep"),
            LocalTransferBookmark("HTTPS://GONE.example", "Gone"),
            LocalTransferBookmark("https://moved.example/", "Moved"),
        )

        val changes = WebDavBookmarkRules.changes(current, merged)

        assertEquals(listOf(LocalTransferBookmark("https://new.example/", "New")), changes.add)
        assertEquals(listOf("HTTPS://GONE.example"), changes.remove)
        assertEquals(listOf(LocalTransferBookmark("https://moved.example/", "Moved", "Work")), changes.update)
        assertTrue(WebDavBookmarkRules.changes(current.take(1), mapOf(entry("https://keep.example/", "Keep", 1))).isEmpty)
    }

    private fun entry(url: String, title: String, at: Long, deleted: Boolean = false, folder: String = "") =
        WebDavBookmarkEntry(url, title, folder, at, deleted).let { it.key to it }
}
