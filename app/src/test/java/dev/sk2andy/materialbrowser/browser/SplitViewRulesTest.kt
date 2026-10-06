package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitViewRulesTest {
    @Test
    fun `divider snaps to thirds and half and closes near an edge`() {
        assertEquals(SplitViewRules.THIRD, SplitViewRules.settle(0.3f))
        assertEquals(SplitViewRules.HALF, SplitViewRules.settle(0.45f))
        assertEquals(SplitViewRules.HALF, SplitViewRules.settle(0.55f))
        assertEquals(SplitViewRules.TWO_THIRDS, SplitViewRules.settle(0.7f))
        assertEquals(SplitViewRules.THIRD, SplitViewRules.settle(0.2f))
        assertNull(SplitViewRules.settle(0.1f))
        assertNull(SplitViewRules.settle(0.9f))
        assertEquals(SplitViewRules.DRAG_EDGE, SplitViewRules.dragRatio(-1f))
        assertEquals(1f - SplitViewRules.DRAG_EDGE, SplitViewRules.dragRatio(2f))
    }

    @Test
    fun `frames split the card around the divider`() {
        val card = BrowserContentFrame(leftPx = 8, topPx = 100, rightPx = 8, bottomPx = 200)
        // 1000 px window: 700 px card, 20 px divider, 680 px shared by the panes.
        val frames = SplitViewRules.frames(card, heightPx = 1000, dividerPx = 20, topRatio = 0.25f)

        assertEquals(card.copy(bottomPx = 200 + 20 + 510), frames.top)
        assertEquals(card.copy(topPx = 100 + 170 + 20), frames.bottom)
        assertEquals(frames.top, frames.of(SplitPane.Top))
        assertEquals(frames.bottom, frames.of(SplitPane.Bottom))
        // The panes and the divider fill the card exactly.
        val topHeight = 1000 - frames.top.topPx - frames.top.bottomPx
        val bottomHeight = 1000 - frames.bottom.topPx - frames.bottom.bottomPx
        assertEquals(700, topHeight + 20 + bottomHeight)
    }

    @Test
    fun `companion is the most recent other tab with a page of the same kind`() {
        val tabs = listOf(
            BrowserTab(id = "selected", lastAccessedAt = 50, url = "https://a.example/"),
            BrowserTab(id = "old", lastAccessedAt = 10, url = "https://b.example/"),
            BrowserTab(id = "recent", lastAccessedAt = 40, url = "https://c.example/"),
            BrowserTab(id = "blank", lastAccessedAt = 45),
            BrowserTab(
                id = "private",
                lastAccessedAt = 49,
                url = "https://d.example/",
                isIncognito = true,
            ),
        )

        assertEquals("recent", SplitViewRules.companionFor(tabs, "selected"))
        assertNull(SplitViewRules.companionFor(tabs, "private"))
        assertNull(SplitViewRules.companionFor(tabs, "missing"))
        // A requested tab wins over the most recent one, but only of the same kind.
        assertEquals("old", SplitViewRules.companionFor(tabs, "selected", requestedTabId = "old"))
        assertNull(SplitViewRules.companionFor(tabs, "selected", requestedTabId = "private"))
        assertNull(SplitViewRules.companionFor(tabs, "private", requestedTabId = "recent"))
        assertEquals(
            "recent",
            SplitViewRules.companionFor(tabs, "selected", requestedTabId = "selected"),
        )
    }

    @Test
    fun `split view closes when its tabs go`() {
        val state = SplitViewState(companionTabId = "b")
        val a = BrowserTab(id = "a", lastAccessedAt = 0)
        val b = BrowserTab(id = "b", lastAccessedAt = 0)

        assertEquals(state, SplitViewRules.reconcile(state, listOf(a, b), a))
        assertNull(SplitViewRules.reconcile(state, listOf(a), a))
        assertNull(SplitViewRules.reconcile(state, listOf(a, b), b))
        assertNull(SplitViewRules.reconcile(null, listOf(a, b), a))
    }

    @Test
    fun `split view closes when the selected tab is of another kind than the companion`() {
        val regular = BrowserTab(id = "regular", lastAccessedAt = 0)
        val private = BrowserTab(id = "private", lastAccessedAt = 0, isIncognito = true)
        val otherPrivate = BrowserTab(id = "other", lastAccessedAt = 0, isIncognito = true)
        val tabs = listOf(regular, private, otherPrivate)
        val privateCompanion = SplitViewState(companionTabId = "private")

        // «Regular tabs» on the lock screen selects a regular tab: the private pair closes.
        assertNull(SplitViewRules.reconcile(privateCompanion, tabs, regular))
        assertEquals(
            privateCompanion,
            SplitViewRules.reconcile(privateCompanion, tabs, otherPrivate),
        )
        val regularCompanion = SplitViewState(companionTabId = "regular")
        assertNull(SplitViewRules.reconcile(regularCompanion, tabs, private))
        assertTrue(SplitViewRules.pairs(private, otherPrivate))
        assertFalse(SplitViewRules.pairs(regular, private))
        assertFalse(SplitViewRules.pairs(regular, regular))
    }

    @Test
    fun `activating the other pane selects its tab and keeps both in place`() {
        val split = SplitViewController()
        split.open("b")
        val selected = mutableListOf<String>()

        split.activateCompanion(selectedTabId = "a", select = selected::add)

        assertEquals(listOf("b"), selected)
        assertEquals(SplitViewState(companionTabId = "a", activePane = SplitPane.Bottom), split.state)

        split.swap()
        assertEquals(SplitPane.Top, split.state?.activePane)
        split.updateRatio(SplitViewRules.THIRD)
        assertEquals(SplitViewRules.THIRD, split.state?.topRatio)
        val b = BrowserTab(id = "b", lastAccessedAt = 0)
        split.reconcile(activeTabs = listOf(b), selectedTab = b)
        assertNull(split.state)
    }

    @Test
    fun `a replaced companion leaves its pane`() {
        val left = mutableListOf<String>()
        val split = SplitViewController(onCompanionLeft = left::add)

        split.open("b")
        split.open("b")
        assertEquals(emptyList<String>(), left)

        // «Side by side» on C while A and B share the screen: B goes to the background.
        split.open("c")
        assertEquals(listOf("b"), left)
        assertEquals("c", split.companionTabId)
        assertEquals(3, split.openRequests)

        split.close()
        assertEquals(listOf("b", "c"), left)
    }

    @Test
    fun `companion pane shows a stopped page and leaves a live one alone`() {
        val live = BrowserTab(id = "b", lastAccessedAt = 0, url = "https://b.example/")
        fun failed(kind: BrowserEngineFailureKind) = live.copy(failureKind = kind)

        assertNull(SplitViewRules.companionStatus(live, blocked = false))
        assertEquals(
            SplitCompanionStatus.SiteBlocked,
            SplitViewRules.companionStatus(live, blocked = true),
        )
        assertEquals(
            SplitCompanionStatus.SiteBlocked,
            SplitViewRules.companionStatus(
                failed(BrowserEngineFailureKind.DangerousSite),
                blocked = false,
            ),
        )
        listOf(
            BrowserEngineFailureKind.Offline,
            BrowserEngineFailureKind.UnknownHost,
            BrowserEngineFailureKind.InsecureConnection,
            BrowserEngineFailureKind.Other,
        ).forEach { kind ->
            assertEquals(
                SplitCompanionStatus.PageFailed,
                SplitViewRules.companionStatus(failed(kind), blocked = false),
            )
        }
        // The engine's own HTTPS-only page stays visible.
        assertNull(
            SplitViewRules.companionStatus(failed(BrowserEngineFailureKind.HttpsOnly), false),
        )
    }
}
