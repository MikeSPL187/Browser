package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeRules
import dev.sk2andy.materialbrowser.ui.SettingsDestination
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import org.junit.Assert.assertEquals
import org.junit.Test

class V5SettingsHomeRulesTest {
    @Test
    fun `pages fall into the canvas groups in order`() {
        val items = SettingsHomeRules.items(hasFirefoxExtensions = true, hasDeveloperOptions = true)
        val groups = V5SettingsHomeRules.groups(items).map { group -> group.map { it.destination } }
        assertEquals(
            listOf(
                listOf(SettingsDestination.Sync),
                listOf(
                    SettingsDestination.Search,
                    SettingsDestination.TabsAndGestures,
                    SettingsDestination.Appearance,
                    SettingsDestination.Browser,
                ),
                listOf(SettingsDestination.ProtectionAndData, SettingsDestination.SiteCapsules, SettingsDestination.Passwords),
                listOf(SettingsDestination.Userscripts, null, SettingsDestination.Downloads),
                listOf(SettingsDestination.AboutLegal, SettingsDestination.DeveloperOptions),
            ),
            groups,
        )
    }

    @Test
    fun `every page the current home lists is in a v5 group, and only those`() {
        val items = SettingsHomeRules.items(hasFirefoxExtensions = false, hasDeveloperOptions = false)
        val shown = V5SettingsHomeRules.groups(items).flatten()
        assertEquals(items.toSet(), shown.toSet())
        assertEquals(items.size, shown.size)
    }

    @Test
    fun `tiles follow the section`() {
        val items = SettingsHomeRules.items(hasFirefoxExtensions = false).associateBy { it.destination }
        assertEquals(V5Tile.Sky, V5SettingsHomeRules.tile(items.getValue(SettingsDestination.Sync)))
        assertEquals(V5Tile.Red, V5SettingsHomeRules.tile(items.getValue(SettingsDestination.Passwords)))
        assertEquals(V5Tile.Pink, V5SettingsHomeRules.tile(items.getValue(SettingsDestination.Appearance)))
        assertEquals(V5Tile.Gray, V5SettingsHomeRules.tile(items.getValue(SettingsDestination.AboutLegal)))
    }
}
