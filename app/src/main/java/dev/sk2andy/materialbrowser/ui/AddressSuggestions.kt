@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)

package dev.sk2andy.materialbrowser.ui

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ClipboardOffer
import dev.sk2andy.materialbrowser.browser.actions.WebContentTarget
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionGroupRules
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.browser.commands.BrowserCommandKind
import dev.sk2andy.materialbrowser.browser.commands.CommandSuggestion
import dev.sk2andy.materialbrowser.data.AddressSuggestion
import dev.sk2andy.materialbrowser.data.AddressSuggestionSource
import dev.sk2andy.materialbrowser.recall.RecallMatch
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.LocalVolaDarkTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaAddressEditor
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

@Composable
internal fun WebContentContextSheet(
    target: WebContentTarget?,
    onOpenLinkInBackground: () -> Unit,
    onDownloadImage: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (target == null) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                stringResource(R.string.content_actions_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (target.canOpenLinkInBackground) {
                TextButton(
                    onClick = onOpenLinkInBackground,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_open_link_background_tab))
                }
            }
            if (target.canDownloadImage) {
                TextButton(
                    onClick = onDownloadImage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_download_image))
                }
            }
        }
    }
}

/**
 * The suggestions card above the address field (board Editing): groups from the top down as
 * [AddressSuggestionGroupRules] orders them, a hairline between groups, open tabs next to the
 * field. [workspaces] resolves the workspace of a tab that is open elsewhere (proposal П1).
 */
@Composable
internal fun AddressSuggestions(
    suggestions: List<AddressSuggestionItem>,
    query: String,
    highlightedIndex: Int,
    onHighlight: (Int) -> Unit,
    onSelect: (AddressSuggestionItem) -> Unit,
    onFill: (AddressSuggestionItem) -> Unit,
    rootHeightPx: Float,
    bottomBarTopPx: FloatState,
    modifier: Modifier = Modifier,
    workspaces: Map<String, BrowserProfile> = emptyMap(),
    clipboardOffer: ClipboardOffer? = null,
    onClipboardOffer: () -> Unit = {},
) {
    if (suggestions.isEmpty() && clipboardOffer == null) return
    val listState = rememberLazyListState()
    LaunchedEffect(highlightedIndex, suggestions.map(AddressSuggestionItem::stableId)) {
        if (highlightedIndex in suggestions.indices) {
            listState.scrollToItem(highlightedIndex)
        }
    }
    val density = LocalDensity.current
    val currentBottomBarTopPx = bottomBarTopPx.floatValue
    val bottomPadding = AddressEditorLayoutRules.suggestionBottomPaddingDp(
        rootHeightPx = rootHeightPx,
        bottomBarTopPx = currentBottomBarTopPx,
        density = density.density,
    ).dp
    val chipSpace = if (clipboardOffer == null) {
        0.dp
    } else {
        VolaAddressEditor.chipHeight + VolaSpacing.x2
    }
    val maxHeight = AddressEditorLayoutRules.suggestionMaxHeightDp(
        bottomBarTopPx = currentBottomBarTopPx,
        topInsetPx = WindowInsets.statusBars.getTop(density).toFloat(),
        density = density.density,
    ).dp - chipSpace
    Column(
        modifier = modifier
            .padding(horizontal = VolaSpacing.x2)
            .padding(bottom = bottomPadding)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
    ) {
        if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AddressSuggestionTestTags.Card),
                shape = RoundedCornerShape(VolaAddressEditor.cardRadius),
                color = addressEditorSurfaceColor(),
                shadowElevation = VolaElevation.level2,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = maxHeight.coerceAtLeast(0.dp)),
                    contentPadding = PaddingValues(VolaAddressEditor.cardPadding),
                ) {
                    itemsIndexed(
                        items = suggestions,
                        key = { _, suggestion -> suggestion.stableId },
                    ) { index, suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            query = query,
                            startsGroup = AddressSuggestionGroupRules.startsGroup(
                                suggestions,
                                index,
                            ),
                            firstOfRecall = suggestion is AddressSuggestionItem.Recall &&
                                suggestions.getOrNull(index - 1) !is AddressSuggestionItem.Recall,
                            highlighted = index == highlightedIndex,
                            workspaces = workspaces,
                            onHighlight = { onHighlight(index) },
                            onSelect = { onSelect(suggestion) },
                            onFill = { onFill(suggestion) },
                        )
                    }
                }
            }
        }
        if (clipboardOffer != null) {
            AddressClipboardChip(offer = clipboardOffer, onClick = onClipboardOffer)
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: AddressSuggestionItem,
    query: String,
    startsGroup: Boolean,
    firstOfRecall: Boolean,
    highlighted: Boolean,
    workspaces: Map<String, BrowserProfile>,
    onHighlight: () -> Unit,
    onSelect: () -> Unit,
    onFill: () -> Unit,
) {
    Column {
        if (startsGroup) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = VolaSpacing.x3, vertical = VolaSpacing.x1),
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = VolaAddressEditor.DIVIDER_ALPHA,
                ),
            )
        }
        if (firstOfRecall) {
            Text(
                text = stringResource(R.string.recall_from_history),
                modifier = Modifier
                    .padding(horizontal = VolaSpacing.x3, vertical = VolaSpacing.x1)
                    .semantics { heading() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        val select = {
            onHighlight()
            onSelect()
        }
        when (suggestion) {
            is AddressSuggestionItem.Navigation -> NavigationSuggestionRow(
                suggestion = suggestion.suggestion,
                workspace = suggestion.suggestion.openTabProfileId?.let(workspaces::get),
                highlighted = highlighted,
                onClick = select,
                onFill = onFill,
            )
            is AddressSuggestionItem.Command -> CommandSuggestionRow(
                suggestion = suggestion.suggestion,
                highlighted = highlighted,
                onClick = select,
            )
            is AddressSuggestionItem.Search -> SearchSuggestionRow(
                query = suggestion.query,
                typed = query,
                highlighted = highlighted,
                onClick = select,
                onFill = onFill,
            )
            is AddressSuggestionItem.Recall -> RecallSuggestionRow(
                match = suggestion.match,
                highlighted = highlighted,
                onClick = select,
                onFill = onFill,
            )
        }
    }
}

/** The card and chip color: the lightest surface, lifted off pure black in the dark theme. */
@Composable
@ReadOnlyComposable
internal fun addressEditorSurfaceColor(): Color = if (LocalVolaDarkTheme.current) {
    MaterialTheme.colorScheme.surfaceContainerHigh
} else {
    MaterialTheme.colorScheme.surfaceContainerLowest
}

@Composable
@ReadOnlyComposable
private fun rowContainerColor(highlighted: Boolean, tinted: Boolean = false): Color = when {
    highlighted -> MaterialTheme.colorScheme.secondaryContainer
    tinted -> MaterialTheme.colorScheme.primaryContainer.copy(
        alpha = VolaAddressEditor.OPEN_TAB_TINT_ALPHA,
    )
    else -> Color.Transparent
}

/**
 * A row with a site tile and two lines. A tap opens it; a long press, or the TalkBack action,
 * puts the address into the field instead.
 */
@Composable
private fun SiteRow(
    tileText: String,
    title: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    onFill: (() -> Unit)?,
    modifier: Modifier = Modifier,
    tinted: Boolean = false,
    tileIcon: ImageVector? = null,
    subtitle: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val fillLabel = stringResource(R.string.address_suggestion_fill)
    val shape = RoundedCornerShape(VolaAddressEditor.siteRowRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VolaAddressEditor.siteRowMinHeight)
            .clip(shape)
            .background(rowContainerColor(highlighted, tinted), shape)
            .semantics { selected = highlighted }
            .combinedClickable(
                role = Role.Button,
                onLongClick = onFill,
                onLongClickLabel = fillLabel.takeIf { onFill != null },
                onClick = onClick,
            )
            .padding(
                start = VolaSpacing.x3,
                end = VolaSpacing.x2,
                top = VolaSpacing.x1,
                bottom = VolaSpacing.x1,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
    ) {
        Box(
            modifier = Modifier
                .size(VolaAddressEditor.siteTileSize)
                .clip(RoundedCornerShape(VolaAddressEditor.siteTileRadius))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (tileIcon != null) {
                Icon(
                    tileIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Text(
                    text = tileText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                LocalTextStyle provides MaterialTheme.typography.bodySmall,
            ) {
                subtitle()
            }
        }
        trailing()
    }
}

@Composable
private fun NavigationSuggestionRow(
    suggestion: AddressSuggestion,
    workspace: BrowserProfile?,
    highlighted: Boolean,
    onClick: () -> Unit,
    onFill: () -> Unit,
) {
    val host = AddressResolver.displayText(suggestion.url)
    val openTab = suggestion.openTabId != null
    SiteRow(
        tileText = AddressSuggestionRowRules.monogram(suggestion.title, host),
        title = suggestion.title,
        highlighted = highlighted,
        tinted = openTab,
        onClick = onClick,
        onFill = onFill,
        modifier = Modifier.testTag(AddressSuggestionTestTags.navigationRow(suggestion.url)),
        subtitle = {
            when {
                workspace != null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x1),
                ) {
                    WorkspaceGem(workspace = workspace, size = VolaAddressEditor.rowGemSize)
                    SubtitleText(
                        stringResource(
                            R.string.address_suggestion_open_in_workspace,
                            workspace.workspaceDisplayName(),
                        ),
                    )
                }
                openTab -> SubtitleText(stringResource(R.string.address_suggestion_open_here))
                suggestion.source == AddressSuggestionSource.Favorite -> SubtitleText(
                    stringResource(R.string.address_suggestion_favorite, host),
                )
                else -> SubtitleText(
                    suggestion.lastVisitedAt
                        ?.takeIf { it > 0L }
                        ?.let { visitedAt ->
                            stringResource(
                                R.string.address_suggestion_detail,
                                host,
                                relativeDay(visitedAt),
                            )
                        }
                        ?: host,
                )
            }
        },
        trailing = {
            when {
                openTab -> Box(
                    modifier = Modifier
                        .height(VolaAddressEditor.switchPillHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = VolaSpacing.x3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.address_suggestion_switch),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                    )
                }
                else -> Icon(
                    imageVector = if (suggestion.source == AddressSuggestionSource.Favorite) {
                        VolaIcons.Star
                    } else {
                        VolaIcons.History
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(end = VolaSpacing.x2),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun SubtitleText(text: String) {
    Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** "Today", "Yesterday" or a date, in the system language. */
@Composable
private fun relativeDay(timeMillis: Long): String {
    val now = remember { System.currentTimeMillis() }
    return remember(timeMillis, now) {
        DateUtils.getRelativeTimeSpanString(
            timeMillis,
            now,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
    }
}

@Composable
private fun RecallSuggestionRow(
    match: RecallMatch,
    highlighted: Boolean,
    onClick: () -> Unit,
    onFill: () -> Unit,
) {
    SiteRow(
        tileText = "",
        tileIcon = VolaIcons.History,
        title = match.title,
        highlighted = highlighted,
        onClick = onClick,
        onFill = onFill,
        modifier = Modifier.testTag(AddressSuggestionTestTags.recallRow(match.url)),
        subtitle = {
            Text(text = match.excerpt, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailing = {},
    )
}

/** A search suggestion: what the user typed in regular weight, the completion in bold. */
@Composable
internal fun SearchSuggestionRow(
    query: String,
    typed: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    onFill: () -> Unit,
) {
    val shape = RoundedCornerShape(VolaAddressEditor.searchRowRadius)
    val completionStart = AddressSuggestionRowRules.completionStart(query, typed)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VolaAddressEditor.searchRowHeight)
            .clip(shape)
            .background(rowContainerColor(highlighted), shape)
            .testTag(AddressSuggestionTestTags.searchRow(query))
            .semantics { selected = highlighted }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = VolaSpacing.x5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x4),
    ) {
        Icon(
            VolaIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = buildAnnotatedString {
                append(query.substring(0, completionStart))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(query.substring(completionStart))
                }
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = onFill,
            modifier = Modifier.testTag(AddressSuggestionTestTags.fillSearch(query)),
        ) {
            Icon(
                VolaIcons.NorthWest,
                contentDescription = stringResource(R.string.cd_fill_address_suggestion, query),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CommandSuggestionRow(
    suggestion: CommandSuggestion,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(VolaAddressEditor.siteRowRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VolaAddressEditor.siteRowMinHeight)
            .clip(shape)
            .background(rowContainerColor(highlighted), shape)
            .semantics(mergeDescendants = true) { selected = highlighted }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = VolaSpacing.x3, vertical = VolaSpacing.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
    ) {
        Box(
            modifier = Modifier
                .size(VolaAddressEditor.siteTileSize)
                .clip(RoundedCornerShape(VolaAddressEditor.siteTileRadius))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            CommandIcon(suggestion.command.kind, MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                suggestion.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                suggestion.effect,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        suggestion.command.targetProfileLabel?.let { profile ->
            Text(
                text = stringResource(R.string.command_target_profile, profile),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = VolaSpacing.x2, vertical = VolaSpacing.x1),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
        }
    }
}

/**
 * The clipboard chip under the suggestions. It knows only what kind of clip there is; the clip
 * itself is read when the chip is tapped (see [ClipboardOffer]).
 */
@Composable
internal fun AddressClipboardChip(
    offer: ClipboardOffer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        if (offer == ClipboardOffer.Link) {
            R.string.address_clipboard_link
        } else {
            R.string.address_clipboard_text
        },
    )
    val action = stringResource(
        if (offer == ClipboardOffer.Link) {
            R.string.address_clipboard_open
        } else {
            R.string.address_clipboard_paste
        },
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = action, onClick = onClick)
            .testTag(AddressSuggestionTestTags.ClipboardChip),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(VolaAddressEditor.chipHeight),
            shape = CircleShape,
            color = addressEditorSurfaceColor(),
            shadowElevation = VolaElevation.level1,
        ) {
            Row(
                modifier = Modifier.padding(start = VolaSpacing.x3, end = VolaSpacing.x1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
            ) {
                Icon(
                    VolaIcons.ContentPaste,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .height(VolaAddressEditor.chipActionHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = VolaSpacing.x3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = action,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * The line above the suggestions: the workspace gem and where the typed address opens,
 * "New tab in «Work»" on a new tab.
 */
@Composable
internal fun AddressEditorHeader(
    workspace: BrowserProfile,
    newTab: Boolean,
    privateTab: Boolean,
    modifier: Modifier = Modifier,
) {
    val name = workspace.workspaceDisplayName()
    // On a pill of its own: over a page the scrim is light and the text must stay legible.
    Row(
        modifier = modifier
            .padding(horizontal = VolaSpacing.x3, vertical = VolaSpacing.x1)
            .shadow(VolaElevation.level1, CircleShape)
            .clip(CircleShape)
            .background(addressEditorSurfaceColor())
            .padding(start = VolaSpacing.x1, end = VolaSpacing.x4)
            .heightIn(min = VolaSpacing.x12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
    ) {
        WorkspaceGem(
            workspace = workspace,
            size = VolaAddressEditor.headerGemSize,
            privateMode = privateTab,
        )
        Text(
            text = when {
                privateTab -> stringResource(R.string.address_editor_private_tab)
                newTab -> stringResource(R.string.address_editor_new_tab_in_workspace, name)
                else -> stringResource(R.string.address_editor_tab_in_workspace, name)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal object AddressSuggestionRowRules {
    /** The letter on a site tile until it has an icon: the title's, else the host's. */
    fun monogram(title: String, host: String): String {
        val source = title.firstOrNull(Char::isLetterOrDigit)
            ?: host.removePrefix("www.").firstOrNull(Char::isLetterOrDigit)
        return source?.uppercaseChar()?.toString().orEmpty()
    }

    /** Where the bold completion starts in [suggestion]: after the typed text it begins with. */
    fun completionStart(suggestion: String, typed: String): Int {
        val prefix = typed.trimStart()
        return if (prefix.isNotEmpty() && suggestion.startsWith(prefix, ignoreCase = true)) {
            prefix.length
        } else {
            suggestion.length
        }
    }
}

@Composable
private fun CommandIcon(kind: BrowserCommandKind, tint: Color) {
    val modifier = Modifier.size(22.dp)
    when (kind) {
        BrowserCommandKind.ClearCacheAndReload,
        BrowserCommandKind.Reload,
        -> Icon(VolaIcons.Refresh, contentDescription = null, modifier = modifier, tint = tint)
        BrowserCommandKind.ClearCookiesAndReload -> Icon(
            painterResource(R.drawable.ic_delete_outline),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        BrowserCommandKind.StopLoading ->
            Icon(VolaIcons.Close, contentDescription = null, modifier = modifier, tint = tint)
        BrowserCommandKind.PinTab,
        BrowserCommandKind.UnpinTab,
        -> Icon(
            painterResource(R.drawable.ic_push_pin),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        BrowserCommandKind.CloseDuplicateTabs -> Icon(
            painterResource(R.drawable.ic_content_copy),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        BrowserCommandKind.MoveTabToProfile,
        BrowserCommandKind.SwitchProfile,
        -> Icon(
            painterResource(R.drawable.ic_switch_to_tab),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        BrowserCommandKind.NewRegularTab ->
            Icon(VolaIcons.Add, contentDescription = null, modifier = modifier, tint = tint)
        BrowserCommandKind.NewIncognitoTab -> Icon(
            painterResource(R.drawable.ic_incognito_outline),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        BrowserCommandKind.OpenSettings -> Icon(
            painterResource(R.drawable.ic_settings),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
    }
}
