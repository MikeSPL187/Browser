package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.DistinctNames
import dev.sk2andy.materialbrowser.browser.TabStack
import dev.sk2andy.materialbrowser.browser.TabStackColor
import dev.sk2andy.materialbrowser.data.TabStackRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.v5.V5GroupEditorTab
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewGroup
import dev.sk2andy.materialbrowser.ui.v5.V5TabGroupEditSheet
import dev.sk2andy.materialbrowser.ui.v5.V5TabGroupMoveSheet

/** Which tab group sheet the v5 overview shows (V12b). */
internal sealed interface TabGroupSheetTarget {
    /** «Move to group» for a long-pressed card. */
    data class Move(val tabId: String) : TabGroupSheetTarget

    /** A new group for the tab, from «Move to group». */
    data class New(val tabId: String) : TabGroupSheetTarget

    /** A long-pressed group: name, color, «Ungroup». */
    data class Edit(val groupId: String) : TabGroupSheetTarget
}

/**
 * The v5 tab group sheets bound to the profile's TabStack data: a tab moves into a group the rules
 * allow (same privacy and pinning), into a new group or out of its own; a group is renamed,
 * recolored or ungrouped, never to another group's name. A change the data refuses (twelve groups already) ticks Reject.
 */
@Composable
internal fun BrowserTabGroupSheetsV5(
    controller: BrowserController,
    target: TabGroupSheetTarget?,
    onTargetChange: (TabGroupSheetTarget?) -> Unit,
) {
    val view = LocalView.current
    val stacks = controller.tabStackController
    // Each sheet keeps its last target, and the edited group as it was, while it slides away.
    val last = remember { LastTargets() }
    when (target) {
        is TabGroupSheetTarget.Move -> last.move = target
        is TabGroupSheetTarget.New -> last.new = target
        is TabGroupSheetTarget.Edit -> stacks.activeStacks.firstOrNull { it.id == target.groupId }?.let { last.edit = it }
        null -> Unit
    }
    val dismiss = { onTargetChange(null) }
    val refuseUnless = { done: Boolean -> if (!done) view.performV5Haptic(V5Haptic.Reject) }
    val colors = TabStackColor.entries
    val tiles = colors.map { it.v5Tile() }
    val defaultName = stringResource(R.string.v5_group_default_name)

    val move = last.move
    val movingTab = move?.let { m -> controller.activeTabs.firstOrNull { it.id == m.tabId } }
    val tabsById = controller.activeTabs.associateBy { it.id }
    V5TabGroupMoveSheet(
        visible = target is TabGroupSheetTarget.Move && movingTab != null,
        tabTitle = if (movingTab != null) displayTabTitle(movingTab) else "",
        groups = stacks.activeStacks
            .filter { stack ->
                val member = stack.tabIds.firstNotNullOfOrNull(tabsById::get)
                movingTab != null && member != null &&
                    member.isIncognito == movingTab.isIncognito && member.isPinned == movingTab.isPinned
            }
            .map { it.v5Group() },
        currentGroupId = movingTab?.let { stacks.stackFor(it.id)?.id },
        onMove = { groupId -> move?.let { refuseUnless(stacks.addTab(it.tabId, groupId)) } },
        onNewGroup = { move?.let { onTargetChange(TabGroupSheetTarget.New(it.tabId)) } },
        onRemoveFromGroup = { move?.let { refuseUnless(stacks.removeTab(it.tabId)) } },
        onDismiss = dismiss,
        groupLimit = TabStackRules.MAX_STACKS.takeIf { stacks.activeStacks.size >= it },
    )

    val new = last.new
    V5TabGroupEditSheet(
        visible = target is TabGroupSheetTarget.New,
        title = stringResource(R.string.v5_group_new),
        initialName = "",
        // A new group takes the next color, so neighbours differ.
        initialColorIndex = stacks.activeStacks.size % colors.size,
        colors = tiles,
        onSave = { draft ->
            new?.let {
                val taken = stacks.activeStacks.map(TabStack::name)
                val groupName = distinctGroupName(draft.name.ifBlank { defaultName }, taken)
                val created = stacks.create(listOf(it.tabId), groupName, colors[draft.colorIndex])
                refuseUnless(created != null)
            }
        },
        onDismiss = dismiss,
    )

    val edit = last.edit
    // The group's tabs in its order, and the open tabs that may join it: outside every group, and as
    // private and as pinned as its tabs, as «Move to group» offers.
    val editTabs = edit?.tabIds?.mapNotNull(tabsById::get).orEmpty()
    val editCandidates = editTabs.firstOrNull()?.let { member ->
        controller.activeTabs.filter { tab ->
            stacks.stackFor(tab.id) == null &&
                tab.isIncognito == member.isIncognito && tab.isPinned == member.isPinned
        }
    }.orEmpty()
    V5TabGroupEditSheet(
        visible = target is TabGroupSheetTarget.Edit && stacks.activeStacks.any { it.id == target.groupId },
        title = stringResource(R.string.v5_group_edit_title),
        initialName = edit?.name.orEmpty(),
        initialColorIndex = edit?.let { colors.indexOf(it.color) }?.coerceAtLeast(0) ?: 0,
        colors = tiles,
        onSave = { draft ->
            edit?.let { group ->
                val others = stacks.activeStacks.filter { it.id != group.id }.map(TabStack::name)
                val newName = distinctGroupName(draft.name.ifBlank { group.name }, others)
                val color = colors[draft.colorIndex]
                if (draft.tabIds.isNotEmpty() && draft.tabIds != group.tabIds) {
                    // New members and a new order go in one change with the name and color.
                    refuseUnless(stacks.update(group.id, draft.tabIds, newName, color, group.previewTabId))
                } else {
                    if (newName != group.name) stacks.rename(group.id, newName)
                    if (color != group.color) stacks.setColor(group.id, color)
                }
            }
        },
        onDismiss = dismiss,
        onUngroup = { edit?.let { refuseUnless(stacks.ungroup(it.id)) } },
        tabs = editTabs.map { it.v5EditorTab(controller) },
        candidates = editCandidates.map { it.v5EditorTab(controller) },
        onCloseGroup = { edit?.let { group -> group.tabIds.forEach(controller::closeTab) } },
    )
}

/** A tab as the group editor lists it: its title and its icon. */
@Composable
private fun BrowserTab.v5EditorTab(controller: BrowserController): V5GroupEditorTab =
    V5GroupEditorTab(id = id, title = displayTabTitle(this), favicon = controller.favicons[id]?.asImageBitmap())

/** A profile's groups never share a name: «Group», «Group 2» (TG-02). */
private fun distinctGroupName(name: String, taken: List<String>): String =
    DistinctNames.distinct(TabStackRules.normalizedName(name), taken, TabStackRules.MAX_NAME_LENGTH)

/** Not state: a new target recomposes anyway, and an old one only outlives it while its sheet hides. */
private class LastTargets {
    var move: TabGroupSheetTarget.Move? = null
    var new: TabGroupSheetTarget.New? = null
    var edit: TabStack? = null
}

/** A profile's group as the v5 overview draws it. */
internal fun TabStack.v5Group(): V5OverviewGroup = V5OverviewGroup(
    id = id,
    name = name,
    tabIds = tabIds,
    collapsed = isCollapsed,
    tile = color.v5Tile(),
)

/** A group's color as a v5 section tile: the data keeps four colors, v5 draws them as tiles. */
internal fun TabStackColor.v5Tile(): V5Tile = when (this) {
    TabStackColor.Grape -> V5Tile.Purple
    TabStackColor.Cherry -> V5Tile.Red
    TabStackColor.Lime -> V5Tile.Green
    TabStackColor.Blueberry -> V5Tile.Blue
}
