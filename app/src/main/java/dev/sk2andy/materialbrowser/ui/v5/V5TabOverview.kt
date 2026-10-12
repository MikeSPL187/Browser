package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Materials
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

/*
 * The v5 tab overview (system-v5.md, section 10; canvas screen «d»): search at the top, the
 * «Tabs / Private» segment, a two-column grid of cards with the current one ringed in the accent,
 * and at the bottom edge [+], the profile chip and «Done», where the thumb is (Safari moved «+» back
 * down after complaints). A card swipes sideways to close (V10b); groups come with V12.
 */

object V5OverviewTestTags {
    const val Overview = "v5_tab_overview"
    const val Search = "v5_tab_overview_search"
    const val SegmentTabs = "v5_tab_overview_segment_tabs"
    const val SegmentPrivate = "v5_tab_overview_segment_private"
    const val NewTab = "v5_tab_overview_new_tab"
    const val Done = "v5_tab_overview_done"
    fun card(id: String) = "v5_tab_card:$id"
    fun close(id: String) = "v5_tab_card_close:$id"
}

/** One card; [preview] is null for private tabs and tabs without a snapshot yet. */
@Immutable
data class V5TabCard(
    val id: String,
    val title: String,
    val selected: Boolean,
    val preview: ImageBitmap?,
    val favicon: ImageBitmap?,
    /** False for pinned tabs: they have no close button and spring back when swiped. */
    val closable: Boolean = true,
)

@Immutable
data class V5OverviewState(
    val cards: List<V5TabCard>,
    val privateSegment: Boolean,
    val counts: V5OverviewCounts,
    val query: String,
    /** The profile chip: its emoji and name. */
    val profileLabel: String,
    /** The profile's tab groups (V12); empty keeps one flat grid. */
    val groups: List<V5OverviewGroup> = emptyList(),
)

@Composable
fun V5TabOverview(
    state: V5OverviewState,
    onQueryChange: (String) -> Unit,
    onSegmentChange: (privateSegment: Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: () -> Unit,
    onDone: () -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where each card sits in the root, for the page that shrinks into its card (V10b). */
    onCardBounds: (String, Rect) -> Unit = { _, _ -> },
    /** A card drawn by the hero layer instead, while the page shrinks into it or grows out. */
    heroCardId: String? = null,
    /** A group's header or collapsed row was tapped: it folds or unfolds. */
    onToggleGroup: (String) -> Unit = {},
) {
    val colors = V5Theme.colors
    val items = remember(state.cards, state.groups, state.query) {
        V5OverviewGroupRules.items(state.cards.map { it.id }, state.groups, searching = state.query.isNotBlank())
    }
    val cardsById = remember(state.cards) { state.cards.associateBy { it.id } }
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(V5OverviewTestTags.Overview),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(state.query, onQueryChange, onCloseAll)
            Segments(state.privateSegment, state.counts, onSegmentChange)
            if (state.cards.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.tab_search_empty_title),
                        style = V5Theme.type.callout,
                        color = colors.labelSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(V5Sizes.TAB_COLUMNS_PHONE),
                    // The grid opens on the selected card, so the page's card is on screen for the hero.
                    state = rememberLazyGridState(
                        initialFirstVisibleItemIndex = V5OverviewGroupRules.indexOf(
                            items,
                            state.cards.firstOrNull { it.selected }?.id,
                        ),
                    ),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = V5Spacing.marginPhone,
                        end = V5Spacing.marginPhone,
                        top = V5Spacing.space3,
                        bottom = V5Spacing.space3,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(V5Sizes.tabGridGap),
                    verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
                ) {
                    items(
                        items,
                        key = { it.key },
                        span = { item -> GridItemSpan(if (item is V5OverviewItem.Card) 1 else maxLineSpan) },
                    ) { item ->
                        when (item) {
                            is V5OverviewItem.Card -> cardsById[item.id]?.let { card ->
                                TabCard(
                                    card = card,
                                    onSelect = onSelect,
                                    onClose = onClose,
                                    onBounds = onCardBounds,
                                    hidden = card.id == heroCardId,
                                    groupTile = item.group?.tile,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                            is V5OverviewItem.GroupHeader -> GroupHeader(
                                group = item.group,
                                count = item.count,
                                onToggle = { onToggleGroup(item.group.id) },
                                modifier = Modifier.animateItem(),
                            )
                            is V5OverviewItem.CollapsedGroup -> CollapsedGroup(
                                group = item.group,
                                count = item.count,
                                previews = item.previewIds.mapNotNull(cardsById::get),
                                onToggle = { onToggleGroup(item.group.id) },
                                modifier = Modifier.animateItem(),
                            )
                            V5OverviewItem.UngroupedHeader -> SectionTitle(
                                stringResource(R.string.v5_overview_ungrouped),
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
            Toolbar(state.profileLabel, onNewTab, onDone)
        }
    }
}

@Composable
private fun Header(query: String, onQueryChange: (String) -> Unit, onCloseAll: () -> Unit) {
    val colors = V5Theme.colors
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = V5Spacing.marginPhone, end = V5Spacing.space2, top = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = V5Sizes.touchTarget)
                .clip(V5Theme.shapes.full)
                .background(colors.fill)
                .padding(horizontal = V5Spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            Icon(VolaIcons.Search, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        stringResource(R.string.v5_overview_search),
                        style = V5Theme.type.body,
                        color = colors.labelTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = V5Theme.type.body.copy(color = colors.label),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth().testTag(V5OverviewTestTags.Search),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        VolaIcons.Close,
                        contentDescription = stringResource(R.string.v5_editor_clear),
                        tint = colors.labelSecondary,
                        modifier = Modifier.size(V5Sizes.iconSmall),
                    )
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(VolaIcons.MoreHoriz, contentDescription = stringResource(R.string.cd_more_options), tint = colors.label)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_close_all_tabs)) },
                    onClick = {
                        menu = false
                        onCloseAll()
                    },
                )
            }
        }
    }
}

@Composable
private fun Segments(privateSegment: Boolean, counts: V5OverviewCounts, onChange: (Boolean) -> Unit) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = V5Spacing.marginPhone, end = V5Spacing.marginPhone, top = V5Spacing.space3)
            .clip(V5Theme.shapes.full)
            .background(colors.fill)
            .padding(V5Spacing.space1)
            .selectableGroup(),
    ) {
        listOf(false, true).forEach { private ->
            val selected = private == privateSegment
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = V5Sizes.touchTarget)
                    .clip(V5Theme.shapes.full)
                    .background(if (selected) colors.surface else colors.fill)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onChange(private) })
                    .testTag(if (private) V5OverviewTestTags.SegmentPrivate else V5OverviewTestTags.SegmentTabs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (private) {
                        stringResource(R.string.v5_overview_private, counts.private)
                    } else {
                        stringResource(R.string.v5_overview_tabs, counts.regular)
                    },
                    style = V5Theme.type.callout.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (selected) colors.label else colors.labelSecondary,
                )
            }
        }
    }
}

@Composable
private fun TabCard(
    card: V5TabCard,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onBounds: (String, Rect) -> Unit,
    hidden: Boolean,
    modifier: Modifier,
    groupTile: V5Tile? = null,
) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.m
    val view = LocalView.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    // Swipe sideways to close (system-v5.md, section 7.1): the card follows the finger, fades, and
    // past 40 % of its width or on a fling it leaves on the tabSwipe spring and the tab closes. The
    // tab closes as the card leaves, so the grid closes the gap at once; a pinned tab springs back.
    val offset = remember(card.id) { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta -> scope.launch { offset.snapTo(offset.value + delta) } }
    Column(
        modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .graphicsLayer {
                translationX = offset.value
                alpha = if (hidden) 0f else V5TabOverviewRules.swipeAlpha(offset.value, widthPx)
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    val closes = V5TabOverviewRules.closes(offset.value, velocity, widthPx, density)
                    if (closes && card.closable) {
                        view.performV5Haptic(V5Haptic.Confirm)
                        onClose(card.id)
                        if (V5Motion.animationsEnabled) {
                            val away = if (offset.value < 0f) -widthPx * 2 else widthPx * 2
                            offset.animateTo(away, V5Motion.tabSwipe.spec(), initialVelocity = velocity)
                        }
                    } else if (closes) {
                        view.performV5Haptic(V5Haptic.Reject)
                        if (V5Motion.animationsEnabled) {
                            offset.animateTo(0f, V5Motion.tabSwipe.spec(), initialVelocity = velocity)
                        } else {
                            offset.snapTo(0f)
                        }
                    } else if (V5Motion.animationsEnabled) {
                        offset.animateTo(0f, V5Motion.tabSwipe.spec(), initialVelocity = velocity)
                    } else {
                        offset.snapTo(0f)
                    }
                },
            )
            .semantics { selected = card.selected }
            .clickable(role = Role.Button, onClick = { onSelect(card.id) })
            .testTag(V5OverviewTestTags.card(card.id)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(V5Sizes.OVERVIEW_CARD_RATIO)
                .onGloballyPositioned { onBounds(card.id, Rect(it.positionInRoot(), it.size.toSize())) }
                .clip(shape)
                .background(colors.surfaceHighest)
                .then(
                    // The group's color only frames its cards (section 10); the selected card keeps the accent.
                    when {
                        card.selected -> Modifier.border(V5Sizes.overviewRing, colors.accent, shape)
                        groupTile != null -> Modifier.border(V5Sizes.groupBorder, groupTile.color, shape)
                        else -> Modifier
                    },
                ),
        ) {
            if (card.preview != null) {
                Image(
                    bitmap = card.preview,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Favicon(card, size = V5Sizes.iconMax)
                }
            }
            if (card.closable) Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(V5Sizes.touchTarget)
                    .clickable(role = Role.Button, onClick = { onClose(card.id) })
                    .testTag(V5OverviewTestTags.close(card.id)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(V5Sizes.overviewClosePlate)
                        .clip(V5Theme.shapes.full)
                        .background(V5Materials.thin.tint(dark = colors.isDark, blurAvailable = false, reduceTransparency = false)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        VolaIcons.Close,
                        contentDescription = stringResource(R.string.cd_close_tab),
                        tint = colors.label,
                        modifier = Modifier.size(V5Sizes.overviewFavicon),
                    )
                }
            }
        }
        Row(
            Modifier.padding(top = V5Spacing.space2, start = V5Spacing.space1, end = V5Spacing.space1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            Favicon(card, size = V5Sizes.overviewFavicon)
            Text(
                card.title,
                style = V5Theme.type.footnote,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Favicon(card: V5TabCard, size: Dp) {
    val favicon = card.favicon
    if (favicon != null) {
        Image(favicon, contentDescription = null, modifier = Modifier.size(size).clip(V5Theme.shapes.xs))
    } else {
        Icon(VolaIcons.Public, contentDescription = null, tint = V5Theme.colors.labelSecondary, modifier = Modifier.size(size))
    }
}

@Composable
private fun Toolbar(profileLabel: String, onNewTab: () -> Unit, onDone: () -> Unit) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(V5Materials.regular.tint(dark = colors.isDark, blurAvailable = false, reduceTransparency = false))
            .navigationBarsPadding()
            .heightIn(min = V5Sizes.bar)
            .padding(horizontal = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNewTab, modifier = Modifier.testTag(V5OverviewTestTags.NewTab)) {
            Icon(VolaIcons.Add, contentDescription = stringResource(R.string.cd_new_tab), tint = colors.accent)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                profileLabel,
                style = V5Theme.type.callout.copy(fontWeight = FontWeight.SemiBold),
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(V5Theme.shapes.full)
                    .background(colors.fill)
                    .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2),
            )
        }
        Box(
            Modifier
                .widthIn(min = V5Sizes.editorCancel)
                .heightIn(min = V5Sizes.touchTarget)
                .clickable(role = Role.Button, onClick = onDone)
                .testTag(V5OverviewTestTags.Done),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.action_done),
                style = V5Theme.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent,
            )
        }
    }
}
