package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `only regular tabs of unlocked workspaces are counted`() {
        assertTrue(ProtectionReportRules.countsTab(isPrivate = false, workspaceProtected = false))
        assertFalse(ProtectionReportRules.countsTab(isPrivate = true, workspaceProtected = false))
        assertFalse(ProtectionReportRules.countsTab(isPrivate = false, workspaceProtected = true))
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
    fun `days the report no longer keeps are dropped, also after the clock moved back`() {
        val old = ProtectionDay(today - ProtectionReportRules.KEPT_DAYS, mapOf("old.com" to 9))
        val edge = ProtectionDay(today - ProtectionReportRules.KEPT_DAYS + 1, mapOf("edge.com" to 1))
        val future = ProtectionDay(today + 1, mapOf("future.com" to 1))

        assertEquals(listOf(edge), ProtectionReportRules.prune(listOf(future, old, edge), today))
    }

    @Test
    fun `the week counts its seven days only, the report keeps thirty`() {
        val days = listOf(
            ProtectionDay(today - 20, mapOf("old.com" to 4)),
            ProtectionDay(today, mapOf("new.com" to 1)),
        )

        assertEquals(1, ProtectionReportRules.week(days, today).total)
        assertEquals(5, ProtectionReportRules.summary(days, today, 30).total)
        assertEquals(30, ProtectionReportRules.summary(days, today, 30).daily.size)
        assertEquals(4, ProtectionReportRules.summary(days, today, 30).daily.first { it > 0 })
    }

    @Test
    fun `record counts trackers and which sites loaded them`() {
        var days = ProtectionReportRules.record(emptyList(), today, "news.com", 3, mapOf("ads.net" to 2, "track.io" to 1))
        days = ProtectionReportRules.record(days, today, "shop.com", 1, mapOf("ads.net" to 1))
        days = ProtectionReportRules.record(days, today, "news.com", 1, mapOf("ads.net" to 1))
        val day = days.single()

        assertEquals(mapOf("ads.net" to 4, "track.io" to 1), day.blockedByTracker)
        assertEquals(mapOf("news.com" to setOf("ads.net", "track.io"), "shop.com" to setOf("ads.net")), day.trackersBySite)
    }

    @Test
    fun `the privacy report ranks trackers and the sites that loaded the most of them`() {
        val days = listOf(
            ProtectionDay(
                today - 10,
                mapOf("news.com" to 5),
                blockedByTracker = mapOf("ads.net" to 5),
                trackersBySite = mapOf("news.com" to setOf("ads.net")),
            ),
            ProtectionDay(
                today,
                mapOf("news.com" to 2, "blog.com" to 3),
                blockedByTracker = mapOf("ads.net" to 1, "track.io" to 3, "pixel.org" to 1),
                trackersBySite = mapOf("news.com" to setOf("ads.net", "track.io"), "blog.com" to setOf("track.io", "pixel.org")),
            ),
        )

        val month = ProtectionReportRules.summary(days, today, 30)
        assertEquals(3, month.trackerCount)
        assertEquals(
            listOf(
                ProtectionTracker("ads.net", 6, 1),
                ProtectionTracker("track.io", 3, 2),
                ProtectionTracker("pixel.org", 1, 1),
            ),
            month.topTrackers,
        )
        assertEquals(
            listOf(ProtectionSiteTrackers("blog.com", 2), ProtectionSiteTrackers("news.com", 2)),
            month.sitesWithMostTrackers,
        )
        val week = ProtectionReportRules.summary(days, today, 7)
        assertEquals(listOf("track.io" to 3, "ads.net" to 1, "pixel.org" to 1), week.topTrackers.map { it.host to it.blocked })
    }

    @Test
    fun `a day keeps a bounded number of site and tracker pairs`() {
        var days = emptyList<ProtectionDay>()
        val perSite = ProtectionReportRules.MAX_TRACKERS_PER_SITE
        repeat(ProtectionReportRules.MAX_SITE_TRACKERS_PER_DAY / perSite + 2) { site ->
            days = ProtectionReportRules.record(days, today, "s$site.com", 1, (1..perSite).associate { "t$it.net" to 1 })
        }

        assertEquals(
            ProtectionReportRules.MAX_SITE_TRACKERS_PER_DAY,
            days.single().trackersBySite.values.sumOf { it.size },
        )
    }

    @Test
    fun `a tracker the day no longer counts is not listed for a site`() {
        var days = emptyList<ProtectionDay>()
        (1..ProtectionReportRules.MAX_TRACKERS_PER_DAY).forEach { index ->
            days = ProtectionReportRules.record(days, today, "s$index.com", 1, mapOf("t$index.net" to 5))
        }
        // The site itself stays (most blocks of the day); only its tracker is past the limit.
        days = ProtectionReportRules.record(days, today, "news.com", 10, mapOf("rare.net" to 1))

        assertEquals(10, days.single().blockedBySite["news.com"])
        assertNull(days.single().blockedByTracker["rare.net"])
        assertNull(days.single().trackersBySite["news.com"])
    }

    @Test
    fun `trackers read after the start join those recorded since`() {
        val recorded = ProtectionReportRules.record(
            listOf(ProtectionDay(today, mapOf("news.com" to 2))),
            today,
            "news.com",
            1,
            mapOf("ads.net" to 1),
        )
        val loaded = mapOf(
            today to ProtectionDayTrackers(mapOf("ads.net" to 2, "track.io" to 1), mapOf("news.com" to setOf("track.io"))),
            today - 1 to ProtectionDayTrackers(mapOf("old.net" to 1), emptyMap()),
        )

        val day = ProtectionReportRules.withTrackers(recorded, loaded).single()
        assertEquals(mapOf("ads.net" to 3, "track.io" to 1), day.blockedByTracker)
        assertEquals(mapOf("news.com" to setOf("track.io", "ads.net")), day.trackersBySite)
    }

    @Test
    fun `a site keeps a bounded number of different trackers`() {
        val many = (1..ProtectionReportRules.MAX_TRACKERS_PER_SITE + 10).associate { "t$it.net" to 1 }
        var days = ProtectionReportRules.record(emptyList(), today, "news.com", many.size, many)
        days = ProtectionReportRules.record(days, today, "news.com", 1, mapOf("late.net" to 1))

        assertEquals(ProtectionReportRules.MAX_TRACKERS_PER_SITE, days.single().trackersBySite.getValue("news.com").size)
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
