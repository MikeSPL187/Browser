package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.browser.SearxngSettings
import dev.sk2andy.materialbrowser.browser.suggestions.SearchSuggestionProvider
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCard
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardHeader
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardLinkRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSwitchRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardValueRow
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.ui.AppearanceSettingsPageV5
import dev.sk2andy.materialbrowser.ui.SearchSettingsPage
import dev.sk2andy.materialbrowser.ui.SettingsPage
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

/* V15d: the current settings pages drawn by the v5 skin. Preview-only sample text is not localized. */

/** A real page: the search engines as checked rows, then the suggestion switches. */
@VolaPreviews
@Composable
private fun V5SearchSettingsPreview() {
    MaterialBrowserTheme {
        ProvideV5SettingsSkin() {
            SearchSettingsPage(
                searchEngine = SearchEngine.DuckDuckGo,
                searxngSettings = SearxngSettings(instanceUrl = "https://search.example.org"),
                isAiModeToggleVisible = true,
                searchSuggestionProvider = SearchSuggestionProvider.DuckDuckGo,
                isHistorySuggestionsEnabled = true,
                onSearchEngineChanged = {},
                onSearxngSettingsChanged = {},
                onAiModeToggleVisibleChanged = {},
                onSearchSuggestionProviderChanged = {},
                onHistorySuggestionsEnabledChanged = {},
                onBack = {},
            )
        }
    }
}

/** Every row end the skin draws: chevron, switch (on, off, unavailable) and a value. */
@VolaPreviews
@Composable
private fun V5SettingsRowsPreview() {
    MaterialBrowserTheme {
        ProvideV5SettingsSkin() {
            SettingsPage(title = "Загрузки", onBack = {}) {
                val separator = V5Theme.colors.separator
                SettingsCardHeader("Куда сохранять")
                SettingsCard(containerColor = V5Theme.colors.surface) {
                    SettingsCardValueRow(
                        title = "Менеджер загрузок",
                        value = "Встроенный",
                        summary = "Файлы сохраняет сам браузер",
                        dividerColor = separator,
                        onClick = {},
                        divider = true,
                    )
                    SettingsCardSwitchRow(
                        title = "Спрашивать, куда сохранить",
                        summary = null,
                        checked = true,
                        dividerColor = separator,
                        onCheckedChange = {},
                        divider = true,
                    )
                    SettingsCardSwitchRow(
                        title = "Открывать после загрузки",
                        summary = "Недоступно для этого менеджера",
                        checked = false,
                        dividerColor = separator,
                        onCheckedChange = {},
                        enabled = false,
                        divider = true,
                    )
                    SettingsCardLinkRow(
                        title = "Папка загрузок",
                        summary = "Download/Vola",
                        dividerColor = separator,
                        onClick = {},
                    )
                }
            }
        }
    }
}

/** V15d-2: «Appearance» under ui.v5, with dark sites as System WebView offers them. */
@VolaPreviews
@Composable
private fun V5AppearanceSettingsPreview() {
    MaterialBrowserTheme {
        ProvideV5SettingsSkin() {
            AppearanceSettingsPageV5(
                settings = AppearanceSettings(),
                onSettingsChanged = {},
                onBack = {},
                forceDarkWebsitesAvailable = true,
            )
        }
    }
}
