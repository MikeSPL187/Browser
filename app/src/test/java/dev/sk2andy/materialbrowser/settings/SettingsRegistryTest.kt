package dev.sk2andy.materialbrowser.settings

import dev.sk2andy.materialbrowser.shared.settings.SettingLevel
import dev.sk2andy.materialbrowser.ui.SettingsDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRegistryTest {
    @Test
    fun `keys are unique and every setting sits on a page of the registry`() {
        val keys = SettingsRegistry.settings.map { setting -> setting.spec.key }
        assertEquals(keys.size, keys.toSet().size)
        SettingsRegistry.settings.forEach { setting ->
            assertTrue(setting.spec.key, SettingsRegistry.page(setting.spec.destination) != null)
        }
    }

    @Test
    fun `settings shown on their pages are searchable`() {
        val destinations = SettingsRegistry.settings.associate { setting ->
            setting.spec.key to setting.spec.destination
        }
        assertEquals(SettingsDestination.ProtectionAndData, destinations["private_tabs_lock"])
        assertEquals(SettingsDestination.ProtectionAndData, destinations["filter_studio"])
        assertEquals(SettingsDestination.ProtectionAndData, destinations["permission_radar"])
        assertEquals(SettingsDestination.Search, destinations["searxng_instance_url"])
        assertEquals(SettingsDestination.TabsAndGestures, destinations["tab_overview_mode"])
        assertEquals(SettingsDestination.TabsAndGestures, destinations["tab_list_starts_at_bottom"])
        assertEquals(SettingsDestination.TabsAndGestures, destinations["resident_tab_limit"])
    }

    @Test
    fun `appearance keeps at most seven main settings and folds the rest`() {
        val main = SettingsRegistry.settingsOn(SettingsDestination.Appearance, SettingLevel.Main)
        val advanced =
            SettingsRegistry.settingsOn(SettingsDestination.Appearance, SettingLevel.Advanced)
        assertTrue(main.size in 1..7)
        assertTrue(advanced.isNotEmpty())
        assertTrue(main.none { setting -> setting in advanced })
    }

    @Test
    fun `themes holds the palette, the accent, the corners and the density`() {
        val keys = SettingsRegistry.settingsOn(SettingsDestination.Themes, SettingLevel.Main)
            .map { setting -> setting.spec.key }
        assertEquals(listOf("color_palette", "accent_override", "shape_style", "density"), keys)
        assertTrue(SettingsRegistry.page(SettingsDestination.Themes) != null)
    }
}
