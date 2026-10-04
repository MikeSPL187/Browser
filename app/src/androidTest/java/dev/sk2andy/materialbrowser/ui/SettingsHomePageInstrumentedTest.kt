package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.BlockerSettings
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsHomePageInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun state(
        appearance: AppearanceSettings = AppearanceSettings(),
        isDefaultBrowser: Boolean = false,
        tabCount: Int = 0,
    ) = SettingsHomeState(
        appearance = appearance,
        searchEngine = SearchEngine.DuckDuckGo,
        blocker = BlockerSettings(),
        httpsOnlyMode = HttpsOnlyMode.Always,
        isHttpsOnlySupported = true,
        downloadSummary = "",
        tabCount = tabCount,
        isDefaultBrowser = isDefaultBrowser,
    )

    @Test
    fun makeDefaultCardShowsUntilVolaIsTheDefault() {
        var isDefault by mutableStateOf(false)
        var requests = 0
        composeRule.setContent {
            MaterialBrowserTheme {
                SettingsHomePage(
                    state = state(isDefaultBrowser = isDefault),
                    isDefaultBrowser = isDefault,
                    onOpenDefaultBrowserSettings = { requests++ },
                    onDestinationChanged = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag(SettingsHomeTestTags.MakeDefault)
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, requests)

        isDefault = true
        composeRule.onNodeWithTag(SettingsHomeTestTags.MakeDefault).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.settings_default_browser_active))
            .assertExists()
    }

    @Test
    fun rowsSummarizeTheCurrentSettings() {
        composeRule.setContent {
            MaterialBrowserTheme {
                SettingsHomePage(
                    state = state(
                        appearance = AppearanceSettings(chromeStyle = BrowserChromeStyle.Air),
                        tabCount = 3,
                    ),
                    isDefaultBrowser = true,
                    onOpenDefaultBrowserSettings = {},
                    onDestinationChanged = {},
                    onDismiss = {},
                )
            }
        }

        val air = context.getString(R.string.settings_chrome_style_air)
        val theme = context.getString(R.string.settings_home_theme_auto)
        composeRule.onNodeWithText("$air · $theme").assertExists()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_home_protection_trackers_https),
        ).assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.settings_home_open_tabs, 3, 3),
        ).assertExists()
    }
}
