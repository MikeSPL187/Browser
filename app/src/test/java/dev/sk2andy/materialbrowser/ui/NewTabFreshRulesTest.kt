package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewTabFreshRulesTest {
    private val current = BrowserTab("new", 0L, profileId = "study")
    private val page = "https://example.com/"

    private fun fresh(
        profileCount: Int = 2,
        favorites: List<FavoriteEntry> = emptyList(),
        history: List<HistoryEntry> = emptyList(),
        tabs: List<BrowserTab> = listOf(current),
    ) = NewTabFreshRules.isFresh("study", profileCount, favorites, history, tabs, current.id)

    @Test
    fun `a new profile with nothing of its own is fresh`() {
        assertTrue(fresh())
        // Blank and private tabs, and other profiles' pages and history, are not its own.
        assertTrue(
            fresh(
                history = listOf(HistoryEntry(page, "", 0L, profileId = "home")),
                tabs = listOf(
                    current,
                    BrowserTab("blank", 0L, profileId = "study"),
                    BrowserTab("private", 0L, profileId = "study", isIncognito = true, url = page),
                    BrowserTab("home", 0L, profileId = "home", url = page),
                ),
            ),
        )
    }

    @Test
    fun `anything of its own ends the fresh start`() {
        assertFalse(fresh(profileCount = 1))
        assertFalse(fresh(favorites = listOf(FavoriteEntry(url = page, title = "", addedAt = 0L))))
        assertFalse(fresh(history = listOf(HistoryEntry(page, "", 0L, profileId = "study"))))
        assertFalse(fresh(tabs = listOf(current, BrowserTab("open", 0L, profileId = "study", url = page))))
    }

    @Test
    fun `only open pages of unlocked other profiles can move here`() {
        val tabs = listOf(
            current,
            BrowserTab("home", 0L, profileId = "home", url = page),
            BrowserTab("home-blank", 0L, profileId = "home"),
            BrowserTab("home-private", 0L, profileId = "home", isIncognito = true, url = page),
            BrowserTab("vault", 0L, profileId = "vault", url = page),
            BrowserTab("study", 0L, profileId = "study", url = page),
        )

        assertEquals(
            listOf("home"),
            NewTabFreshRules.movableTabs("study", tabs, lockedProfileIds = setOf("vault")).map(BrowserTab::id),
        )
    }
}
