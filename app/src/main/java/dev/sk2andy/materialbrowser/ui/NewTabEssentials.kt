@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.EssentialsController
import dev.sk2andy.materialbrowser.data.EssentialCandidate
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.data.EssentialsRules
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaEssentials
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaShapes
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaTypeScale
import kotlinx.coroutines.launch

internal object NewTabEssentialsTestTags {
    const val Edit = "new_tab_essentials_edit"
    const val Done = "new_tab_essentials_done"
    const val Add = "new_tab_essentials_add"
    const val Tile = "new_tab_essentials_tile"
}

/** Changes the new tab makes to its workspace's Essentials; `null` where the page is a preview. */
@Stable
internal interface NewTabEssentialsEditor {
    /** Open tabs of the workspace that are not pinned yet, most recent first. */
    val candidates: List<EssentialCandidate>

    /** Tab icons by tab id, for the add sheet. */
    val candidateIcons: Map<String, Bitmap>
    val isFull: Boolean

    fun remove(entry: EssentialEntry)
    fun move(entry: EssentialEntry, toIndex: Int)
    fun add(candidate: EssentialCandidate)
}

@Composable
internal fun rememberNewTabEssentialsEditor(
    controller: BrowserController,
    profileId: String,
): NewTabEssentialsEditor = remember(controller, profileId) {
    val essentials = controller.essentials
    object : NewTabEssentialsEditor {
        override val candidates: List<EssentialCandidate>
            get() = EssentialsRules.candidates(
                entries = essentials.entriesFor(profileId),
                tabs = controller.activeTabs
                    .filter { tab -> tab.profileId == profileId && !tab.isIncognito }
                    .sortedByDescending(BrowserTab::lastAccessedAt)
                    .map { tab -> EssentialCandidate(tab.id, tab.url, tab.title) },
            )
        override val candidateIcons: Map<String, Bitmap>
            get() = controller.favicons
        override val isFull: Boolean
            get() = essentials.entriesFor(profileId).size >= EssentialsRules.MAX_ENTRIES

        override fun remove(entry: EssentialEntry) {
            essentials.remove(profileId, entry.id)
        }

        override fun move(entry: EssentialEntry, toIndex: Int) {
            essentials.move(profileId, entry.id, toIndex)
        }

        override fun add(candidate: EssentialCandidate) {
            essentials.add(profileId, candidate.url, candidate.title, controller.favicons[candidate.tabId])
        }
    }
}

/**
 * Essentials as the NewTab board draws them: an overline with «Edit», a four-column grid of
 * 68 dp tiles with the site icon (or its letter) and a caption. «Edit» puts a ✕ on every tile,
 * lets tiles be dragged and adds an «Add» tile that opens «Add from open tabs» (board
 * EssentialsEdit). With nothing pinned the section shows the States message instead.
 */
@Composable
internal fun NewTabEssentialsSection(
    entries: List<EssentialEntry>,
    icons: Map<String, Bitmap>,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    enabled: Boolean,
    onOpen: (EssentialEntry) -> Unit,
    editor: NewTabEssentialsEditor?,
    modifier: Modifier = Modifier,
) {
    var addSheetVisible by remember { mutableStateOf(false) }
    val editable = editor != null
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VolaSpacing.x3)) {
        if (editing) {
            EssentialsEditHeader(onDone = { onEditingChange(false) })
        } else if (entries.isNotEmpty() || editable) {
            EssentialsHeader(
                showEdit = editable && entries.isNotEmpty(),
                enabled = enabled,
                onEdit = { onEditingChange(true) },
            )
        }
        if (entries.isEmpty() && !editing) {
            if (editable) {
                VolaStateMessage(
                    icon = painterResource(R.drawable.ic_push_pin),
                    title = stringResource(R.string.essentials_empty_title),
                    message = stringResource(R.string.essentials_empty_body),
                    actionLabel = stringResource(R.string.essentials_empty_action),
                    onAction = { addSheetVisible = true },
                )
            }
        } else {
            EssentialsGrid(
                entries = entries,
                icons = icons,
                editing = editing,
                enabled = enabled,
                showAdd = editing && editor?.isFull == false,
                onOpen = onOpen,
                onLongPress = { if (editable) onEditingChange(true) },
                onRemove = { entry -> editor?.remove(entry) },
                onMove = { entry, index -> editor?.move(entry, index) },
                onAdd = { addSheetVisible = true },
            )
        }
    }
    if (addSheetVisible && editor != null) {
        EssentialsAddSheet(editor = editor, onDismiss = { addSheetVisible = false })
    }
}

@Composable
private fun EssentialsHeader(showEdit: Boolean, enabled: Boolean, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LocalMinimumInteractiveComponentSize.current)
            .padding(horizontal = VolaSpacing.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.essentials_title).uppercase(),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = VolaTypeScale.overline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showEdit) {
            TextButton(
                onClick = onEdit,
                enabled = enabled,
                modifier = Modifier.testTag(NewTabEssentialsTestTags.Edit),
            ) {
                Text(stringResource(R.string.essentials_edit), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EssentialsEditHeader(onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = VolaSpacing.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.essentials_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.essentials_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onDone,
            modifier = Modifier.testTag(NewTabEssentialsTestTags.Done),
            shape = CircleShape,
        ) {
            Text(stringResource(R.string.action_done), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A dragged tile: where it was picked up and how far the finger moved since. */
private data class EssentialDrag(val id: String, val origin: Offset, val delta: Offset)

@Composable
private fun EssentialsGrid(
    entries: List<EssentialEntry>,
    icons: Map<String, Bitmap>,
    editing: Boolean,
    enabled: Boolean,
    showAdd: Boolean,
    onOpen: (EssentialEntry) -> Unit,
    onLongPress: () -> Unit,
    onRemove: (EssentialEntry) -> Unit,
    onMove: (EssentialEntry, Int) -> Unit,
    onAdd: () -> Unit,
) {
    val view = LocalView.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var drag by remember { mutableStateOf<EssentialDrag?>(null) }
    var draggedPlaced by remember { mutableStateOf<Offset?>(null) }
    var pitch by remember { mutableStateOf(Offset.Zero) }
    var tileExtent by remember { mutableStateOf(Offset.Zero) }
    val positions = remember { mutableMapOf<String, Offset>() }
    val latestEntries by rememberUpdatedState(entries)
    val latestOnMove by rememberUpdatedState(onMove)
    // Pointer handlers outlive recompositions, so they read state, never captured values.
    fun dropIndex(): Int? = drag?.let { active ->
        val center = active.origin + active.delta + tileExtent / 2f
        EssentialsGridRules.slotAt(
            x = center.x,
            y = center.y,
            pitchX = pitch.x,
            pitchY = pitch.y,
            columns = VolaEssentials.COLUMNS,
            count = latestEntries.size,
            rtl = rtl,
        )
    }
    val currentDrag = drag
    // While a tile is dragged the others already make room for it.
    val shown = currentDrag?.let { active ->
        dropIndex()?.let { target -> EssentialsRules.move(entries, active.id, target) }
    } ?: entries
    Layout(
        content = {
            shown.forEachIndexed { index, entry ->
                key(entry.id) {
                    val dragged = currentDrag?.id == entry.id
                    EssentialTile(
                        entry = entry,
                        icon = icons[entry.url],
                        colorIndex = entries.indexOf(entry),
                        editing = editing,
                        enabled = enabled,
                        dragged = dragged,
                        canMoveEarlier = index > 0,
                        canMoveLater = index < shown.lastIndex,
                        onOpen = { onOpen(entry) },
                        onLongPress = {
                            view.performConfirmHaptic()
                            onLongPress()
                        },
                        onRemove = { onRemove(entry) },
                        onMove = { delta -> onMove(entry, index + delta) },
                        modifier = Modifier
                            .then(if (dragged) Modifier.zIndex(1f) else Modifier.animatePlacement())
                            .onPlaced { coordinates ->
                                val position = coordinates.positionInParent()
                                positions[entry.id] = position
                                if (drag?.id == entry.id) draggedPlaced = position
                                if (index == 0) {
                                    tileExtent = Offset(
                                        coordinates.size.width.toFloat(),
                                        coordinates.size.height.toFloat(),
                                    )
                                }
                            }
                            .then(
                                if (editing && enabled) {
                                    Modifier.pointerInput(entry.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                view.performTabFocusHaptic()
                                                val origin = positions[entry.id] ?: Offset.Zero
                                                draggedPlaced = origin
                                                drag = EssentialDrag(entry.id, origin, Offset.Zero)
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                drag = drag?.let { it.copy(delta = it.delta + amount) }
                                            },
                                            onDragEnd = {
                                                val target = dropIndex()
                                                drag = null
                                                if (target != null) {
                                                    view.performConfirmHaptic()
                                                    latestOnMove(entry, target)
                                                }
                                            },
                                            onDragCancel = { drag = null },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (dragged) {
                                    Modifier.graphicsLayer {
                                        val active = drag ?: return@graphicsLayer
                                        val at = draggedPlaced ?: active.origin
                                        translationX = active.origin.x + active.delta.x - at.x
                                        translationY = active.origin.y + active.delta.y - at.y
                                        scaleX = VolaEssentials.DRAG_SCALE
                                        scaleY = VolaEssentials.DRAG_SCALE
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
            if (showAdd) {
                key(ADD_TILE_KEY) {
                    EssentialAddTile(onAdd = onAdd, modifier = Modifier.animatePlacement())
                }
            }
        },
    ) { measurables, constraints ->
        val columns = VolaEssentials.COLUMNS
        val gapX = VolaEssentials.columnGap.roundToPx()
        val gapY = VolaEssentials.rowGap.roundToPx()
        val cellWidth = ((constraints.maxWidth - gapX * (columns - 1)) / columns).coerceAtLeast(0)
        val placeables = measurables.map { measurable ->
            measurable.measure(Constraints(minWidth = cellWidth, maxWidth = cellWidth))
        }
        val rowHeights = placeables.chunked(columns).map { row -> row.maxOf { it.height } }
        val height = rowHeights.sum() + gapY * (rowHeights.size - 1).coerceAtLeast(0)
        pitch = Offset((cellWidth + gapX).toFloat(), ((rowHeights.firstOrNull() ?: 0) + gapY).toFloat())
        layout(constraints.maxWidth, height) {
            var y = 0
            placeables.chunked(columns).forEachIndexed { row, items ->
                items.forEachIndexed { column, placeable ->
                    placeable.placeRelative(column * (cellWidth + gapX), y)
                }
                y += rowHeights[row] + gapY
            }
        }
    }
}

private const val ADD_TILE_KEY = "essentials-add"

/** Springs a tile from its old slot to its new one when the grid changes order. */
private fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    val animatable = remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    var target by remember { mutableStateOf<IntOffset?>(null) }
    onPlaced { coordinates -> target = coordinates.positionInParent().round() }
        .offset {
            val destination = target ?: return@offset IntOffset.Zero
            val current = animatable.value
                ?: Animatable(destination, IntOffset.VectorConverter).also { animatable.value = it }
            if (current.targetValue != destination) {
                scope.launch { current.animateTo(destination, VolaMotion.standard()) }
            }
            current.value - destination
        }
}

@Composable
private fun EssentialTile(
    entry: EssentialEntry,
    icon: Bitmap?,
    colorIndex: Int,
    editing: Boolean,
    enabled: Boolean,
    dragged: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = EssentialsRules.label(entry)
    val removeLabel = stringResource(R.string.essentials_remove, label)
    val earlierLabel = stringResource(R.string.essentials_move_earlier)
    val laterLabel = stringResource(R.string.essentials_move_later)
    Column(
        modifier = modifier
            .testTag(NewTabEssentialsTestTags.Tile)
            .then(
                if (editing) {
                    Modifier.semantics {
                        contentDescription = label
                        customActions = listOfNotNull(
                            CustomAccessibilityAction(removeLabel) { onRemove(); true },
                            CustomAccessibilityAction(earlierLabel) { onMove(-1); true }
                                .takeIf { canMoveEarlier },
                            CustomAccessibilityAction(laterLabel) { onMove(1); true }
                                .takeIf { canMoveLater },
                        )
                    }
                } else {
                    Modifier.combinedClickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onOpen,
                        onLongClick = onLongPress,
                    )
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolaEssentials.labelGap),
    ) {
        Box(modifier = Modifier.size(VolaEssentials.tileSize)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = VolaEssentials.tileShape,
                // The card color: lowest surface in light, a raised surface on pure black.
                color = VolaTheme.extendedColors.card.copy(alpha = VolaEssentials.TILE_ALPHA),
                shadowElevation = if (dragged) VolaElevation.level3 else VolaElevation.level1,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    EssentialIcon(
                        entry = entry,
                        icon = icon,
                        colorIndex = colorIndex,
                        size = VolaEssentials.iconSize,
                    )
                }
            }
            if (editing) {
                RemoveBadge(
                    contentDescription = removeLabel,
                    enabled = enabled,
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The ✕ over a tile's corner; its touch target is the full minimum size around it. */
@Composable
private fun RemoveBadge(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val touch = LocalMinimumInteractiveComponentSize.current
    val shift = -VolaEssentials.removeBadgeInset - (touch - VolaEssentials.removeBadgeSize) / 2
    Box(
        modifier = modifier
            .offset(x = shift, y = shift)
            .size(touch)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(VolaEssentials.removeBadgeSize)
                .border(VolaEssentials.removeBadgeRing, MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                .padding(VolaEssentials.removeBadgeRing)
                .background(MaterialTheme.colorScheme.inverseSurface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = VolaIcons.Close,
                contentDescription = null,
                modifier = Modifier.size(VolaEssentials.removeIconSize),
                tint = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

@Composable
private fun EssentialAddTile(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .testTag(NewTabEssentialsTestTags.Add)
            .clickable(role = Role.Button, onClick = onAdd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolaEssentials.labelGap),
    ) {
        Box(
            modifier = Modifier
                .size(VolaEssentials.tileSize)
                .border(VolaEssentials.addOutline, primary, VolaEssentials.tileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = VolaIcons.Add, contentDescription = null, tint = primary)
        }
        Text(
            text = stringResource(R.string.essentials_add),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = primary,
            maxLines = 1,
        )
    }
}

/** The site icon, or its first letter on one of the theme's container colors. */
@Composable
internal fun EssentialIcon(
    entry: EssentialEntry,
    icon: Bitmap?,
    colorIndex: Int,
    size: Dp,
) {
    if (icon != null && !icon.isRecycled) {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(VolaEssentials.iconShape),
        )
        return
    }
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (Math.floorMod(colorIndex, MONOGRAM_COLOR_COUNT)) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(VolaEssentials.iconShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = EssentialsRules.monogram(entry),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
        )
    }
}

private const val MONOGRAM_COLOR_COUNT = 3

/** «Add from open tabs» (board EssentialsEdit): the workspace's tabs that are not pinned yet. */
@Composable
private fun EssentialsAddSheet(editor: NewTabEssentialsEditor, onDismiss: () -> Unit) {
    val candidates = editor.candidates
    val icons = editor.candidateIcons
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = VolaShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = VolaSpacing.x4, end = VolaSpacing.x4, bottom = VolaSpacing.x8),
            verticalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
        ) {
            Text(
                text = stringResource(R.string.essentials_add_sheet_title),
                modifier = Modifier
                    .padding(horizontal = VolaSpacing.x1)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            when {
                editor.isFull -> Text(
                    text = pluralStringResource(
                        R.plurals.essentials_full,
                        EssentialsRules.MAX_ENTRIES,
                        EssentialsRules.MAX_ENTRIES,
                    ),
                    modifier = Modifier.padding(horizontal = VolaSpacing.x1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                candidates.isEmpty() -> Text(
                    text = stringResource(R.string.essentials_add_sheet_empty),
                    modifier = Modifier.padding(horizontal = VolaSpacing.x1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Surface(
                    shape = VolaShapes.card,
                    color = VolaTheme.extendedColors.card,
                ) {
                    Column {
                        candidates.forEachIndexed { index, candidate ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = VolaSpacing.x4),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                )
                            }
                            EssentialCandidateRow(
                                candidate = candidate,
                                icon = icons[candidate.tabId],
                                colorIndex = index,
                                onAdd = { editor.add(candidate) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EssentialCandidateRow(
    candidate: EssentialCandidate,
    icon: Bitmap?,
    colorIndex: Int,
    onAdd: () -> Unit,
) {
    val entry = EssentialEntry(url = candidate.url, title = candidate.title)
    val label = EssentialsRules.label(entry)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VolaEssentials.sheetRowMinHeight)
            .padding(start = VolaSpacing.x4, end = VolaSpacing.x2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
    ) {
        EssentialIcon(entry = entry, icon = icon, colorIndex = colorIndex, size = VolaEssentials.sheetIconSize)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = EssentialsRules.host(candidate.url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FilledTonalIconButton(onClick = onAdd) {
            Icon(
                imageVector = VolaIcons.Add,
                contentDescription = stringResource(R.string.essentials_add_tab, label),
            )
        }
    }
}

/** «Removed from Essentials · Undo» on the browser's snackbar, like a closed tab. */
@Composable
internal fun EssentialRemovalSnackbarEffect(
    essentials: EssentialsController,
    hostState: SnackbarHostState,
) {
    val removal = essentials.removal
    val message = stringResource(R.string.essentials_removed)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(removal) {
        val token = removal ?: return@LaunchedEffect
        hostState.currentSnackbarData?.dismiss()
        try {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) essentials.undoRemoval(token)
        } finally {
            essentials.dismissRemoval(token)
        }
    }
}
