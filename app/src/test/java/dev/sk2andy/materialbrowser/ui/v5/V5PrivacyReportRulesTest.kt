package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.data.ProtectionSiteTrackers
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.data.ProtectionTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class V5PrivacyReportRulesTest {
    private fun summary(
        trackers: List<ProtectionTracker> = emptyList(),
        sites: List<ProtectionSiteTrackers> = emptyList(),
    ) = ProtectionSummary(
        total = 10,
        siteCount = 2,
        trackerCount = trackers.size,
        daily = listOf(0, 10),
        topSites = emptyList(),
        topTrackers = trackers,
        sitesWithMostTrackers = sites,
        firstEpochDay = 1,
        lastEpochDay = 2,
    )

    @Test
    fun `the busiest day fills the chart and quiet days keep a sliver`() {
        assertEquals(
            listOf(V5PrivacyReportRules.EMPTY_BAR, 0.5f, 1f, V5PrivacyReportRules.MIN_BAR),
            V5PrivacyReportRules.barFractions(listOf(0, 50, 100, 1)),
        )
    }

    @Test
    fun `a period without blocks is all slivers`() {
        assertEquals(
            List(7) { V5PrivacyReportRules.EMPTY_BAR },
            V5PrivacyReportRules.barFractions(List(7) { 0 }),
        )
        assertEquals(emptyList<Float>(), V5PrivacyReportRules.barFractions(emptyList()))
    }

    @Test
    fun `the findings skip empty entries`() {
        assertNull(V5PrivacyReportRules.topTracker(summary()))
        assertNull(V5PrivacyReportRules.busiestSite(summary(sites = listOf(ProtectionSiteTrackers("a.example", 0)))))
        val tracker = ProtectionTracker("ads.example", blocked = 12, siteCount = 3)
        val site = ProtectionSiteTrackers("news.example", trackerCount = 7)
        assertEquals(tracker, V5PrivacyReportRules.topTracker(summary(trackers = listOf(tracker))))
        assertEquals(site, V5PrivacyReportRules.busiestSite(summary(sites = listOf(site))))
    }

    @Test
    fun `the periods are a week and every kept day`() {
        assertEquals(listOf(7, 30), V5PrivacyReportRules.WINDOWS)
        assertEquals(1, V5PrivacyReportRules.windowIndex(30))
        assertEquals(0, V5PrivacyReportRules.windowIndex(14))
    }

    @Test
    fun `the chart's description gives today, the busiest day and the daily average`() {
        val week = listOf(2, 9, 0, 4, 6, 3, 4)
        assertEquals(V5PrivacyReportRules.ChartFacts(today = 4, most = 9, average = 4), V5PrivacyReportRules.chartFacts(week))
        assertEquals(V5PrivacyReportRules.ChartFacts(today = 0, most = 0, average = 0), V5PrivacyReportRules.chartFacts(emptyList()))
    }

    @Test
    fun `a week names its days ending today, a month names none`() {
        // 2026-10-23 is a Friday.
        val friday = LocalDate.of(2026, 10, 23).toEpochDay()
        assertEquals(
            listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"),
            V5PrivacyReportRules.dayLabels(7, friday, Locale.ENGLISH),
        )
        assertEquals("Пт", V5PrivacyReportRules.dayLabels(7, friday, Locale.forLanguageTag("ru")).last())
        assertEquals(emptyList<String>(), V5PrivacyReportRules.dayLabels(30, friday, Locale.ENGLISH))
    }
}
