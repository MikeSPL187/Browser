package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceSheetRulesTest {
    @Test
    fun `the summary names Essentials only when the workspace has some`() {
        assertEquals("6 tabs · 5 in Essentials", WorkspaceSheetRules.summary("6 tabs", "5 in Essentials"))
        assertEquals("6 tabs", WorkspaceSheetRules.summary("6 tabs", null))
    }

    @Test
    fun `only the workspace's own regular tabs are counted`() {
        val tabs = listOf(
            BrowserTab("a", 0L, profileId = "work"),
            BrowserTab("b", 0L, profileId = "work"),
            BrowserTab("c", 0L, profileId = "work", isIncognito = true),
            BrowserTab("d", 0L, profileId = "home"),
        )

        assertEquals(2, WorkspaceSheetRules.tabCount(tabs, "work"))
        assertEquals(0, WorkspaceSheetRules.tabCount(tabs, "anime"))
    }

    @Test
    fun `a new workspace starts with the first icon`() {
        assertEquals("💼", WorkspaceSheetRules.defaultIcon(listOf("💼", "🏠")))
        assertNull(WorkspaceSheetRules.defaultIcon(emptyList()))
    }

    @Test
    fun `the collapsed grid keeps the chosen icon in view`() {
        val icons = listOf("a", "b", "c", "d", "e")

        assertEquals(listOf("a", "b", "c"), WorkspaceSheetRules.collapsedIcons(icons, "b", limit = 3))
        assertEquals(listOf("a", "b", "e"), WorkspaceSheetRules.collapsedIcons(icons, "e", limit = 3))
        assertEquals(listOf("a", "b", "c"), WorkspaceSheetRules.collapsedIcons(icons, null, limit = 3))
        assertEquals(listOf("a", "b", "c"), WorkspaceSheetRules.collapsedIcons(icons, "z", limit = 3))
    }
}
