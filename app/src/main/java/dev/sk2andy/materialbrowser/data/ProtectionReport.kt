package dev.sk2andy.materialbrowser.data

import java.net.URI
import java.util.Locale

/**
 * Requests blocked on one day, by the site the user was on. Kept only on the device; private
 * tabs never reach it. [total] counts every block of the day, also those of sites dropped past
 * [ProtectionReportRules.MAX_SITES_PER_DAY], so it is never less than the sum of [blockedBySite].
 */
data class ProtectionDay(
    val epochDay: Long,
    val blockedBySite: Map<String, Int>,
    val total: Int = blockedBySite.values.fold(0) { sum, count -> sum.saturatedPlus(count) },
)

/** What the new tab card and the weekly report show. */
data class ProtectionWeek(
    val total: Int,
    val siteCount: Int,
    /** Seven days, the oldest first and today last. */
    val daily: List<Int>,
    /** Sites with the most blocked trackers, most first. */
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

/** The pure rules of the weekly protection report (П7). */
object ProtectionReportRules {
    const val DAYS = 7

    /** Sites kept per day; the least blocked go first, so a day stays a few kilobytes. */
    const val MAX_SITES_PER_DAY = 200
    const val TOP_SITES = 5

    /** The site a tab is on, without `www.`; `null` for pages that are not on the web. */
    fun site(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null
        return uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")?.takeIf(String::isNotBlank)
    }

    /** Adds [blocked] trackers on [site] today and drops days that left the week. */
    fun record(
        days: List<ProtectionDay>,
        today: Long,
        site: String,
        blocked: Int,
    ): List<ProtectionDay> {
        if (blocked <= 0) return prune(days, today)
        val day = days.firstOrNull { it.epochDay == today }
        val current = day?.blockedBySite.orEmpty()
        var updated = current + (site to (current[site] ?: 0).saturatedPlus(blocked))
        if (updated.size > MAX_SITES_PER_DAY) {
            updated = updated.entries
                .sortedByDescending { it.value }
                .take(MAX_SITES_PER_DAY)
                .associate { it.key to it.value }
        }
        val total = (day?.total ?: 0).saturatedPlus(blocked)
        return prune(days.filterNot { it.epochDay == today } + ProtectionDay(today, updated, total), today)
    }

    /** Keeps the last [DAYS] days up to today, oldest first. A clock moved back keeps nothing newer. */
    fun prune(days: List<ProtectionDay>, today: Long): List<ProtectionDay> =
        days.filter { it.epochDay in (today - DAYS + 1)..today && it.blockedBySite.isNotEmpty() }
            .sortedBy(ProtectionDay::epochDay)

    fun week(days: List<ProtectionDay>, today: Long): ProtectionWeek {
        val kept = prune(days, today)
        if (kept.isEmpty()) return ProtectionWeek.empty(today)
        val bySite = mutableMapOf<String, Int>()
        kept.forEach { day ->
            day.blockedBySite.forEach { (site, count) ->
                bySite[site] = (bySite[site] ?: 0).saturatedPlus(count)
            }
        }
        val first = today - DAYS + 1
        return ProtectionWeek(
            total = kept.fold(0) { sum, day -> sum.saturatedPlus(day.total) },
            siteCount = bySite.size,
            daily = (first..today).map { epochDay -> kept.firstOrNull { it.epochDay == epochDay }?.total ?: 0 },
            topSites = bySite.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(TOP_SITES)
                .map { ProtectionSite(it.key, it.value) },
            firstEpochDay = first,
            lastEpochDay = today,
        )
    }
}

private fun Int.saturatedPlus(other: Int): Int =
    (toLong() + other).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
