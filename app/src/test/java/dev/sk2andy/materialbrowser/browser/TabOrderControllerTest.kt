package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabOrderControllerTest {
    private val tabs = mutableListOf(
        BrowserTab("a", 0L, profileId = "work"),
        BrowserTab("x", 0L, profileId = "home"),
        BrowserTab("b", 0L, profileId = "work"),
        BrowserTab("c", 0L, profileId = "work"),
    )
    private var sorting = false
    private val ephemeral = mutableSetOf<String>()
    private val pinnedEvents = mutableListOf<Pair<String, Boolean>>()
    private val orderEvents = mutableListOf<String>()
    private var saves = 0
    private val controller = TabOrderController(
        tabs = tabs,
        activeTabs = { tabs.filter { it.profileId == "work" } },
        activeProfileId = { "work" },
        automaticSorting = { sorting },
        isEphemeral = { it in ephemeral },
        onPinnedChanged = { id, pinned -> pinnedEvents += id to pinned },
        onOrderChanged = { orderEvents += it },
        persist = { saves++ },
    )

    private fun workIds() = tabs.filter { it.profileId == "work" }.map(BrowserTab::id)

    @Test
    fun `pinning moves the tab ahead of the others and is synced and saved`() {
        assertTrue(controller.setPinned("c", true))

        assertEquals(listOf("c", "a", "b"), workIds())
        assertEquals(listOf("c" to true), pinnedEvents)
        assertEquals(1, saves)
    }

    @Test
    fun `pinning what is already pinned changes nothing`() {
        controller.setPinned("c", true)

        assertFalse(controller.setPinned("c", true))
        assertEquals(1, saves)
    }

    @Test
    fun `a move regroups the workspace's tabs where its first tab was`() {
        assertTrue(controller.move("a", 2))

        assertEquals(listOf("b", "c", "a", "x"), tabs.map(BrowserTab::id))
        assertEquals(listOf("work"), orderEvents)
    }

    @Test
    fun `sorting by recent use and session-only tabs block manual moves`() {
        sorting = true
        assertFalse(controller.move("a", 2))
        sorting = false
        ephemeral += "a"
        assertFalse(controller.move("a", 2))
        assertFalse(controller.setPinned("a", true))
        assertEquals(0, saves)
    }

    @Test
    fun `an extension's tab lands where asked, but only in the current workspace`() {
        assertTrue(controller.positionCreatedTab("c", 0))
        assertEquals(listOf("c", "a", "b"), workIds())
        assertFalse(controller.positionCreatedTab("x", 0))
        assertTrue(controller.positionCreatedTab("c", 0))
        assertEquals(1, saves)
    }
}
