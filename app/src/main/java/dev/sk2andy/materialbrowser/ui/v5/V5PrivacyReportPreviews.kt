package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.data.ProtectionSiteTrackers
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.data.ProtectionTracker
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 privacy report as canvas screen «л1» (30 days), in light and dark, and an empty week. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val PreviewDaily = listOf(
    18, 26, 22, 30, 44, 38, 20, 24, 52, 48, 30, 28, 36, 60, 42,
    34, 26, 30, 40, 58, 46, 38, 30, 22, 28, 44, 50, 62, 48, 40,
)

private val PreviewSummary = ProtectionSummary(
    total = 4912,
    siteCount = 86,
    trackerCount = 143,
    daily = PreviewDaily,
    topSites = emptyList(),
    topTrackers = listOf(ProtectionTracker("adtrack.example", blocked = 812, siteCount = 38)),
    sitesWithMostTrackers = listOf(ProtectionSiteTrackers("novosti-dnya.ru", trackerCount = 14)),
    firstEpochDay = 0,
    lastEpochDay = 29,
)

private val EmptyWeek = ProtectionSummary(
    total = 0,
    siteCount = 0,
    trackerCount = 0,
    daily = List(7) { 0 },
    topSites = emptyList(),
    topTrackers = emptyList(),
    sitesWithMostTrackers = emptyList(),
    firstEpochDay = 0,
    lastEpochDay = 6,
)

@Composable
private fun ReportPreview(dark: Boolean, summary: ProtectionSummary, windowDays: Int, hasData: Boolean = summary.total > 0) {
    VolaV5Theme(dark = dark) {
        V5PrivacyReport(
            visible = true,
            summary = summary,
            windowDays = windowDays,
            onWindowChange = {},
            onClear = {},
            onDismiss = {},
            onHideCard = {},
            hasData = hasData,
        )
    }
}

@Preview(name = "Privacy report · 30 days · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivacyReportLightPreview() {
    ReportPreview(dark = false, summary = PreviewSummary, windowDays = 30)
}

@Preview(name = "Privacy report · 30 days · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivacyReportDarkPreview() {
    ReportPreview(dark = true, summary = PreviewSummary, windowDays = 30)
}

@Preview(name = "Privacy report · empty week · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivacyReportEmptyPreview() {
    ReportPreview(dark = true, summary = EmptyWeek, windowDays = 7)
}

/** An empty week with older days kept: «Clear» stays on, since it deletes every kept day (PR-01). */
@Preview(name = "Privacy report · empty week, older data · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivacyReportEmptyWeekOlderDataPreview() {
    ReportPreview(dark = false, summary = EmptyWeek, windowDays = 7, hasData = true)
}
