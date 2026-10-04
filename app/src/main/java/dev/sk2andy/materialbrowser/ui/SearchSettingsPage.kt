package dev.sk2andy.materialbrowser.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.browser.SearxngRules
import dev.sk2andy.materialbrowser.browser.SearxngSettings
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.browser.suggestions.SearchSuggestionProvider
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCard
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardChoiceRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardHeader
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSwitchRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardTile
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardValueRow
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsCardTokens
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

internal object SearchSettingsTestTags {
    const val SearxngInstanceUrl = "search_settings_searxng_instance_url"
    const val SearxngFallback = "search_settings_searxng_fallback"
    const val HistorySuggestions = "search_settings_history_suggestions"
    const val SuggestionProvider = "search_settings_suggestion_provider"

    fun engine(engine: SearchEngine): String = "search_settings_engine_${engine.stableId}"
}

/** What the search page shows for each engine (board W-SetSearch). */
internal object SearchSettingsRules {
    /** One line on what sets the engine apart, privacy first. */
    @StringRes
    fun description(engine: SearchEngine): Int = when (engine) {
        SearchEngine.Google -> R.string.search_engine_google_summary
        SearchEngine.DuckDuckGo -> R.string.search_engine_duckduckgo_summary
        SearchEngine.Bing -> R.string.search_engine_bing_summary
        SearchEngine.Brave -> R.string.search_engine_brave_summary
        SearchEngine.Ecosia -> R.string.search_engine_ecosia_summary
        SearchEngine.Startpage -> R.string.search_engine_startpage_summary
        SearchEngine.Qwant -> R.string.search_engine_qwant_summary
        SearchEngine.Kagi -> R.string.search_engine_kagi_summary
        SearchEngine.Perplexity -> R.string.search_engine_perplexity_summary
        SearchEngine.ChatGPT -> R.string.search_engine_chatgpt_summary
        SearchEngine.SearXNG -> R.string.search_engine_searxng_summary
    }

    /** The engine's letter tile takes the accents in turn, so neighbours never share a color. */
    fun accent(index: Int): WorkspaceAccent =
        WorkspaceAccent.entries[index % WorkspaceAccent.entries.size]

    fun letter(engine: SearchEngine): String = engine.displayName.take(1)

    fun showsSearxngServer(
        engine: SearchEngine,
        suggestions: SearchSuggestionProvider,
    ): Boolean = engine == SearchEngine.SearXNG || suggestions == SearchSuggestionProvider.SearXNG
}

/** Search settings (board W-SetSearch): the engines on a card, then suggestions. */
@Composable
internal fun SearchSettingsPage(
    searchEngine: SearchEngine,
    searxngSettings: SearxngSettings,
    isAiModeToggleVisible: Boolean,
    searchSuggestionProvider: SearchSuggestionProvider,
    isHistorySuggestionsEnabled: Boolean,
    onSearchEngineChanged: (SearchEngine) -> Unit,
    onSearxngSettingsChanged: (SearxngSettings) -> Unit,
    onAiModeToggleVisibleChanged: (Boolean) -> Unit,
    onSearchSuggestionProviderChanged: (SearchSuggestionProvider) -> Unit,
    onHistorySuggestionsEnabledChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val cardColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh)
    val dividerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    SettingsPage(
        title = stringResource(R.string.settings_section_search),
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.cardGap)) {
            SettingsCardHeader(stringResource(R.string.settings_search_engine))
            SettingsCard(containerColor = cardColor) {
                SearchEngine.entries.forEachIndexed { index, engine ->
                    SettingsCardChoiceRow(
                        title = engine.displayName,
                        summary = engineSummary(engine, searxngSettings),
                        selected = engine == searchEngine,
                        dividerColor = dividerColor,
                        divider = index != SearchEngine.entries.lastIndex,
                        onClick = { onSearchEngineChanged(engine) },
                        modifier = Modifier.testTag(SearchSettingsTestTags.engine(engine)),
                        leading = {
                            SettingsCardTile(
                                colors = settingsTileColors(SearchSettingsRules.accent(index)),
                            ) { _, tint ->
                                Text(
                                    SearchSettingsRules.letter(engine),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = tint,
                                )
                            }
                        },
                    )
                }
            }
            if (SearchSettingsRules.showsSearxngServer(searchEngine, searchSuggestionProvider)) {
                SearxngServerField(
                    searxngSettings = searxngSettings,
                    onSearxngSettingsChanged = onSearxngSettingsChanged,
                )
            }
            SettingsCardHeader(stringResource(R.string.settings_search_suggestions))
            SettingsCard(containerColor = cardColor) {
                SuggestionProviderRow(
                    title = stringResource(R.string.settings_search_suggestions),
                    selected = searchSuggestionProvider,
                    providers = SearchSuggestionProvider.entries,
                    summary = stringResource(
                        when (searchSuggestionProvider) {
                            SearchSuggestionProvider.None ->
                                R.string.settings_search_suggestions_none_summary
                            SearchSuggestionProvider.SearXNG ->
                                R.string.settings_searxng_search_suggestions_summary
                            else -> R.string.settings_search_suggestions_summary
                        },
                    ),
                    dividerColor = dividerColor,
                    onSelected = onSearchSuggestionProviderChanged,
                    modifier = Modifier.testTag(SearchSettingsTestTags.SuggestionProvider),
                )
                if (searchSuggestionProvider == SearchSuggestionProvider.SearXNG) {
                    SuggestionProviderRow(
                        title = stringResource(R.string.settings_searxng_suggestion_fallback),
                        selected = searxngSettings.suggestionFallback,
                        providers = SearchSuggestionProvider.entries
                            .filterNot { it == SearchSuggestionProvider.SearXNG },
                        summary = stringResource(
                            R.string.settings_searxng_suggestion_fallback_summary,
                        ),
                        dividerColor = dividerColor,
                        onSelected = { provider ->
                            onSearxngSettingsChanged(
                                searxngSettings.copy(suggestionFallback = provider),
                            )
                        },
                        modifier = Modifier.testTag(SearchSettingsTestTags.SearxngFallback),
                    )
                }
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_history_suggestions_title),
                    summary = stringResource(R.string.settings_history_suggestions_summary),
                    checked = isHistorySuggestionsEnabled,
                    dividerColor = dividerColor,
                    divider = searchEngine.supportsAiSearch,
                    onCheckedChange = onHistorySuggestionsEnabledChanged,
                    modifier = Modifier.testTag(SearchSettingsTestTags.HistorySuggestions),
                )
                if (searchEngine.supportsAiSearch) {
                    SettingsCardSwitchRow(
                        title = stringResource(R.string.settings_ai_mode_toggle_title),
                        summary = stringResource(R.string.settings_ai_mode_toggle_subtitle),
                        checked = isAiModeToggleVisible,
                        dividerColor = dividerColor,
                        onCheckedChange = onAiModeToggleVisibleChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun engineSummary(engine: SearchEngine, searxngSettings: SearxngSettings): String {
    val description = stringResource(SearchSettingsRules.description(engine))
    val server = searxngSettings.instanceUrl.takeIf { engine == SearchEngine.SearXNG }
    return if (server.isNullOrBlank()) {
        description
    } else {
        stringResource(R.string.search_engine_searxng_server_summary, description, server)
    }
}

/** A suggestion source picked from a list that opens under its row. */
@Composable
private fun SuggestionProviderRow(
    title: String,
    selected: SearchSuggestionProvider,
    providers: List<SearchSuggestionProvider>,
    summary: String,
    dividerColor: Color,
    onSelected: (SearchSuggestionProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        SettingsCardValueRow(
            title = title,
            value = selected.displayName(),
            summary = summary,
            dividerColor = dividerColor,
            divider = true,
            onClick = { expanded = true },
        )
        SettingsDropdown(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            providers.forEach { provider ->
                SettingsDropdownItem(
                    label = provider.displayName(),
                    selected = provider == selected,
                    onClick = {
                        expanded = false
                        onSelected(provider)
                    },
                )
            }
        }
    }
}

/** The address of the user's own SearXNG server, checked as it is typed. */
@Composable
private fun SearxngServerField(
    searxngSettings: SearxngSettings,
    onSearxngSettingsChanged: (SearxngSettings) -> Unit,
) {
    var instanceUrlDraft by remember { mutableStateOf(searxngSettings.instanceUrl) }
    val invalid = instanceUrlDraft.isNotBlank() &&
        SearxngRules.normalizedInstanceUrl(instanceUrlDraft) == null
    OutlinedTextField(
        value = instanceUrlDraft,
        onValueChange = { value ->
            val boundedValue = value.take(SearxngRules.MAX_INSTANCE_URL_LENGTH)
            val normalizedValue = SearxngRules.normalizedInstanceUrl(boundedValue)
            instanceUrlDraft = boundedValue
            when {
                boundedValue.isBlank() -> onSearxngSettingsChanged(
                    searxngSettings.copy(instanceUrl = ""),
                )
                normalizedValue != null -> onSearxngSettingsChanged(
                    searxngSettings.copy(instanceUrl = normalizedValue),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SearchSettingsTestTags.SearxngInstanceUrl),
        label = { Text(stringResource(R.string.settings_searxng_instance_url)) },
        supportingText = {
            Text(
                stringResource(
                    if (invalid) {
                        R.string.settings_searxng_instance_url_invalid
                    } else {
                        R.string.settings_searxng_instance_url_summary
                    },
                ),
            )
        },
        isError = invalid,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
        singleLine = true,
    )
}
