package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    const val Profile = "v5_tab_overview_profile"
    const val CloseAll = "v5_tab_overview_close_all"
    const val Empty = "v5_tab_overview_empty"
    const val EmptyNewTab = "v5_tab_overview_empty_new_tab"
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
    /** The profile chip: the profile's name, after its symbol. */
    val profileLabel: String,
    /** The profile's tab groups (V12); empty keeps one flat grid. */
    val groups: List<V5OverviewGroup> = emptyList(),
    /** The profile's symbol on the chip (a Material Symbol for its stored emoji key). */
    val profileIcon: ImageVector? = null,
    /** Whether the profile can have private tabs: without them «+» leaves the private segment (О-2). */
    val canOpenPrivate: Boolean = true,
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
    /** A card was long-pressed: its tab moves into a group (V12b); null keeps cards tap-only. */
    onCardLongPress: ((String) -> Unit)? = null,
    /** A group's header or collapsed row was long-pressed: the group is edited (V12b). */
    onGroupLongPress: ((String) -> Unit)? = null,
    /** The profile chip was tapped: the profiles sheet opens (V11a); null keeps the chip a label. */
    onProfiles: (() -> Unit)? = null,
    /** A pinned card was swiped away: it stays, and the host says why (О-5). */
    onCloseRefused: () -> Unit = {},
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
            val canAddTab = V5TabOverviewRules.canAddTab(state.privateSegment, state.canOpenPrivate)
            Header(state.query, onQueryChange, onCloseAll, canCloseAll = state.cards.any(V5TabCard::closable))
            Segments(state.privateSegment, state.counts, onSegmentChange)
            if (state.cards.isEmpty()) {
                Empty(
                    empty = V5TabOverviewRules.empty(state.query, state.privateSegment, state.canOpenPrivate),
                    query = state.query.trim(),
                    onNewTab = onNewTab,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
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
                                    onCloseRefused = onCloseRefused,
                                    onBounds = onCardBounds,
                                    hidden = card.id == heroCardId,
                                    groupTile = item.group?.tile,
                                    onLongPress = onCardLongPress,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                            is V5OverviewItem.GroupHeader -> GroupHeader(
                                group = item.group,
                                count = item.count,
                                onToggle = { onToggleGroup(item.group.id) },
                                onLongPress = onGroupLongPress?.let { edit -> { edit(item.group.id) } },
                                modifier = Modifier.animateItem(),
                            )
                            is V5OverviewItem.CollapsedGroup -> CollapsedGroup(
                                group = item.group,
                                count = item.count,
                                previews = item.previewIds.mapNotNull(cardsById::get),
                                onToggle = { onToggleGroup(item.group.id) },
                                onLongPress = onGroupLongPress?.let { edit -> { edit(item.group.id) } },
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
            Toolbar(state.profileLabel, state.profileIcon, onNewTab.takeIf { canAddTab }, onDone, onProfiles)
        }
    }
}

/** An empty grid says why, and offers the one thing to do next (О-3): never just «No tabs found». */
@Composable
private fun Empty(empty: V5OverviewEmpty, query: String, onNewTab: () -> Unit, modifier: Modifier) {
    val colors = V5Theme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = V5Sizes.stateContentMax)
                .padding(horizontal = V5Spacing.space6)
                .testTag(V5OverviewTestTags.Empty),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            val title = when (empty) {
                V5OverviewEmpty.NoMatches -> stringResource(R.string.v5_overview_no_matches, query)
                V5OverviewEmpty.NoPrivateTabs -> stringResource(R.string.v5_overview_private_empty_title)
                V5OverviewEmpty.PrivateUnavailable -> stringResource(R.string.v5_overview_private_unavailable)
                V5OverviewEmpty.NoTabs -> stringResource(R.string.v5_overview_tabs_empty_title)
            }
            Text(
                title,
                style = V5Theme.type.headline,
                color = colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (empty == V5OverviewEmpty.NoPrivateTabs) {
                Text(
                    stringResource(R.string.v5_overview_private_empty_message),
                    style = V5Theme.type.callout,
                    color = colors.labelSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            val action = when (empty) {
                V5OverviewEmpty.NoPrivateTabs -> R.string.v5_menu_new_private_tab
                V5OverviewEmpty.NoTabs -> R.string.cd_new_tab
                V5OverviewEmpty.NoMatches, V5OverviewEmpty.PrivateUnavailable -> null
            }
            if (action != null) {
                V5StatePrimaryAction(
                    V5StateAction(
                        label = stringResource(action),
                        onClick = onNewTab,
                        icon = VolaIcons.Add,
                        testTag = V5OverviewTestTags.EmptyNewTab,
                    ),
                    modifier = Modifier.padding(top = V5Spacing.space2),
                )
            }
        }
    }
}

@Composable
private fun Header(query: String, onQueryChange: (String) -> Unit, onCloseAll: () -> Unit, canCloseAll: Boolean) {
    val colors = V5Theme.colors
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = V5Spacing.marginPhone, end = V5Spacing.space2, top = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        V5SearchField(
            query = query,
            onQueryChange = onQueryChange,
            hint = stringResource(R.string.v5_overview_search),
            modifier = Modifier.weight(1f),
            fieldModifier = Modifier.testTag(V5OverviewTestTags.Search),
        )
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(VolaIcons.MoreHoriz, contentDescription = stringResource(R.string.cd_more_options), tint = colors.label)
            }
            // «More» offers closing every tab here, in red as an action that removes.
            V5PromptSheet(
                visible = menu,
                title = stringResource(R.string.cd_more_options),
                message = null,
                actions = listOf(
                    V5PromptAction(
                        label = stringResource(R.string.action_close_all_tabs),
                        role = V5PromptRole.Destructive,
                        testTag = V5OverviewTestTags.CloseAll,
                        // Nothing in front to close (an empty segment, only pinned tabs): the row says so by fading.
                        enabled = canCloseAll,
                        onClick = {
                            menu = false
                            onCloseAll()
                        },
                    ),
                ),
                onDismiss = { menu = false },
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabCard(
    card: V5TabCard,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onCloseRefused: () -> Unit,
    onBounds: (String, Rect) -> Unit,
    hidden: Boolean,
    modifier: Modifier,
    groupTile: V5Tile? = null,
    onLongPress: ((String) -> Unit)? = null,
) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.m
    val longPressLabel = stringResource(R.string.v5_group_move_title)
    val pinnedLabel = stringResource(R.string.v5_newtab_pinned)
    val view = LocalView.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    // Swipe sideways to close (system-v5.md, section 7.1): the card follows the finger, fades, and
    // past 40 % of its width or on a fling it leaves on the tabSwipe spring and the tab closes. The
    // tab closes once the card is off screen, so the leaving is seen (M-03) and the grid then closes
    // the gap; a pinned tab springs back, and the host says why.
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
                        if (V5Motion.animationsEnabled) {
                            val away = if (offset.value < 0f) -widthPx * 2 else widthPx * 2
                            offset.animateTo(away, V5Motion.tabSwipe.spec(), initialVelocity = velocity)
                        }
                        onClose(card.id)
                        // A card still here a moment later was not closed (the overview is leaving): it comes back.
                        delay(V5SwipeToDeleteRules.RETURN_AFTER_MILLIS)
                        if (V5Motion.animationsEnabled) offset.animateTo(0f, V5Motion.tabSwipe.spec()) else offset.snapTo(0f)
                    } else if (closes) {
                        view.performV5Haptic(V5Haptic.Reject)
                        onCloseRefused()
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
            .semantics {
                selected = card.selected
                // A pinned card has no «×»: TalkBack hears why (О-5).
                if (!card.closable) stateDescription = pinnedLabel
            }
            .combinedClickable(
                role = Role.Button,
                onClick = { onSelect(card.id) },
                onLongClickLabel = longPressLabel.takeIf { onLongPress != null },
                onLongClick = onLongPress?.let { press ->
                    {
                        view.performV5Haptic(V5Haptic.LongPress)
                        press(card.id)
                    }
                },
            )
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
                        // Each card's «×» names its tab, as «Unpin %1$s» on the new tab page does (О-4).
                        contentDescription = stringResource(R.string.v5_overview_close_named, card.title),
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
                modifier = Modifier.weight(1f, fill = false),
            )
            // Seen as well as heard: a pinned card shows its pin where the others have nothing.
            if (!card.closable) {
                Icon(
                    VolaIcons.PushPin,
                    contentDescription = null,
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(V5Sizes.overviewFavicon),
                )
            }
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
private fun Toolbar(
    profileLabel: String,
    profileIcon: ImageVector?,
    /** Null where the segment in front cannot have a new tab: no «+» at all rather than a wrong tab. */
    onNewTab: (() -> Unit)?,
    onDone: () -> Unit,
    onProfiles: (() -> Unit)?,
) {
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
        if (onNewTab != null) {
            IconButton(onClick = onNewTab, modifier = Modifier.testTag(V5OverviewTestTags.NewTab)) {
                Icon(VolaIcons.Add, contentDescription = stringResource(R.string.cd_new_tab), tint = colors.accent)
            }
        } else {
            // The bar keeps its layout: the chip stays centered.
            Box(Modifier.size(V5Sizes.touchTarget))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            // The chip keeps its look; tapping it lists the profiles, its touch target is the bar's.
            val chooseLabel = stringResource(R.string.v5_profiles_choose)
            val chip = if (onProfiles != null) {
                Modifier
                    .heightIn(min = V5Sizes.touchTarget)
                    .clickable(role = Role.Button, onClickLabel = chooseLabel, onClick = onProfiles)
                    .testTag(V5OverviewTestTags.Profile)
            } else {
                Modifier
            }
            Box(chip, contentAlignment = Alignment.Center) {
                Row(
                    Modifier
                        .clip(V5Theme.shapes.full)
                        .background(colors.fill)
                        .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2),
                    horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (profileIcon != null) {
                        Icon(
                            profileIcon,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(V5Sizes.iconSmall),
                        )
                    }
                    Text(
                        profileLabel,
                        style = V5Theme.type.callout.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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
