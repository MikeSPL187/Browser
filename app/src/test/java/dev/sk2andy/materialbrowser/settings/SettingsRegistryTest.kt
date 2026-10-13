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

    @Test
    fun `under v5 search leaves out what only the current interface draws`() {
        fun inV5(key: String) = SettingsRegistry.searchableInV5(
            key,
            SettingsRegistry.settings.first { setting -> setting.spec.key == key }.spec.destination,
        )
        listOf("chrome_style", "address_bar_color", "surface_style", "frosted_blur", "color_palette", "density")
            .forEach { key -> assertTrue(key, !inV5(key)) }
        listOf("appearance_mode", "web_content_font_size", "animations", "private_tabs_lock", "tab_overview_mode")
            .forEach { key -> assertTrue(key, inV5(key)) }
        assertTrue(!SettingsRegistry.searchableInV5("page:Themes", SettingsDestination.Themes))
        assertTrue(SettingsRegistry.searchableInV5("page:Appearance", SettingsDestination.Appearance))
    }

    @Test
    fun `under v5 the less used protection settings open on the More page`() {
        val protection = SettingsDestination.ProtectionAndData
        listOf("dns_over_https", "webrtc_protection", "filter_studio", "recall", "data_archive_export")
            .forEach { key -> assertEquals(key, SettingsDestination.ProtectionMore, SettingsRegistry.v5Destination(key, protection)) }
        listOf("https_only", "block_ads", "history_save", "private_tabs_lock")
            .forEach { key -> assertEquals(key, protection, SettingsRegistry.v5Destination(key, protection)) }
        SettingsRegistry.settings.forEach { setting ->
            val key = setting.spec.key
            val destination = setting.spec.destination
            if (destination != protection && destination != SettingsDestination.Browser) {
                assertEquals(key, destination, SettingsRegistry.v5Destination(key, destination))
            }
        }
    }

    @Test
    fun `under v5 the engine, startup and rarer site settings open on the browser More page`() {
        val browser = SettingsDestination.Browser
        listOf("browser_engine", "startup_animation", "open_home_on_startup", "scroll_bar", "inline_media_player")
            .forEach { key -> assertEquals(key, SettingsDestination.BrowserMore, SettingsRegistry.v5Destination(key, browser)) }
        listOf("video_autoplay", "translation_provider", "external_app_links", "external_link_preview", "page:Browser")
            .forEach { key -> assertEquals(key, browser, SettingsRegistry.v5Destination(key, browser)) }
        SettingsRegistry.settings.filter { setting -> setting.spec.destination == browser }.forEach { setting ->
            val target = SettingsRegistry.v5Destination(setting.spec.key, browser)
            assertTrue(setting.spec.key, target == browser || target == SettingsDestination.BrowserMore)
        }
    }
}
