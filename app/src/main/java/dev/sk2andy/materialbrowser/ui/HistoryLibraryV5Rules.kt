package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.HistoryClearRequest
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** The periods «Clear…» offers in the v5 history (V28, B2b): Safari's four, none to type in. */
internal enum class V5HistoryClearRange { LastHour, Today, TodayAndYesterday, AllTime }

internal object HistoryLibraryV5Rules {
    private const val HOUR_MILLIS = 60L * 60L * 1000L

    /** What [range] clears in [profileId] at [nowMillis]: up to and including this moment. */
    fun clearRequest(range: V5HistoryClearRange, profileId: String, nowMillis: Long, zoneId: ZoneId): HistoryClearRequest {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val since = when (range) {
            V5HistoryClearRange.LastHour -> nowMillis - HOUR_MILLIS
            V5HistoryClearRange.Today -> startOf(today, zoneId)
            V5HistoryClearRange.TodayAndYesterday -> startOf(today.minusDays(1), zoneId)
            V5HistoryClearRange.AllTime -> Long.MIN_VALUE
        }
        return HistoryClearRequest(profileIds = setOf(profileId), sinceInclusiveMillis = since, untilExclusiveMillis = Long.MAX_VALUE)
    }

    /** The letter a site without an icon shows: the first letter or digit of its host. */
    fun siteLetter(host: String): String =
        host.removePrefix("www.").firstOrNull(Char::isLetterOrDigit)?.uppercase(Locale.ROOT) ?: "#"

    /** The same host always gets the same tile, gray left for files. */
    fun siteTile(host: String): V5Tile {
        val tiles = V5Tile.entries.filter { it != V5Tile.Gray }
        return tiles[Math.floorMod(host.removePrefix("www.").hashCode(), tiles.size)]
    }

    private fun startOf(day: LocalDate, zoneId: ZoneId): Long = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
}
