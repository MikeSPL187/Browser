package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.browser.BrowserTab
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Stable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaTabOverview

/**
 * The top of the tab overview (board W-Tabs): the workspace name with its tab count, then the
 * actions. The workspaces themselves sit at the bottom, in the dock.
 */
@Composable
internal fun TabOverviewHeader(
    title: String,
    tabCount: Int,
    enabled: Boolean,
    pinnedJumpVisible: Boolean,
    onPinnedJump: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    search: TabOverviewSearch? = null,
) {
    val colors = MaterialTheme.colorScheme
    if (search?.query != null) {
        TabSearchField(search = search, modifier = modifier)
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TabOverviewChromeTestTags.Header)
            .heightIn(min = VolaTabOverview.headerHeight)
            .padding(
                start = VolaTabOverview.headerStartPadding,
                end = VolaTabOverview.headerEndPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(VolaTabOverview.headerCountGap),
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .alignByBaseline()
                    .weight(1f, fill = false)
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.tab_overview_count, tabCount, tabCount),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (search != null) {
            IconButton(
                onClick = search.onOpen,
                enabled = enabled,
                modifier = Modifier.testTag(TabOverviewHeaderTestTags.Search),
            ) {
                Icon(
                    VolaIcons.Search,
                    contentDescription = stringResource(R.string.tab_search_hint),
                    tint = colors.onSurface,
                )
            }
        }
        AnimatedVisibility(
            visible = pinnedJumpVisible,
            enter = fadeIn(VolaMotion.effects()),
            exit = fadeOut(VolaMotion.effects()),
        ) {
            IconButton(
                onClick = onPinnedJump,
                enabled = enabled,
                modifier = Modifier.testTag(TabOverviewChromeTestTags.PinnedTabsJump),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_push_pin),
                    contentDescription = stringResource(R.string.cd_scroll_to_pinned_tabs),
                    tint = colors.onSurface,
                )
            }
        }
        IconButton(
            onClick = onSettings,
            enabled = enabled,
            modifier = Modifier.testTag(TabOverviewChromeTestTags.Settings),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.action_settings),
                tint = colors.onSurface,
            )
        }
        IconButton(
            onClick = onMore,
            enabled = enabled,
            modifier = Modifier.testTag(TabOverviewChromeTestTags.More),
        ) {
            Icon(
                VolaIcons.MoreVert,
                contentDescription = stringResource(R.string.cd_tab_actions),
                tint = colors.onSurface,
            )
        }
    }
}

internal object TabOverviewHeaderTestTags {
    const val Search = "tab_overview_search"
    const val SearchField = "tab_overview_search_field"
    const val SearchClose = "tab_overview_search_close"
}

/** Search in the header: [query] is null while closed, the text once the field is open. */
internal class TabOverviewSearch(
    val query: String?,
    val onOpen: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onClose: () -> Unit,
)

/** The field that takes the header's place while searching; it opens with the keyboard. */
@Composable
private fun TabSearchField(search: TabOverviewSearch, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TabOverviewChromeTestTags.Header)
            .heightIn(min = VolaTabOverview.headerHeight)
            .padding(
                start = VolaTabOverview.searchFieldStartPadding,
                end = VolaTabOverview.headerEndPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(VolaTabOverview.searchFieldHeight)
                .clip(CircleShape)
                .background(VolaTheme.extendedColors.card)
                .padding(horizontal = VolaTabOverview.searchFieldPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(VolaIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant)
            val hint = stringResource(R.string.tab_search_hint)
            BasicTextField(
                value = search.query.orEmpty(),
                onValueChange = search.onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = VolaTabOverview.searchFieldPadding)
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = hint }
                    .testTag(TabOverviewHeaderTestTags.SearchField),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                singleLine = true,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (search.query.isNullOrEmpty()) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        field()
                    }
                },
            )
        }
        IconButton(
            onClick = search.onClose,
            modifier = Modifier.testTag(TabOverviewHeaderTestTags.SearchClose),
        ) {
            Icon(
                VolaIcons.Close,
                contentDescription = stringResource(R.string.tab_search_close),
                tint = colors.onSurface,
            )
        }
    }
}

/** Tab search of the overview: closed (null) each time the overview opens; Back closes it first. */
@Stable
internal class TabOverviewSearchState {
    var query by mutableStateOf<String?>(null)

    /** The header's search, or null where search does not apply (the carousel pages every tab). */
    fun header(available: Boolean): TabOverviewSearch? = if (available) {
        TabOverviewSearch(
            query = query,
            onOpen = { query = "" },
            onQueryChange = { text -> query = text },
            onClose = { query = null },
        )
    } else {
        null
    }

    /** [hidden] tabs (locked private ones) never match a query: their text is not searchable. */
    fun filter(tabs: List<BrowserTab>, hidden: (BrowserTab) -> Boolean = { false }): List<BrowserTab> =
        TabSearchRules.filter(
            items = tabs,
            query = query,
            title = { tab -> if (hidden(tab)) "" else tab.title },
            url = { tab -> if (hidden(tab)) "" else tab.url },
        )

    fun hasNoResults(results: List<BrowserTab>): Boolean =
        results.isEmpty() && !query.isNullOrBlank()
}

@Composable
internal fun rememberTabOverviewSearchState(visible: Boolean): TabOverviewSearchState {
    val state = remember(visible) { TabOverviewSearchState() }
    BackHandler(enabled = visible && state.query != null) { state.query = null }
    return state
}

/** In place of the grid when no tab matches the search. */
@Composable
internal fun TabSearchEmptyState(modifier: Modifier = Modifier) {
    VolaStateMessage(
        icon = rememberVectorPainter(VolaIcons.Search),
        title = stringResource(R.string.tab_search_empty_title),
        message = stringResource(R.string.tab_search_empty_message),
        modifier = modifier.padding(horizontal = VolaSpacing.x4),
    )
}
