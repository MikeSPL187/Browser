package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.data.ProtectionSiteTrackers
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.data.ProtectionTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
