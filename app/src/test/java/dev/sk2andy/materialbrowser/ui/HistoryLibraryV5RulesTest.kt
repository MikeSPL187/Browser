package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HistoryLibraryV5RulesTest {
    private val zone = ZoneId.of("Europe/Moscow")
    private val now = ZonedDateTime.of(2026, 10, 16, 11, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `the last hour clears from an hour ago, in the current profile only`() {
        val request = HistoryLibraryV5Rules.clearRequest(V5HistoryClearRange.LastHour, "work", now, zone)
        assertEquals(now - 3_600_000L, request.sinceInclusiveMillis)
        assertEquals(Long.MAX_VALUE, request.untilExclusiveMillis)
        assertEquals(setOf("work"), request.profileIds)
    }

    @Test
    fun `today and yesterday start at local midnight`() {
        val midnight = ZonedDateTime.of(2026, 10, 16, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(midnight, HistoryLibraryV5Rules.clearRequest(V5HistoryClearRange.Today, "p", now, zone).sinceInclusiveMillis)
        assertEquals(
            midnight - 86_400_000L,
            HistoryLibraryV5Rules.clearRequest(V5HistoryClearRange.TodayAndYesterday, "p", now, zone).sinceInclusiveMillis,
        )
    }

    @Test
    fun `all time clears everything of the profile`() {
        assertEquals(Long.MIN_VALUE, HistoryLibraryV5Rules.clearRequest(V5HistoryClearRange.AllTime, "p", now, zone).sinceInclusiveMillis)
    }

    @Test
    fun `a site's letter and tile come from its host`() {
        assertEquals("P", HistoryLibraryV5Rules.siteLetter("www.polar-notes.ru"))
        assertEquals("#", HistoryLibraryV5Rules.siteLetter("--"))
        assertEquals(HistoryLibraryV5Rules.siteTile("polar-notes.ru"), HistoryLibraryV5Rules.siteTile("www.polar-notes.ru"))
        assertNotEquals(V5Tile.Gray, HistoryLibraryV5Rules.siteTile("polar-notes.ru"))
    }
}
