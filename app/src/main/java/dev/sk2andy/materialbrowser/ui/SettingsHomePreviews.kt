package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.blocking.BlockerSettings
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

@Composable
private fun SettingsHomePreview(isDefaultBrowser: Boolean) {
    MaterialBrowserTheme {
        SettingsHomePage(
            state = SettingsHomeState(
                appearance = AppearanceSettings(),
                searchEngine = SearchEngine.entries.first(),
                blocker = BlockerSettings(),
                httpsOnlyMode = HttpsOnlyMode.Always,
                isHttpsOnlySupported = true,
                downloadSummary = "Vola",
            ),
            isDefaultBrowser = isDefaultBrowser,
            onOpenDefaultBrowserSettings = {},
            onDestinationChanged = {},
            onDismiss = {},
            onOpenFirefoxExtensions = {},
        )
    }
}

/** The settings home (board W-Settings): «Make Vola your default», then the pages on cards. */
@VolaPreviews
@Composable
private fun SettingsHomeNotDefaultPreview() {
    SettingsHomePreview(isDefaultBrowser = false)
}

/** Once Vola is the default browser, the cards start at the top. */
@VolaPreviews
@Composable
private fun SettingsHomeDefaultPreview() {
    SettingsHomePreview(isDefaultBrowser = true)
}
