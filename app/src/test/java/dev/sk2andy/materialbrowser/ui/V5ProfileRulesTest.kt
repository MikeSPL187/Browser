package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5ProfileRulesTest {
    @Test
    fun `every stored color has a v5 accent`() {
        assertEquals(V5Accent.Purple, V5ProfileRules.accent(WorkspaceAccent.Violet))
        assertEquals(V5Accent.Blue, V5ProfileRules.accent(WorkspaceAccent.Blue))
        assertEquals(V5Accent.Green, V5ProfileRules.accent(WorkspaceAccent.Teal))
        assertEquals(V5Accent.Orange, V5ProfileRules.accent(WorkspaceAccent.Coral))
        assertEquals(V5Accent.Pink, V5ProfileRules.accent(WorkspaceAccent.Rose))
        assertEquals(V5Accent.Graphite, V5ProfileRules.accent(WorkspaceAccent.Graphite))
    }

    @Test
    fun `rows count each profile's tabs and put other devices last`() {
        val profiles = listOf(
            BrowserProfile(id = "laptop", emoji = "💻", syncedDeviceId = "d1", syncedDisplayName = "Laptop"),
            BrowserProfile(id = "personal", emoji = "🏔", name = "Personal"),
            BrowserProfile(id = "work", emoji = "💼", name = "", accent = WorkspaceAccent.Amber),
        )
        val tabs = listOf(
            BrowserTab(id = "a", lastAccessedAt = 0L, profileId = "personal"),
            BrowserTab(id = "b", lastAccessedAt = 0L, profileId = "personal"),
            BrowserTab(id = "c", lastAccessedAt = 0L, profileId = "work"),
        )
        val items = V5ProfileRules.items(profiles, tabs, "personal", setOf("work"), untitled = "Workspace")
        assertEquals(listOf("personal", "work", "laptop"), items.map { it.id })
        // A locked profile does not tell how many tabs it hides (ПР-4).
        assertEquals(listOf(2, null, 0), items.map { it.tabCount })
        assertTrue(items[0].active)
        assertFalse(items[1].active)
        assertTrue(items[1].locked)
        assertEquals("Workspace", items[1].name)
        assertEquals(V5Accent.Orange, items[1].accent)
        assertEquals("Laptop", items[2].device)
        assertEquals("Laptop", items[2].name)
    }

    @Test
    fun `a chosen accent is stored as its twin and reads back the same`() {
        V5Accent.entries.forEach { accent ->
            assertEquals(accent, V5ProfileRules.accent(V5ProfileRules.stored(accent)))
        }
        assertEquals(WorkspaceAccent.Violet, V5ProfileRules.stored(V5Accent.Purple))
        assertEquals(WorkspaceAccent.Amber, V5ProfileRules.stored(V5Accent.Orange))
    }

    @Test
    fun `a saved profile name is never blank and never another profile's`() {
        val others = listOf(
            BrowserProfile(id = "default", emoji = "🏠"),
            BrowserProfile(id = "work", emoji = "💼", name = "Work"),
        )

        assertEquals("Profile 2", V5ProfileRules.distinctName("  ", others, untitled = "Profile"))
        assertEquals("work 2", V5ProfileRules.distinctName(" work ", others, untitled = "Profile"))
        assertEquals("Home", V5ProfileRules.distinctName("Home", others, untitled = "Profile"))
    }
}
