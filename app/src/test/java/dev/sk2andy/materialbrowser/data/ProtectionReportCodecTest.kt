package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionReportCodecTest {
    @Test
    fun `round trip keeps days and counts`() {
        val days = listOf(
            ProtectionDay(19_999, mapOf("a.com" to 3)),
            ProtectionDay(20_000, mapOf("a.com" to 1, "b.com" to 7)),
        )

        assertEquals(days, ProtectionReportCodec.decode(ProtectionReportCodec.encode(days)))
    }

    @Test
    fun `trackers are saved apart from the days and survive a round trip`() {
        val days = listOf(
            ProtectionDay(
                20_000,
                mapOf("news.com" to 3),
                blockedByTracker = mapOf("ads.net" to 2, "track.io" to 1),
                trackersBySite = mapOf("news.com" to setOf("ads.net", "track.io")),
            ),
        )

        assertEquals(listOf(ProtectionDay(20_000, mapOf("news.com" to 3))), ProtectionReportCodec.decode(ProtectionReportCodec.encode(days)))
        assertEquals(
            mapOf(20_000L to ProtectionDayTrackers(days.single().blockedByTracker, days.single().trackersBySite)),
            ProtectionReportCodec.decodeTrackers(ProtectionReportCodec.encodeTrackers(days)),
        )
    }

    @Test
    fun `malformed trackers are skipped`() {
        assertEquals(emptyMap<Long, ProtectionDayTrackers>(), ProtectionReportCodec.decodeTrackers(""))
        assertEquals(
            mapOf(20_000L to ProtectionDayTrackers(mapOf("ads.net" to 2), mapOf("a.com" to setOf("ads.net")))),
            ProtectionReportCodec.decodeTrackers(
                """{"20000": {"blocked": {"ads.net": 2, "x.net": 0}, "sites": {"a.com": ["ads.net", "x.net"], "b.com": []}},""" +
                    """ "x": {}, "20001": {"sites": {"a.com": ["ads.net"]}}}""",
            ),
        )
    }

    @Test
    fun `malformed input is skipped`() {
        assertEquals(emptyList<ProtectionDay>(), ProtectionReportCodec.decode(""))
        assertEquals(
            listOf(ProtectionDay(20_000, mapOf("a.com" to 2))),
            ProtectionReportCodec.decode("""{"x": {"a.com": 1}, "20000": {"a.com": 2, "b.com": -1}, "20001": 5}"""),
        )
    }

    @Test
    fun `the exact day total survives a round trip`() {
        val days = listOf(ProtectionDay(20_000, mapOf("a.com" to 3), total = 9))

        assertEquals(days, ProtectionReportCodec.decode(ProtectionReportCodec.encode(days)))
    }

    @Test
    fun `a day saved before totals existed counts the sum of its sites`() {
        assertEquals(
            listOf(ProtectionDay(20_000, mapOf("a.com" to 2, "b.com" to 3), total = 5)),
            ProtectionReportCodec.decode("""{"20000": {"a.com": 2, "b.com": 3}}"""),
        )
        assertEquals(
            listOf(ProtectionDay(20_000, mapOf("a.com" to 2), total = 2)),
            ProtectionReportCodec.decode("""{"20000": {"a.com": 2}, "totals": {"20000": 1, "19999": 4}}"""),
        )
    }
}
