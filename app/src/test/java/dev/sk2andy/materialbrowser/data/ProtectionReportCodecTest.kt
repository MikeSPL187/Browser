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
    fun `trackers and the sites that loaded them survive a round trip`() {
        val days = listOf(
            ProtectionDay(
                20_000,
                mapOf("news.com" to 3),
                blockedByTracker = mapOf("ads.net" to 2, "track.io" to 1),
                trackersBySite = mapOf("news.com" to setOf("ads.net", "track.io")),
            ),
        )

        assertEquals(days, ProtectionReportCodec.decode(ProtectionReportCodec.encode(days)))
    }

    @Test
    fun `a report saved before trackers existed loads without them`() {
        assertEquals(
            listOf(ProtectionDay(20_000, mapOf("a.com" to 2))),
            ProtectionReportCodec.decode("""{"20000": {"a.com": 2}, "totals": {"20000": 2}}"""),
        )
        // Trackers of a site the day no longer lists are not kept.
        assertEquals(
            listOf(ProtectionDay(20_000, mapOf("a.com" to 2), blockedByTracker = mapOf("ads.net" to 2))),
            ProtectionReportCodec.decode(
                """{"20000": {"a.com": 2}, "trackers": {"20000": {"ads.net": 2, "x.net": 0}},""" +
                    """ "siteTrackers": {"20000": {"gone.com": ["ads.net"], "a.com": []}}}""",
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
