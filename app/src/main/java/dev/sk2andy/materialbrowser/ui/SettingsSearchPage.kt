package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.settings.SettingsRegistry
import dev.sk2andy.materialbrowser.shared.settings.SettingSearchCandidate
import dev.sk2andy.materialbrowser.shared.settings.SettingsSearchRules
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaSettingsSearch

internal object SettingsSearchTestTags {
    const val Open = "settings_search_open"
    const val Field = "settings_search_field"
    const val Results = "settings_search_results"
    const val Empty = "settings_search_empty"
    fun result(key: String) = "settings_search_result:$key"
}

/**
 * Finds a setting by its name or description and opens its page. The words come from
 * [SettingsRegistry], in the interface language.
 */
@Composable
internal fun SettingsSearchPage(
    onOpen: (SettingSearchCandidate) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val candidates = remember(configuration) {
        SettingsRegistry.searchCandidates(context.resources)
    }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, candidates) { SettingsSearchRules.search(query, candidates) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = VolaSettingsSearch.sidePadding),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VolaSettingsSearch.headerGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag(SettingsSearchTestTags.Field),
                placeholder = { Text(stringResource(R.string.settings_search_hint)) },
                leadingIcon = { Icon(VolaIcons.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                VolaIcons.Close,
                                contentDescription = stringResource(R.string.settings_search_clear),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = VolaSettingsSearch.fieldShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        when {
            query.isBlank() ->
                SettingsSearchMessage(stringResource(R.string.settings_search_prompt))
            results.isEmpty() -> SettingsSearchMessage(
                stringResource(R.string.settings_search_empty, query.trim()),
                modifier = Modifier.testTag(SettingsSearchTestTags.Empty),
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VolaSettingsSearch.resultsTopPadding)
                    .testTag(SettingsSearchTestTags.Results),
            ) {
                items(results, key = SettingSearchCandidate::key) { result ->
                    ListItem(
                        headlineContent = { Text(result.title) },
                        supportingContent = listOfNotNull(
                            result.page.takeIf(String::isNotEmpty),
                            result.summary,
                        ).joinToString(" · ").takeIf(String::isNotEmpty)?.let { supporting ->
                            {
                                Text(
                                    supporting,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        modifier = Modifier
                            .clickable { onOpen(result) }
                            .testTag(SettingsSearchTestTags.result(result.key)),
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSearchMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .fillMaxWidth()
            .padding(VolaSettingsSearch.messagePadding),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}
