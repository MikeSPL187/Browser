package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.settings.SettingsRegistry
import dev.sk2andy.materialbrowser.shared.settings.SettingSearchCandidate
import dev.sk2andy.materialbrowser.shared.settings.SettingsSearchRules
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
        SettingsRegistry.searchCandidates(context.resources, true)
    }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, candidates) { SettingsSearchRules.search(query, candidates) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    SettingsSearchPageV5(query, { query = it }, results, focusRequester, onOpen, onBack)
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
