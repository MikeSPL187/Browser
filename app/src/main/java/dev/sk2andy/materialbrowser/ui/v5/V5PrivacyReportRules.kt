package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.data.ProtectionReportRules
import dev.sk2andy.materialbrowser.data.ProtectionSiteTrackers
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.data.ProtectionTracker

/*
 * What the v5 privacy report (V18, canvas screen «л1», system-v5.md section 12) shows of a summary:
 * no more than three numbers — the blocks of the period, the most frequent tracker and the site
 * with the most trackers — and a bar per day.
 */
object V5PrivacyReportRules {
    /** The periods to choose from: a week, and every day the report keeps. */
    val WINDOWS = listOf(ProtectionReportRules.DAYS, ProtectionReportRules.KEPT_DAYS)

    /** A day without blocks still shows a sliver, so the chart keeps its rhythm. */
    const val EMPTY_BAR = 0.04f

    /** A day with a few blocks is never thinner than this beside a busy one. */
    const val MIN_BAR = 0.08f

    /** The bars of the days before today, in the accent at this strength; today is in full. */
    const val PAST_BAR_ALPHA = 0.3f

    /** Each day's bar as a fraction of the chart's height: the busiest day fills it. */
    fun barFractions(daily: List<Int>): List<Float> {
        val most = daily.maxOrNull() ?: 0
        return daily.map { count ->
            when {
                count <= 0 || most <= 0 -> EMPTY_BAR
                else -> (count.toFloat() / most).coerceAtLeast(MIN_BAR)
            }
        }
    }

    /** The tracker blocked most often in the period, if any. */
    fun topTracker(summary: ProtectionSummary): ProtectionTracker? =
        summary.topTrackers.firstOrNull { it.blocked > 0 && it.siteCount > 0 }

    /** The site that tried to load the most different trackers, if any. */
    fun busiestSite(summary: ProtectionSummary): ProtectionSiteTrackers? =
        summary.sitesWithMostTrackers.firstOrNull { it.trackerCount > 0 }

    /** The chosen period, or the week when [windowDays] is not one of [WINDOWS]. */
    fun windowIndex(windowDays: Int): Int = WINDOWS.indexOf(windowDays).coerceAtLeast(0)
}
