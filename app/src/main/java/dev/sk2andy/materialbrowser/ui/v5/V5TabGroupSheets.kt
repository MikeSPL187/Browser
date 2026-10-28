package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic

/*
 * Tab group sheets of the v5 overview (V12b, Safari's «Move to Tab Group»): a long press on a card
 * moves its tab into a group, into a new one, or out of its group; a long press on a group's
 * header renames it, recolors it or ungroups its tabs. The browser binds them to TabStack.
 */

object V5TabGroupSheetTestTags {
    const val NewGroup = "v5_group_new"
    const val RemoveFromGroup = "v5_group_remove_tab"
    const val NameField = "v5_group_name"
    const val Save = "v5_group_save"
    const val Ungroup = "v5_group_ungroup"
    const val CloseGroup = "v5_group_close"
    const val CloseGroupConfirm = "v5_group_close_confirm"
    const val AddTabs = "v5_group_add_tabs"
    const val AddTabsBack = "v5_group_add_tabs_back"
    fun tab(id: String) = "v5_group_tab:$id"
    fun candidate(id: String) = "v5_group_candidate:$id"
    fun moveTo(id: String) = "v5_group_move_to:$id"
    fun color(index: Int) = "v5_group_color:$index"
}

/**
 * «Move to group» for one tab ([tabTitle]): the profile's [groups] (the tab's own [currentGroupId]
 * marked), «New group» and, for a grouped tab, «Remove from group».
 */
@Composable
fun V5TabGroupMoveSheet(
    visible: Boolean,
    tabTitle: String,
    groups: List<V5OverviewGroup>,
    currentGroupId: String?,
    onMove: (groupId: String) -> Unit,
    onNewGroup: () -> Unit,
    onRemoveFromGroup: () -> Unit,
    onDismiss: () -> Unit,
    /**
     * Null while a new group can be made; else the most groups a profile holds. «New group» is then
     * off and says so before a name is typed, not with a lone vibration after «Save».
     */
    groupLimit: Int? = null,
) {
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = stringResource(R.string.v5_group_move_title)) {
        V5SheetHeader(
            title = stringResource(R.string.v5_group_move_title),
            subtitle = tabTitle,
            onClose = onDismiss,
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            if (groups.isNotEmpty()) {
                V5ListGroup {
                    groups.forEach { group ->
                        V5ListRow(
                            title = group.name,
                            icon = V5RowIcon(VolaIcons.TabGroup, group.tile),
                            trailing = if (group.id == currentGroupId) V5RowTrailing.None else V5RowTrailing.Chevron,
                            value = if (group.id == currentGroupId) stringResource(R.string.v5_group_current) else null,
                            enabled = group.id != currentGroupId,
                            onClick = {
                                onDismiss()
                                onMove(group.id)
                            },
                            modifier = Modifier.testTag(V5TabGroupSheetTestTags.moveTo(group.id)),
                        )
                    }
                }
            }
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.v5_group_new),
                    subtitle = groupLimit?.let { limit -> pluralStringResource(R.plurals.v5_group_limit, limit, limit) },
                    icon = V5RowIcon(VolaIcons.Add),
                    enabled = groupLimit == null,
                    onClick = onNewGroup,
                    modifier = Modifier.testTag(V5TabGroupSheetTestTags.NewGroup),
                )
                if (currentGroupId != null) {
                    V5ListRow(
                        title = stringResource(R.string.v5_group_remove_tab),
                        icon = V5RowIcon(VolaIcons.Remove),
                        onClick = {
                            onDismiss()
                            onRemoveFromGroup()
                        },
                        modifier = Modifier.testTag(V5TabGroupSheetTestTags.RemoveFromGroup),
                    )
                }
            }
        }
    }
}

/** A tab in a group's editor: its title, and its icon or its letter on a tile. */
@Immutable
data class V5GroupEditorTab(val id: String, val title: String, val favicon: ImageBitmap? = null)

/** What the group editor saves: the name, the color's index in the colors, the tabs in their order. */
@Immutable
data class V5GroupDraft(val name: String, val colorIndex: Int, val tabIds: List<String>)

/** The editor's two pages: the group itself, and the open tabs that can join it. */
private enum class GroupEditorPage { Main, AddTabs }

/**
 * A group's editor (canvas «GroupEditor»): «Cancel · title · Done» on top, the name, the color, and
 * for an existing group its [tabs] in their order (a handle drags a tab up or down, TalkBack moves it
 * with «Move earlier / later»), «Add open tabs» from [candidates], «Ungroup» and, in red, «Close the
 * group and its tabs» after a question. A new group ([onUngroup] null) has only the name and color.
 * Nothing changes until «Done»; [colors] are the data's colors as section tiles, in order.
 */
@Composable
fun V5TabGroupEditSheet(
    visible: Boolean,
    title: String,
    initialName: String,
    initialColorIndex: Int,
    colors: List<V5Tile>,
    onSave: (V5GroupDraft) -> Unit,
    onDismiss: () -> Unit,
    onUngroup: (() -> Unit)? = null,
    tabs: List<V5GroupEditorTab> = emptyList(),
    /** Open tabs that may join: outside every group, as private and as pinned as the group's tabs. */
    candidates: List<V5GroupEditorTab> = emptyList(),
    onCloseGroup: (() -> Unit)? = null,
) {
    // A fresh draft each time the sheet opens for a group; while it slides away after «Done» it keeps
    // what was saved instead of flashing back to the old name and color.
    val opening = rememberSheetOpening(visible)
    var name by rememberSaveable(opening) { mutableStateOf(initialName) }
    var colorIndex by rememberSaveable(opening) { mutableIntStateOf(initialColorIndex) }
    var tabIds by rememberSaveable(opening) { mutableStateOf(tabs.map(V5GroupEditorTab::id)) }
    var page by rememberSaveable(opening) { mutableStateOf(GroupEditorPage.Main) }
    var confirmClose by rememberSaveable(opening) { mutableStateOf(false) }
    val byId = (tabs + candidates).associateBy(V5GroupEditorTab::id)
    val save = {
        onDismiss()
        onSave(V5GroupDraft(name.trim().ifEmpty { initialName }, colorIndex, tabIds.filter(byId::containsKey)))
    }
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        // Back steps out of «Add open tabs» before it closes the sheet.
        BackHandler(enabled = page != GroupEditorPage.Main) { page = GroupEditorPage.Main }
        val animated = V5Motion.animationsEnabled
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (animated) {
                    fadeIn(V5Motion.effects.spec()) togetherWith fadeOut(V5Motion.effects.spec())
                } else {
                    fadeIn(V5Motion.reducedFade()) togetherWith fadeOut(V5Motion.reducedFade())
                }
            },
            label = "group editor page",
        ) { shown ->
            Column {
                if (shown == GroupEditorPage.AddTabs) {
                    AddTabsPage(
                        candidates = candidates,
                        chosen = tabIds,
                        onToggle = { id -> tabIds = if (id in tabIds) tabIds - id else tabIds + id },
                        onBack = { page = GroupEditorPage.Main },
                        onClose = onDismiss,
                    )
                    return@Column
                }
                V5EditHeader(
                    title = title,
                    cancel = stringResource(R.string.action_cancel),
                    onCancel = onDismiss,
                    done = stringResource(R.string.v5_group_save),
                    onDone = save,
                    doneTestTag = V5TabGroupSheetTestTags.Save,
                )
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = V5Spacing.space2, bottom = V5Spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
                ) {
                    V5NameField(
                        value = name,
                        placeholder = stringResource(R.string.v5_group_name),
                        onValueChange = { name = it },
                        onDone = save,
                        maxLength = V5_GROUP_NAME_MAX,
                        modifier = Modifier.testTag(V5TabGroupSheetTestTags.NameField),
                    )
                    V5SwatchRow(
                        title = stringResource(R.string.v5_group_color),
                        swatches = colors.map { V5Swatch(it.color, stringResource(it.groupColorName())) },
                        selected = colorIndex,
                        onSelect = { colorIndex = it },
                        testTag = V5TabGroupSheetTestTags::color,
                    )
                    if (onUngroup != null && tabIds.isNotEmpty()) {
                        GroupTabs(
                            tabIds = tabIds,
                            byId = byId,
                            canAdd = candidates.any { it.id !in tabIds },
                            onMove = { id, to -> tabIds = V5NewTabRules.moved(tabIds, id, to) },
                            onAdd = { page = GroupEditorPage.AddTabs },
                        )
                    }
                    if (onUngroup != null) {
                        V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                            V5ListRow(
                                title = stringResource(R.string.v5_group_ungroup),
                                onClick = {
                                    onDismiss()
                                    onUngroup()
                                },
                                modifier = Modifier.testTag(V5TabGroupSheetTestTags.Ungroup),
                            )
                            if (onCloseGroup != null) {
                                V5ListRow(
                                    title = pluralStringResource(R.plurals.v5_group_close, tabs.size, tabs.size),
                                    titleColor = V5Theme.colors.danger,
                                    onClick = { confirmClose = true },
                                    modifier = Modifier.testTag(V5TabGroupSheetTestTags.CloseGroup),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    // Closing tabs is not undone from here: the sheet asks first, in red (S-08).
    V5PromptSheet(
        visible = visible && confirmClose,
        title = pluralStringResource(R.plurals.v5_group_close, tabs.size, tabs.size),
        message = stringResource(R.string.v5_group_close_message),
        actions = listOf(
            V5PromptAction(
                label = stringResource(R.string.v5_group_close_action),
                role = V5PromptRole.Destructive,
                onClick = {
                    confirmClose = false
                    onDismiss()
                    onCloseGroup?.invoke()
                },
                testTag = V5TabGroupSheetTestTags.CloseGroupConfirm,
            ),
        ),
        onDismiss = { confirmClose = false },
    )
}

/** The group's tabs in their order; a handle drags one, TalkBack moves it one place at a time. */
@Composable
private fun GroupTabs(
    tabIds: List<String>,
    byId: Map<String, V5GroupEditorTab>,
    canAdd: Boolean,
    onMove: (id: String, toIndex: Int) -> Unit,
    onAdd: () -> Unit,
) {
    val view = LocalView.current
    val currentIds by rememberUpdatedState(tabIds)
    val move by rememberUpdatedState(onMove)
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val moveEarlier = stringResource(R.string.v5_newtab_move_earlier)
    val moveLater = stringResource(R.string.v5_newtab_move_later)
    V5ListGroup(header = stringResource(R.string.v5_group_tabs_header, tabIds.size)) {
        tabIds.forEachIndexed { index, id ->
            val tab = byId[id] ?: return@forEachIndexed
            val dragging = id == dragId
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = V5Sizes.rowSingleLine)
                    .onSizeChanged { rowHeight = it.height }
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                    .background(if (dragging) V5Theme.colors.surfaceHighest else Color.Transparent)
                    .semantics(mergeDescendants = true) {
                        customActions = buildList {
                            if (index > 0) add(CustomAccessibilityAction(moveEarlier) { move(id, index - 1); true })
                            if (index < tabIds.lastIndex) add(CustomAccessibilityAction(moveLater) { move(id, index + 1); true })
                        }
                    }
                    .padding(start = V5Spacing.space4)
                    .testTag(V5TabGroupSheetTestTags.tab(id)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
            ) {
                GroupTabTile(tab)
                Text(
                    tab.title,
                    style = V5Theme.type.callout,
                    color = V5Theme.colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(V5Sizes.touchTarget)
                        .pointerInput(id) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragId = id
                                    dragOffset = 0f
                                    view.performV5Haptic(V5Haptic.DragStart)
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount
                                    val step = rowHeight.toFloat()
                                    val at = currentIds.indexOf(id)
                                    if (step > 0f && dragOffset > step / 2 && at < currentIds.lastIndex) {
                                        move(id, at + 1)
                                        dragOffset -= step
                                        view.performV5Haptic(V5Haptic.SegmentTick)
                                    } else if (step > 0f && dragOffset < -step / 2 && at > 0) {
                                        move(id, at - 1)
                                        dragOffset += step
                                        view.performV5Haptic(V5Haptic.SegmentTick)
                                    }
                                },
                                onDragEnd = { dragId = null },
                                onDragCancel = { dragId = null },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        VolaIcons.DragHandle,
                        contentDescription = null,
                        tint = V5Theme.colors.labelSecondary,
                        modifier = Modifier.size(V5Sizes.iconSmall),
                    )
                }
            }
        }
        if (canAdd) {
            V5ListRow(
                title = stringResource(R.string.v5_group_add_tabs),
                icon = V5RowIcon(VolaIcons.Add),
                titleColor = V5Theme.colors.accent,
                onClick = onAdd,
                modifier = Modifier.testTag(V5TabGroupSheetTestTags.AddTabs),
            )
        }
    }
}

/** The open tabs that can join: a tap marks one to join on «Done», another tap unmarks it. */
@Composable
private fun AddTabsPage(
    candidates: List<V5GroupEditorTab>,
    chosen: List<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    V5SheetHeader(
        title = stringResource(R.string.v5_group_add_tabs),
        onClose = onClose,
        leading = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.heightIn(min = V5Sizes.touchTarget).testTag(V5TabGroupSheetTestTags.AddTabsBack),
            ) {
                Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = V5Theme.colors.label)
            }
        },
    )
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
    ) {
        V5ListGroup {
            candidates.forEach { tab ->
                V5ListRow(
                    title = tab.title,
                    leading = { GroupTabTile(tab) },
                    trailing = if (tab.id in chosen) V5RowTrailing.Check else V5RowTrailing.None,
                    onClick = { onToggle(tab.id) },
                    modifier = Modifier.testTag(V5TabGroupSheetTestTags.candidate(tab.id)),
                )
            }
        }
    }
}

/** A tab's icon, or the first letter of its title on a tile of its own color. */
@Composable
private fun GroupTabTile(tab: V5GroupEditorTab) {
    val favicon = tab.favicon
    Box(
        Modifier
            .size(V5Sizes.rowTile)
            .clip(V5Theme.shapes.xs)
            .background(if (favicon != null) V5Theme.colors.fill else V5NewTabRules.letterTile(tab.id).color),
        contentAlignment = Alignment.Center,
    ) {
        if (favicon != null) {
            Image(favicon, contentDescription = null, modifier = Modifier.size(V5Sizes.iconSmall))
        } else {
            Text(
                V5NewTabRules.letter(tab.title),
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = V5NewTabRules.letterTile(tab.id).glyph,
            )
        }
    }
}

/** A group color's name for TalkBack. */
private fun V5Tile.groupColorName(): Int = when (this) {
    V5Tile.Orange -> R.string.v5_group_color_orange
    V5Tile.Teal -> R.string.v5_group_color_teal
    V5Tile.Purple -> R.string.v5_group_color_purple
    V5Tile.Blue -> R.string.v5_group_color_blue
    V5Tile.Pink -> R.string.v5_group_color_pink
    V5Tile.Green -> R.string.v5_group_color_green
    V5Tile.Gray -> R.string.v5_group_color_gray
    else -> R.string.v5_group_color
}

/** A group's name fits the data's limit (TabStackRules.MAX_NAME_LENGTH). */
internal const val V5_GROUP_NAME_MAX = 40
