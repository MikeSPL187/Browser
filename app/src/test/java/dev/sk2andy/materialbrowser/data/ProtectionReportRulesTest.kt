package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProtectionReportRulesTest {
    private val today = 20_000L

    @Test
    fun `site is the web host without www`() {
        assertEquals("example.com", ProtectionReportRules.site("https://WWW.Example.com/a?b"))
        assertEquals("news.example.com", ProtectionReportRules.site("http://news.example.com"))
        assertNull(ProtectionReportRules.site("about:blank"))
        assertNull(ProtectionReportRules.site("file:///sdcard/page.html"))
    }

    @Test
    fun `record adds up per site and day`() {
        var days = ProtectionReportRules.record(emptyList(), today, "a.com", 3)
        days = ProtectionReportRules.record(days, today, "a.com", 2)
        days = ProtectionReportRules.record(days, today, "b.com", 1)
        days = ProtectionReportRules.record(days, today, "b.com", 0)

        assertEquals(listOf(ProtectionDay(today, mapOf("a.com" to 5, "b.com" to 1))), days)
    }

    @Test
    fun `days outside the week are dropped, also after the clock moved back`() {
        val old = ProtectionDay(today - 7, mapOf("old.com" to 9))
        val edge = ProtectionDay(today - 6, mapOf("edge.com" to 1))
        val future = ProtectionDay(today + 1, mapOf("future.com" to 1))

        assertEquals(listOf(edge), ProtectionReportRules.prune(listOf(future, old, edge), today))
    }

    @Test
    fun `a day keeps its most blocked sites when it gets too many`() {
        var days = emptyList<ProtectionDay>()
        (1..ProtectionReportRules.MAX_SITES_PER_DAY + 5).forEach { index ->
            days = ProtectionReportRules.record(days, today, "s$index.com", index)
        }
        val sites = days.single().blockedBySite

        assertEquals(ProtectionReportRules.MAX_SITES_PER_DAY, sites.size)
        assertNull(sites["s1.com"])
    }

    @Test
    fun `the day total keeps counting blocks of sites dropped past the limit`() {
        var days = emptyList<ProtectionDay>()
        (1..ProtectionReportRules.MAX_SITES_PER_DAY + 1).forEach { index ->
            days = ProtectionReportRules.record(days, today, "s$index.com", 1)
        }
        days = ProtectionReportRules.record(days, today, "s1.com", 1)
        val week = ProtectionReportRules.week(days, today)

        assertEquals(ProtectionReportRules.MAX_SITES_PER_DAY + 2, days.single().total)
        assertEquals(ProtectionReportRules.MAX_SITES_PER_DAY + 2, week.total)
        assertEquals(ProtectionReportRules.MAX_SITES_PER_DAY + 2, week.daily.last())
    }

    @Test
    fun `week sums days, counts sites and orders the top sites`() {
        val days = listOf(
            ProtectionDay(today - 6, mapOf("a.com" to 4, "b.com" to 1)),
            ProtectionDay(today, mapOf("b.com" to 5, "c.com" to 2)),
        )

        val week = ProtectionReportRules.week(days, today)

        assertEquals(12, week.total)
        assertEquals(3, week.siteCount)
        assertEquals(listOf(5, 0, 0, 0, 0, 0, 7), week.daily)
        assertEquals(listOf("b.com" to 6, "a.com" to 4, "c.com" to 2), week.topSites.map { it.host to it.blocked })
        assertEquals(today - 6, week.firstEpochDay)
    }

    @Test
    fun `an empty week has seven zero days`() {
        val week = ProtectionReportRules.week(emptyList(), today)

        assertEquals(0, week.total)
        assertEquals(List(7) { 0 }, week.daily)
    }

    @Test
    fun `totals saturate instead of overflowing`() {
        val days = listOf(ProtectionDay(today, mapOf("a.com" to Int.MAX_VALUE, "b.com" to 5)))

        assertEquals(Int.MAX_VALUE, ProtectionReportRules.week(days, today).total)
        assertEquals(
            Int.MAX_VALUE,
            ProtectionReportRules.record(days, today, "a.com", 1).single().blockedBySite["a.com"],
        )
    }
}
