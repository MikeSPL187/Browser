package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.BlockerSettings
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsHomeSummaryRulesTest {
    private val state = SettingsHomeState(
        appearance = AppearanceSettings(),
        searchEngine = SearchEngine.entries.first(),
        blocker = BlockerSettings(),
        httpsOnlyMode = HttpsOnlyMode.Always,
        isHttpsOnlySupported = true,
        downloadSummary = "",
    )

    @Test
    fun `appearance names the shell style and the theme`() {
        assertEquals(
            R.string.settings_chrome_style_air,
            SettingsHomeSummaryRules.chromeStyle(BrowserChromeStyle.Air),
        )
        assertEquals(
            R.string.settings_home_theme_auto,
            SettingsHomeSummaryRules.theme(BrowserAppearanceMode.System),
        )
        assertEquals(
            R.string.settings_home_theme_dark,
            SettingsHomeSummaryRules.theme(BrowserAppearanceMode.Dark),
        )
    }

    @Test
    fun `protection names tracker blocking and HTTPS in every tab`() {
        assertEquals(
            R.string.settings_home_protection_trackers_https,
            SettingsHomeSummaryRules.protection(state),
        )
        assertEquals(
            R.string.settings_home_protection_trackers,
            SettingsHomeSummaryRules.protection(state.copy(httpsOnlyMode = HttpsOnlyMode.PrivateTabs)),
        )
        assertEquals(
            R.string.settings_home_protection_https,
            SettingsHomeSummaryRules.protection(
                state.copy(blocker = BlockerSettings(blockAdsAndTrackers = false)),
            ),
        )
    }

    @Test
    fun `HTTPS only is not claimed where the engine cannot enforce it`() {
        assertEquals(
            R.string.settings_home_protection_trackers,
            SettingsHomeSummaryRules.protection(state.copy(isHttpsOnlySupported = false)),
        )
        assertNull(
            SettingsHomeSummaryRules.protection(
                state.copy(
                    blocker = BlockerSettings(blockAdsAndTrackers = false),
                    isHttpsOnlySupported = false,
                ),
            ),
        )
    }

    @Test
    fun `summary parts join with a dot and skip what is missing`() {
        assertEquals(
            "DuckDuckGo · suggestions",
            SettingsHomeSummaryRules.join(listOf("DuckDuckGo", "suggestions")),
        )
        assertEquals("DuckDuckGo", SettingsHomeSummaryRules.join(listOf("DuckDuckGo", null)))
        assertNull(SettingsHomeSummaryRules.join(listOf(null, "")))
    }

    @Test
    fun `a large font puts the make default button under the text`() {
        assertFalse(SettingsHomeSummaryRules.stacksMakeDefault(1f))
        assertFalse(SettingsHomeSummaryRules.stacksMakeDefault(1.15f))
        assertTrue(SettingsHomeSummaryRules.stacksMakeDefault(1.3f))
        assertTrue(SettingsHomeSummaryRules.stacksMakeDefault(2f))
    }
}
