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
    fun `appearance keeps at most seven main settings and folds the rest`() {
        val main = SettingsRegistry.settingsOn(SettingsDestination.Appearance, SettingLevel.Main)
        val advanced =
            SettingsRegistry.settingsOn(SettingsDestination.Appearance, SettingLevel.Advanced)
        assertTrue(main.size in 1..7)
        assertTrue(advanced.isNotEmpty())
        assertTrue(main.none { setting -> setting in advanced })
    }
}
