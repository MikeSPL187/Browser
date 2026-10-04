package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** Settings home by board W-Settings: «Make Vola your default», then cards with live summaries. */
@VolaPreviews
@Composable
private fun SettingsHomePreview() {
    MaterialBrowserTheme {
        SettingsHomePage(
            downloadSummary = "Vola",
            onDestinationChanged = {},
            onDismiss = {},
            onOpenFirefoxExtensions = {},
            live = SettingsHomeLiveState(
                appearance = AppearanceSettings(),
                searchEngine = SearchEngine.DuckDuckGo,
                searchSuggestionsOn = true,
                httpsOnlyMode = HttpsOnlyMode.Always,
                tabCount = 12,
                userscriptCount = 3,
                capsuleCount = 2,
                isDefaultBrowser = false,
            ),
            onMakeDefault = {},
        )
    }
}
