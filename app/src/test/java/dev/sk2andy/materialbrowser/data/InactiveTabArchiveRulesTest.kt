package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InactiveTabArchiveRulesTest {
    @Test
    fun `only lifetimes counted in days archive, and only when enabled`() {
        assertTrue(InactiveTabArchiveRules.applies(InactiveTabLifetime.SevenDays, enabled = true))
        assertFalse(InactiveTabArchiveRules.applies(InactiveTabLifetime.SevenDays, enabled = false))
        assertFalse(InactiveTabArchiveRules.applies(InactiveTabLifetime.Never, enabled = true))
        assertFalse(InactiveTabArchiveRules.applies(InactiveTabLifetime.Immediately, enabled = true))
        assertFalse(InactiveTabArchiveRules.applies(InactiveTabLifetime.WhenAppCloses, enabled = true))
    }

    @Test
    fun `stale tabs join the snoozed list as archived, after the ones that wake`() {
        val waking = SnoozedTab(BrowserTab("waking", 1L), wakeAtMillis = 500L, createdAtMillis = 1L)
        val stale = BrowserTab("stale", 2L, url = "https://example.com/", isLoading = true)

        val result = InactiveTabArchiveRules.archived(listOf(stale), listOf(waking), nowMillis = 900L)

        assertEquals(listOf("waking", "stale"), result.map { it.tab.id })
        val archived = result.last()
        assertTrue(archived.isArchived)
        assertEquals(900L, archived.createdAtMillis)
        assertEquals("https://example.com/", archived.tab.url)
        assertFalse(archived.tab.isLoading)
        assertFalse(waking.isArchived)
    }

    @Test
    fun `an archived tab forgets its last load failure`() {
        val failed = BrowserTab(
            "failed",
            2L,
            error = "Not found",
            httpStatusCode = 404,
            failureKind = BrowserEngineFailureKind.Other,
        )

        val archived = InactiveTabArchiveRules.archived(listOf(failed), emptyList(), 900L).single()

        assertNull(archived.tab.error)
        assertNull(archived.tab.httpStatusCode)
        assertNull(archived.tab.failureKind)
    }

    @Test
    fun `a tab archived again replaces its old entry`() {
        val old = SnoozedTab(BrowserTab("tab", 1L), wakeAtMillis = 500L, createdAtMillis = 1L)

        val result = InactiveTabArchiveRules.archived(listOf(BrowserTab("tab", 1L)), listOf(old), 9L)

        assertEquals(1, result.size)
        assertTrue(result.single().isArchived)
    }

    @Test
    fun `private tabs are never archived`() {
        val private = BrowserTab("private", 1L, isIncognito = true)

        assertFalse(InactiveTabArchiveRules.canArchive(private))
        assertTrue(InactiveTabArchiveRules.archived(listOf(private), emptyList(), 900L).isEmpty())
    }

    @Test
    fun `archived tabs never set the wake alarm`() {
        val archived = InactiveTabArchiveRules.archived(listOf(BrowserTab("a", 1L)), emptyList(), 900L)
        val waking = SnoozedTab(BrowserTab("w", 1L), wakeAtMillis = 2_000L, createdAtMillis = 1L)

        assertNull(SnoozeScheduleRules.nextTriggerAt(archived, 1_000L))
        assertEquals(2_000L, SnoozeScheduleRules.nextTriggerAt(archived + waking, 1_000L))
    }

    @Test
    fun `archived tabs are not due for restore`() {
        val archived = InactiveTabArchiveRules.archived(listOf(BrowserTab("a", 1L)), emptyList(), 900L)

        val result = SnoozeRestoreRules.restoreDue(
            tabs = emptyList(),
            snoozedTabs = archived,
            profiles = listOf(BrowserProfile("candy", "🍬")),
            activeProfileId = "candy",
            nowMillis = Long.MAX_VALUE - 1,
            maxTabs = 10,
        )

        assertTrue(result.restoredTabIds.isEmpty())
    }
}
