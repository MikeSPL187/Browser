package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabActionsRulesTest {
    private val page = TabActionsFacts(
        isWebPage = true,
        isHttpPage = true,
        isIncognito = false,
        isPinned = false,
        isEssential = false,
        canAddEssential = true,
        isBookmarked = false,
        canToggleSiteMute = true,
        isSiteMuted = false,
        canDelete = true,
        canCloseAll = true,
        otherWorkspaceCount = 2,
    )

    @Test
    fun `a web page gets the board's four quick actions, pinning in place of Split View`() {
        assertEquals(
            listOf(
                TabQuickAction.AddToEssentials,
                TabQuickAction.Duplicate,
                TabQuickAction.Pin,
                TabQuickAction.Share,
            ),
            TabActionsRules.quickActions(page),
        )
        assertEquals(
            listOf(
                TabQuickAction.RemoveFromEssentials,
                TabQuickAction.Duplicate,
                TabQuickAction.Unpin,
                TabQuickAction.Share,
            ),
            TabActionsRules.quickActions(page.copy(isEssential = true, isPinned = true)),
        )
    }

    @Test
    fun `a full Essentials grid, a private tab and a blank tab offer only what works`() {
        assertEquals(
            listOf(TabQuickAction.Duplicate, TabQuickAction.Pin, TabQuickAction.Share),
            TabActionsRules.quickActions(page.copy(canAddEssential = false)),
        )
        assertEquals(
            listOf(TabQuickAction.Duplicate, TabQuickAction.Pin, TabQuickAction.Share),
            TabActionsRules.quickActions(page.copy(isIncognito = true)),
        )
        assertEquals(
            listOf(TabQuickAction.Pin),
            TabActionsRules.quickActions(page.copy(isWebPage = false, isHttpPage = false)),
        )
    }

    @Test
    fun `more keeps every Candy action that applies`() {
        assertEquals(
            listOf(
                TabMoreAction.AddBookmark,
                TabMoreAction.MuteSite,
                TabMoreAction.OpenInApp,
                TabMoreAction.Print,
                TabMoreAction.Summarize,
                TabMoreAction.CandyTrail,
                TabMoreAction.SiteCapsule,
                TabMoreAction.CloseAll,
            ),
            TabActionsRules.moreActions(page),
        )
        assertEquals(
            listOf(TabMoreAction.CloseAll),
            TabActionsRules.moreActions(
                page.copy(isWebPage = false, isHttpPage = false, canToggleSiteMute = false),
            ),
        )
        assertEquals(
            TabMoreAction.RemoveBookmark,
            TabActionsRules.moreActions(page.copy(isBookmarked = true)).first(),
        )
        assertTrue(
            TabMoreAction.UnmuteSite in TabActionsRules.moreActions(page.copy(isSiteMuted = true)),
        )
    }

    @Test
    fun `move, snooze and close follow the tab`() {
        assertTrue(TabActionsRules.canMove(page))
        assertFalse(TabActionsRules.canMove(page.copy(otherWorkspaceCount = 0)))
        assertTrue(TabActionsRules.canSnooze(page))
        assertFalse(TabActionsRules.canSnooze(page.copy(isIncognito = true)))
        assertFalse(TabActionsRules.canClose(page.copy(canDelete = false)))
    }
}
