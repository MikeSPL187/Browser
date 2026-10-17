package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.settings.SettingSearchCandidate
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5SearchField

/**
 * «Search settings» on a phone under v5 (owner 17.10: the phone all v5): the v5 search field
 * beside the back arrow, the found settings as v5 rows on one card, each saying its page.
 */
@Composable
internal fun SettingsSearchPageV5(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SettingSearchCandidate>,
    focusRequester: FocusRequester,
    onOpen: (SettingSearchCandidate) -> Unit,
    onBack: () -> Unit,
) {
    V5PromptTheme {
        val colors = V5Theme.colors
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                Modifier.padding(end = V5Spacing.marginPhone, top = V5Spacing.space1, bottom = V5Spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        VolaIcons.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = colors.accent,
                    )
                }
                V5SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    hint = stringResource(R.string.settings_search_hint),
                    modifier = Modifier.weight(1f),
                    fieldModifier = Modifier
                        .focusRequester(focusRequester)
                        .testTag(SettingsSearchTestTags.Field),
                )
            }
            when {
                query.isBlank() -> Message(stringResource(R.string.settings_search_prompt))
                results.isEmpty() -> Message(
                    stringResource(R.string.settings_search_empty, query.trim()),
                    Modifier.testTag(SettingsSearchTestTags.Empty),
                )
                else -> Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = V5Spacing.space2, bottom = V5Spacing.space6)
                        .testTag(SettingsSearchTestTags.Results),
                ) {
                    V5ListGroup {
                        results.forEach { result ->
                            // The page first, so a setting found by its description still says where.
                            val supporting = listOfNotNull(result.page.takeIf(String::isNotEmpty), result.summary)
                                .joinToString(" · ")
                            V5ListRow(
                                title = result.title,
                                subtitle = supporting.takeIf(String::isNotEmpty),
                                trailing = V5RowTrailing.Chevron,
                                onClick = { onOpen(result) },
                                modifier = Modifier.testTag(SettingsSearchTestTags.result(result.key)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = V5Theme.type.body,
        color = V5Theme.colors.labelSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space8),
    )
}
