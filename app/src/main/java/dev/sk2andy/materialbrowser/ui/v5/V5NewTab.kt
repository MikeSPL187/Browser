package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.zIndex
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5NewTabTestTags {
    const val Page = "v5_new_tab"
    const val Edit = "v5_new_tab_edit"
    const val Add = "v5_new_tab_add"
    const val Report = "v5_new_tab_report"
    const val ReportMore = "v5_new_tab_report_more"
    fun pinned(id: String) = "v5_new_tab_pinned:$id"
    fun recent(id: String) = "v5_new_tab_recent:$id"
}

/** A pinned site: its name under the tile, and its icon if the site has one. */
@Immutable
data class V5PinnedSite(val id: String, val title: String, val icon: ImageBitmap?)

/** A tab used lately, to carry on with. */
@Immutable
data class V5RecentTab(val id: String, val title: String, val host: String, val favicon: ImageBitmap?)

/** The week of the privacy report as the new tab's card shows it. */
@Immutable
data class V5NewTabReport(val total: Int, val daily: List<Int>, val weekdays: List<String>)

@Immutable
data class V5NewTabState(
    /** The profile chip; null keeps the top empty. */
    val profileLabel: String?,
    val pinned: List<V5PinnedSite>,
    /** False once the profile pins as many sites as it may keep. */
    val canAdd: Boolean,
    /** Null when the user hid the card. */
    val report: V5NewTabReport?,
    val recent: List<V5RecentTab>,
    /** The profile's symbol on its accent; null shows the name's first letter. */
    val profileIcon: ImageVector? = null,
)

/**
 * The v5 new tab (V13a, canvas screen «а», system-v5.md section 11): a light wash of the profile's
 * accent at the top, the profile, pinned sites four to a row — each tile is its own icon over a blurred
 * copy of it, Zen's trick — the week of the privacy report and the tabs used last. «Edit» turns a tap
 * on a tile into «remove»; back or «Done» ends it.
 */
@Composable
fun V5NewTab(
    state: V5NewTabState,
    onOpenPinned: (String) -> Unit,
    onRemovePinned: (String) -> Unit,
    onAddPinned: () -> Unit,
    onOpenReport: () -> Unit,
    onHideReport: () -> Unit,
    onOpenRecent: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    /** A pinned site was dragged (or moved by TalkBack) to a new place among the pinned ones. */
    onMovePinned: (id: String, toIndex: Int) -> Unit = { _, _ -> },
) {
    val colors = V5Theme.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    val nothingPinned = state.pinned.isEmpty()
    LaunchedEffect(nothingPinned) { if (nothingPinned) editing = false }
    BackHandler(enabled = editing) { editing = false }
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(V5NewTabTestTags.Page),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(V5Sizes.newTabWash)
                .background(Brush.verticalGradient(listOf(colors.accentSoft, Color.Transparent))),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = V5Spacing.marginPhone, vertical = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
        ) {
            state.profileLabel?.let { ProfileChip(it, state.profileIcon) }
            Pinned(
                state = state,
                editing = editing,
                onEditingChange = { editing = it },
                onOpen = onOpenPinned,
                onRemove = onRemovePinned,
                onAdd = onAddPinned,
                onMove = onMovePinned,
            )
            state.report?.let { ReportCard(it, onOpenReport, onHideReport) }
            if (state.recent.isNotEmpty()) Recent(state.recent, onOpenRecent)
        }
    }
}

@Composable
private fun ProfileChip(label: String, icon: ImageVector?) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .clip(V5Theme.shapes.full)
            .background(colors.surface)
            .padding(start = V5Spacing.space1, end = V5Spacing.space4, top = V5Spacing.space1, bottom = V5Spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Box(
            Modifier
                .size(V5Sizes.rowTile)
                .clip(V5Theme.shapes.full)
                .background(colors.accentFill),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.onAccentFill,
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
            } else {
                Text(V5NewTabRules.letter(label), style = V5Theme.type.footnote, color = colors.onAccentFill)
            }
        }
        Text(label, style = V5Theme.type.headline, color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}, actionTag: String = "") {
    val colors = V5Theme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = V5Theme.type.title,
            color = colors.label,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (action != null) {
            Box(
                Modifier
                    .heightIn(min = V5Sizes.touchTarget)
                    .clip(V5Theme.shapes.full)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = V5Spacing.space2)
                    .testTag(actionTag),
                contentAlignment = Alignment.Center,
            ) {
                Text(action, style = V5Theme.type.body, color = colors.accent)
            }
        }
    }
}

/**
 * The pinned sites. While editing, a long press lifts a tile and it follows the finger; the others
 * make room at once and the drop saves the new place. Outside editing a long press starts it.
 */
@Composable
private fun Pinned(
    state: V5NewTabState,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
    onMove: (String, Int) -> Unit,
) {
    val view = LocalView.current
    val ids = state.pinned.map { it.id }
    val byId = state.pinned.associateBy { it.id }
    // Where each tile sits in the root, before any drag offset, to find the tile under the finger.
    val bounds = remember { HashMap<String, Rect>() }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOrder by remember { mutableStateOf(emptyList<String>()) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var grab by remember { mutableStateOf(Offset.Zero) }
    val currentIds by rememberUpdatedState(ids)
    val move by rememberUpdatedState(onMove)
    val shown = if (dragId == null) ids else dragOrder
    val moveEarlier = stringResource(R.string.v5_newtab_move_earlier)
    val moveLater = stringResource(R.string.v5_newtab_move_later)
    Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
        SectionHeader(
            title = stringResource(R.string.v5_newtab_pinned),
            action = when {
                state.pinned.isEmpty() -> null
                editing -> stringResource(R.string.v5_newtab_done)
                else -> stringResource(R.string.v5_newtab_edit)
            },
            onAction = { onEditingChange(!editing) },
            actionTag = V5NewTabTestTags.Edit,
        )
        V5NewTabRules.rows(shown.size, canAdd = state.canAdd && !editing).forEach { row ->
            val lifted = row.any { it is V5NewTabRules.Cell.Site && shown[it.index] == dragId }
            Row(Modifier.fillMaxWidth().zIndex(if (lifted) 1f else 0f)) {
                row.forEach { cell ->
                    val cellModifier = Modifier.weight(1f)
                    when (cell) {
                        is V5NewTabRules.Cell.Site -> {
                            val id = shown[cell.index]
                            val site = byId[id]
                            if (site == null) {
                                Spacer(cellModifier)
                            } else {
                                val index = cell.index
                                val dragging = id == dragId
                                PinnedTile(
                                    site = site,
                                    editing = editing,
                                    onClick = { if (editing) onRemove(id) else onOpen(id) },
                                    // While editing a long press lifts the tile (below); taking it here
                                    // keeps its release from counting as a tap that would unpin.
                                    onLongClick = if (editing) {
                                        {}
                                    } else {
                                        {
                                            view.performV5Haptic(V5Haptic.LongPress)
                                            onEditingChange(true)
                                        }
                                    },
                                    modifier = cellModifier
                                        .onGloballyPositioned { bounds[id] = it.boundsInRoot() }
                                        .then(
                                            if (editing) {
                                                Modifier.pointerInput(id) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { local ->
                                                            val start = bounds[id] ?: return@detectDragGesturesAfterLongPress
                                                            grab = local
                                                            pointer = start.topLeft + local
                                                            dragOrder = currentIds
                                                            dragId = id
                                                            view.performV5Haptic(V5Haptic.DragStart)
                                                        },
                                                        onDrag = { change, amount ->
                                                            change.consume()
                                                            pointer += amount
                                                            val under = dragOrder.firstOrNull { other ->
                                                                other != id && bounds[other]?.contains(pointer) == true
                                                            }
                                                            if (under != null) {
                                                                dragOrder = V5NewTabRules.moved(dragOrder, id, dragOrder.indexOf(under))
                                                                view.performV5Haptic(V5Haptic.SegmentTick)
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            val to = dragOrder.indexOf(id)
                                                            dragId = null
                                                            if (to >= 0 && to != currentIds.indexOf(id)) move(id, to)
                                                        },
                                                        onDragCancel = { dragId = null },
                                                    )
                                                }
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .graphicsLayer {
                                            val base = bounds[id]
                                            if (dragging && base != null) {
                                                translationX = pointer.x - grab.x - base.left
                                                translationY = pointer.y - grab.y - base.top
                                                scaleX = PINNED_LIFT_SCALE
                                                scaleY = PINNED_LIFT_SCALE
                                            }
                                        }
                                        .semantics {
                                            if (editing) {
                                                customActions = buildList {
                                                    if (index > 0) {
                                                        add(CustomAccessibilityAction(moveEarlier) { move(id, index - 1); true })
                                                    }
                                                    if (index < shown.lastIndex) {
                                                        add(CustomAccessibilityAction(moveLater) { move(id, index + 1); true })
                                                    }
                                                }
                                            }
                                        },
                                )
                            }
                        }
                        V5NewTabRules.Cell.Add -> AddTile(onAdd, cellModifier)
                    }
                }
                repeat(V5Sizes.PINNED_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PinnedTile(
    site: V5PinnedSite,
    editing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = V5Theme.colors
    // TalkBack reads the name under the tile; while editing, the action says it removes the site.
    val removeLabel = stringResource(R.string.v5_newtab_remove_pinned, site.title)
    TileCell(
        label = site.title,
        onClick = onClick,
        onClickLabel = if (editing) removeLabel else null,
        onLongClick = onLongClick,
        onLongClickLabel = if (editing) null else stringResource(R.string.v5_newtab_edit),
        modifier = modifier.testTag(V5NewTabTestTags.pinned(site.id)),
    ) {
        Box(
            Modifier
                .size(V5Sizes.pinnedTile)
                .clip(V5Theme.shapes.m),
            contentAlignment = Alignment.Center,
        ) {
            val icon = site.icon
            if (icon != null) {
                // The site's own colors behind it, without anyone choosing them.
                Image(
                    icon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().blur(V5Sizes.pinnedBlur),
                )
                Box(Modifier.matchParentSize().background(colors.surface.copy(alpha = PINNED_VEIL_ALPHA)))
                Image(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(V5Sizes.pinnedFavicon).clip(V5Theme.shapes.xs),
                )
            } else {
                val tile = V5NewTabRules.letterTile(site.id)
                Box(Modifier.matchParentSize().background(tile.color))
                Text(V5NewTabRules.letter(site.title), style = V5Theme.type.title, color = tile.glyph)
            }
        }
        if (editing) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = -V5Spacing.space1, y = -V5Spacing.space1)
                    .size(V5Sizes.pinnedBadge)
                    .clip(V5Theme.shapes.full)
                    .background(colors.labelSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VolaIcons.Remove, contentDescription = null, tint = colors.background, modifier = Modifier.size(V5Sizes.iconSmall))
            }
        }
    }
}

@Composable
private fun AddTile(onAdd: () -> Unit, modifier: Modifier) {
    val colors = V5Theme.colors
    val label = stringResource(R.string.v5_newtab_add)
    TileCell(label = label, onClick = onAdd, onClickLabel = null, modifier = modifier.testTag(V5NewTabTestTags.Add)) {
        Box(
            Modifier
                .size(V5Sizes.pinnedTile)
                .clip(V5Theme.shapes.m)
                .background(colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VolaIcons.Add, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.icon))
        }
    }
}

/** A tile with its name under it; the tile box is [tile]'s scope, so a badge can sit on its corner. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TileCell(
    label: String,
    onClick: () -> Unit,
    onClickLabel: String?,
    modifier: Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    tile: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier
            .clip(V5Theme.shapes.m)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick,
                onLongClickLabel = onLongClickLabel.takeIf { onLongClick != null },
                onLongClick = onLongClick,
            )
            .padding(vertical = V5Spacing.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space1),
    ) {
        Box(content = tile)
        Text(
            label,
            style = V5Theme.type.caption,
            color = V5Theme.colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = V5Spacing.space1),
        )
    }
}

/** The week of the privacy report; a tap opens the whole report, «•••» hides the card. */
@Composable
private fun ReportCard(report: V5NewTabReport, onOpen: () -> Unit, onHide: () -> Unit) {
    val colors = V5Theme.colors
    val more = stringResource(R.string.v5_newtab_report_more)
    val moreLabel = stringResource(R.string.v5_newtab_more)
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(V5Theme.shapes.m)
            .background(colors.surface)
            .clickable(role = Role.Button, onClickLabel = more, onClick = onOpen)
            .padding(start = V5Spacing.space4, end = V5Spacing.space1, top = V5Spacing.space1, bottom = V5Spacing.space4)
            .testTag(V5NewTabTestTags.Report),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
            Icon(VolaIcons.ShieldLock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.iconSmall))
            Text(
                stringResource(R.string.v5_report_title),
                style = V5Theme.type.headline,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box {
                Box(
                    Modifier
                        .size(V5Sizes.touchTarget)
                        .clip(V5Theme.shapes.full)
                        .clickable(role = Role.Button, onClick = { menu = true })
                        .semantics { contentDescription = moreLabel }
                        .testTag(V5NewTabTestTags.ReportMore),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VolaIcons.MoreHoriz, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.v5_report_hide_card)) },
                        onClick = {
                            menu = false
                            onHide()
                        },
                    )
                }
            }
        }
        Column(
            Modifier.padding(end = V5Spacing.space3),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            V5ReportTotal(report.total)
            if (report.total > 0) {
                V5ReportChart(report.daily, rememberCountFormat().format(report.daily.lastOrNull() ?: 0))
                Row(horizontalArrangement = Arrangement.spacedBy(V5Sizes.reportBarGap)) {
                    report.weekdays.forEach { day ->
                        Text(
                            day,
                            style = V5Theme.type.caption,
                            color = colors.labelSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Text(stringResource(R.string.v5_report_empty), style = V5Theme.type.footnote, color = colors.labelSecondary)
            }
            Text(more, style = V5Theme.type.footnote, color = colors.accent)
        }
    }
}

@Composable
private fun Recent(tabs: List<V5RecentTab>, onOpen: (String) -> Unit) {
    val colors = V5Theme.colors
    Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
        SectionHeader(stringResource(R.string.v5_newtab_recent))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(V5Theme.shapes.m)
                .background(colors.surface),
        ) {
            tabs.forEach { tab ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = V5Sizes.rowTwoLine)
                        .clickable(role = Role.Button, onClick = { onOpen(tab.id) })
                        .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2)
                        .testTag(V5NewTabTestTags.recent(tab.id)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
                ) {
                    val favicon = tab.favicon
                    if (favicon != null) {
                        Image(favicon, contentDescription = null, modifier = Modifier.size(V5Sizes.iconSmall).clip(V5Theme.shapes.xs))
                    } else {
                        Icon(VolaIcons.Public, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(tab.title, style = V5Theme.type.body, color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(tab.host, style = V5Theme.type.footnote, color = colors.labelSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** A lifted tile grows a little under the finger. */
private const val PINNED_LIFT_SCALE = 1.08f

/** How much of the surface lies over a tile's blurred icon, so the icon on top stands out. */
private const val PINNED_VEIL_ALPHA = 0.35f
