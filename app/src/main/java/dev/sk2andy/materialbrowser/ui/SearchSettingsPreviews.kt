package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.browser.SearxngSettings
import dev.sk2andy.materialbrowser.browser.suggestions.SearchSuggestionProvider
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

@Composable
private fun SearchSettingsPreview(engine: SearchEngine, suggestions: SearchSuggestionProvider) {
    MaterialBrowserTheme {
        SearchSettingsPage(
            searchEngine = engine,
            searxngSettings = SearxngSettings(instanceUrl = "https://search.example.org"),
            isAiModeToggleVisible = true,
            searchSuggestionProvider = suggestions,
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

/** Search settings (board W-SetSearch): the engines with what sets each apart, then suggestions. */
@VolaPreviews
@Composable
private fun SearchSettingsDuckDuckGoPreview() {
    SearchSettingsPreview(SearchEngine.DuckDuckGo, SearchSuggestionProvider.DuckDuckGo)
}

/** With SearXNG the engine's line names the server, and the server's address is asked for. */
@VolaPreviews
@Composable
private fun SearchSettingsSearxngPreview() {
    SearchSettingsPreview(SearchEngine.SearXNG, SearchSuggestionProvider.SearXNG)
}
