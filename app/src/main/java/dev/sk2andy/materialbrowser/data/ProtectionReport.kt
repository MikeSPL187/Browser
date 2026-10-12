package dev.sk2andy.materialbrowser.data

import java.net.URI
import java.util.Locale

/**
 * Requests blocked on one day, by the site the user was on. Kept only on the device; private
 * tabs never reach it. [total] counts every block of the day, also those of sites dropped past
 * [ProtectionReportRules.MAX_SITES_PER_DAY], so it is never less than the sum of [blockedBySite].
 * [blockedByTracker] counts the blocks by the tracker's site (the registrable domain of the blocked
 * request), and [trackersBySite] which trackers each site tried to load (V18, as in Safari).
 */
data class ProtectionDay(
    val epochDay: Long,
    val blockedBySite: Map<String, Int>,
    val total: Int = blockedBySite.values.fold(0) { sum, count -> sum.saturatedPlus(count) },
    val blockedByTracker: Map<String, Int> = emptyMap(),
    val trackersBySite: Map<String, Set<String>> = emptyMap(),
)

/** What the new tab card and the weekly report show. */
data class ProtectionWeek(
    val total: Int,
    val siteCount: Int,
    /** Seven days, the oldest first and today last. */
    val daily: List<Int>,
    /** Sites with the most blocked requests, most first. */
    val topSites: List<ProtectionSite>,
    val firstEpochDay: Long,
    val lastEpochDay: Long,
) {
    companion object {
        fun empty(today: Long) = ProtectionWeek(
            total = 0,
            siteCount = 0,
            daily = List(ProtectionReportRules.DAYS) { 0 },
            topSites = emptyList(),
            firstEpochDay = today - ProtectionReportRules.DAYS + 1,
            lastEpochDay = today,
        )
    }
}

data class ProtectionSite(val host: String, val blocked: Int)

/** A tracker of the privacy report: how often it was blocked and on how many sites. */
data class ProtectionTracker(val host: String, val blocked: Int, val siteCount: Int)

/** A site of the privacy report with the number of different trackers it tried to load. */
data class ProtectionSiteTrackers(val host: String, val trackerCount: Int)

/** The privacy report over the last [daily].size days (V18): what the blocker stopped and where. */
data class ProtectionSummary(
    val total: Int,
    val siteCount: Int,
    val trackerCount: Int,
    /** One count per day, the oldest first and today last. */
    val daily: List<Int>,
    /** Sites with the most blocked requests, most first. */
    val topSites: List<ProtectionSite>,
    /** Trackers blocked most often, most first. */
    val topTrackers: List<ProtectionTracker>,
    /** Sites that tried to load the most different trackers, most first. */
    val sitesWithMostTrackers: List<ProtectionSiteTrackers>,
    val firstEpochDay: Long,
    val lastEpochDay: Long,
)

/** The pure rules of the protection report: the weekly card (П7) and the privacy report (V18). */
object ProtectionReportRules {
    /** Days of the new tab card's week. */
    const val DAYS = 7

    /** Days the report keeps, as Safari's privacy report does. */
    const val KEPT_DAYS = 30

    /** Sites kept per day; the least blocked go first, so a day stays a few kilobytes. */
    const val MAX_SITES_PER_DAY = 200

    /** Trackers kept per day, the least blocked going first. */
    const val MAX_TRACKERS_PER_DAY = 200

    /** Different trackers kept per site and day. */
    const val MAX_TRACKERS_PER_SITE = 64
    const val TOP_SITES = 5

    /** Entries of each list of the privacy report. */
    const val TOP_ENTRIES = 10

    /**
     * Whether blocks on a tab reach the report. Private tabs never do, and neither do tabs of a
     * workspace behind a lock: the report is shared by every workspace and lists sites, so it
     * would show what the locked one visited.
     */
    fun countsTab(isPrivate: Boolean, workspaceProtected: Boolean): Boolean =
        !isPrivate && !workspaceProtected

    /** The site a tab is on, without `www.`; `null` for pages that are not on the web. */
    fun site(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null
        return uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")?.takeIf(String::isNotBlank)
    }

    /**
     * Adds [blocked] requests on [site] today, [trackers] being the blocked requests by tracker
     * (some or all of them), and drops days the report no longer keeps.
     */
    fun record(
        days: List<ProtectionDay>,
        today: Long,
        site: String,
        blocked: Int,
        trackers: Map<String, Int> = emptyMap(),
    ): List<ProtectionDay> {
        if (blocked <= 0) return prune(days, today)
        val day = days.firstOrNull { it.epochDay == today }
        val current = day?.blockedBySite.orEmpty()
        val siteCount = (current[site] ?: 0).saturatedPlus(blocked)
        val sites = mostBlocked(current + (site to siteCount), MAX_SITES_PER_DAY)
        val byTracker = day?.blockedByTracker.orEmpty().toMutableMap()
        trackers.forEach { (tracker, count) ->
            if (count > 0) byTracker[tracker] = (byTracker[tracker] ?: 0).saturatedPlus(count)
        }
        val siteTrackers = day?.trackersBySite.orEmpty().toMutableMap()
        if (trackers.isNotEmpty()) {
            val known = siteTrackers[site].orEmpty()
            val room = (MAX_TRACKERS_PER_SITE - known.size).coerceAtLeast(0)
            siteTrackers[site] = known + trackers.keys.filterNot(known::contains).take(room)
        }
        val updated = ProtectionDay(
            epochDay = today,
            blockedBySite = sites,
            total = (day?.total ?: 0).saturatedPlus(blocked),
            blockedByTracker = mostBlocked(byTracker, MAX_TRACKERS_PER_DAY),
            trackersBySite = siteTrackers.filterKeys(sites::containsKey),
        )
        return prune(days.filterNot { it.epochDay == today } + updated, today)
    }

    /** Keeps the last [KEPT_DAYS] days up to today, oldest first. A clock moved back keeps nothing newer. */
    fun prune(days: List<ProtectionDay>, today: Long): List<ProtectionDay> =
        days.filter { it.epochDay in (today - KEPT_DAYS + 1)..today && it.blockedBySite.isNotEmpty() }
            .sortedBy(ProtectionDay::epochDay)

    fun week(days: List<ProtectionDay>, today: Long): ProtectionWeek {
        val summary = summary(days, today, DAYS, TOP_SITES)
        return ProtectionWeek(
            total = summary.total,
            siteCount = summary.siteCount,
            daily = summary.daily,
            topSites = summary.topSites,
            firstEpochDay = summary.firstEpochDay,
            lastEpochDay = summary.lastEpochDay,
        )
    }

    /** The report over the last [windowDays] days up to today (at most [KEPT_DAYS]). */
    fun summary(
        days: List<ProtectionDay>,
        today: Long,
        windowDays: Int,
        top: Int = TOP_ENTRIES,
    ): ProtectionSummary {
        val window = windowDays.coerceIn(1, KEPT_DAYS)
        val first = today - window + 1
        val kept = prune(days, today).filter { it.epochDay >= first }
        val bySite = mutableMapOf<String, Int>()
        val byTracker = mutableMapOf<String, Int>()
        val sitesByTracker = mutableMapOf<String, MutableSet<String>>()
        val trackersBySite = mutableMapOf<String, MutableSet<String>>()
        kept.forEach { day ->
            day.blockedBySite.forEach { (site, count) -> bySite[site] = (bySite[site] ?: 0).saturatedPlus(count) }
            day.blockedByTracker.forEach { (tracker, count) ->
                byTracker[tracker] = (byTracker[tracker] ?: 0).saturatedPlus(count)
            }
            day.trackersBySite.forEach { (site, trackers) ->
                trackersBySite.getOrPut(site, ::mutableSetOf) += trackers
                trackers.forEach { tracker -> sitesByTracker.getOrPut(tracker, ::mutableSetOf) += site }
            }
        }
        return ProtectionSummary(
            total = kept.fold(0) { sum, day -> sum.saturatedPlus(day.total) },
            siteCount = bySite.size,
            trackerCount = byTracker.size,
            daily = (first..today).map { epochDay -> kept.firstOrNull { it.epochDay == epochDay }?.total ?: 0 },
            topSites = ranked(bySite, top).map { ProtectionSite(it.key, it.value) },
            topTrackers = ranked(byTracker, top).map {
                ProtectionTracker(it.key, it.value, sitesByTracker[it.key]?.size ?: 0)
            },
            sitesWithMostTrackers = ranked(trackersBySite.mapValues { it.value.size }, top)
                .map { ProtectionSiteTrackers(it.key, it.value) },
            firstEpochDay = first,
            lastEpochDay = today,
        )
    }

    private fun ranked(counts: Map<String, Int>, top: Int): List<Map.Entry<String, Int>> =
        counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(top)

    private fun mostBlocked(counts: Map<String, Int>, limit: Int): Map<String, Int> =
        if (counts.size <= limit) counts else counts.entries.sortedByDescending { it.value }.take(limit)
            .associate { it.key to it.value }
}

private fun Int.saturatedPlus(other: Int): Int =
    (toLong() + other).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
