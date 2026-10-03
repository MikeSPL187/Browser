package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabStacksControllerTest {
    private val tabs = listOf("a", "b", "c").map { id -> BrowserTab(id = id, lastAccessedAt = 0L) }
    private var saves = 0
    private val controller = TabStacksController(
        allTabs = { tabs },
        activeTabs = { tabs },
        persist = { saves++ },
    )

    @Test
    fun `a stack groups active tabs and saves the session`() {
        val stackId = controller.create(listOf("a", "b"), "Work", TabStackColor.Lime, null)

        assertNotNull(stackId)
        assertEquals(listOf("a", "b"), controller.stackFor("b")?.tabIds)
        assertEquals(1, saves)
    }

    @Test
    fun `a stack never takes a tab that is not in the workspace`() {
        assertNull(controller.create(listOf("a", "gone"), "Work", TabStackColor.Lime, null))
        assertEquals(0, saves)
    }

    @Test
    fun `edits that change nothing are not saved`() {
        val stackId = controller.create(listOf("a", "b"), "Work", TabStackColor.Lime, null)!!

        assertFalse(controller.addTab("a", stackId))
        assertFalse(controller.removeTab("c"))
        assertFalse(controller.setPreview(stackId, "c"))
        assertEquals(1, saves)
    }

    @Test
    fun `a tab joins and leaves a stack`() {
        val stackId = controller.create(listOf("a", "b"), "Work", TabStackColor.Lime, null)!!

        assertTrue(controller.addTab("c", stackId))
        assertEquals(listOf("a", "b", "c"), controller.stackFor("c")?.tabIds)
        assertTrue(controller.removeTab("c"))
        assertNull(controller.stackFor("c"))
        assertEquals(3, saves)
    }

    @Test
    fun `a collapsed stack shows one card in the overview`() {
        val stackId = controller.create(listOf("a", "b"), "Work", TabStackColor.Lime, null)!!

        assertTrue(controller.toggleCollapsed(stackId, triggerTabId = "a"))

        assertEquals(2, controller.overviewTabs.size)
        assertEquals(controller.overviewTabId("a"), controller.overviewTabId("b"))
    }
}
