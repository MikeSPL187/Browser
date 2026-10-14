package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

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
) {
    V5Sheet(visible = visible, onDismiss = onDismiss, title = stringResource(R.string.v5_group_move_title)) {
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
                    icon = V5RowIcon(VolaIcons.Add),
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

/**
 * A group's name and color: for a new group ([onUngroup] null) or an existing one, which can also be
 * ungrouped. [colors] are the data's colors as section tiles, in order; the result is an index.
 */
@Composable
fun V5TabGroupEditSheet(
    visible: Boolean,
    title: String,
    initialName: String,
    initialColorIndex: Int,
    colors: List<V5Tile>,
    onSave: (name: String, colorIndex: Int) -> Unit,
    onDismiss: () -> Unit,
    onUngroup: (() -> Unit)? = null,
) {
    // A fresh draft each time the sheet opens for a group.
    var name by rememberSaveable(visible, initialName) { mutableStateOf(initialName) }
    var colorIndex by rememberSaveable(visible, initialColorIndex) { mutableIntStateOf(initialColorIndex) }
    val save = {
        onDismiss()
        onSave(name.trim().ifEmpty { initialName }, colorIndex)
    }
    V5Sheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
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
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.v5_group_save),
                    titleColor = V5Theme.colors.accent,
                    onClick = save,
                    modifier = Modifier.testTag(V5TabGroupSheetTestTags.Save),
                )
                if (onUngroup != null) {
                    V5ListRow(
                        title = stringResource(R.string.v5_group_ungroup),
                        titleColor = V5Theme.colors.danger,
                        onClick = {
                            onDismiss()
                            onUngroup()
                        },
                        modifier = Modifier.testTag(V5TabGroupSheetTestTags.Ungroup),
                    )
                }
            }
        }
    }
}

/** A group color's name for TalkBack. */
private fun V5Tile.groupColorName(): Int = when (this) {
    V5Tile.Purple -> R.string.v5_group_color_purple
    V5Tile.Red -> R.string.v5_group_color_red
    V5Tile.Green -> R.string.v5_group_color_green
    V5Tile.Blue -> R.string.v5_group_color_blue
    else -> R.string.v5_group_color
}

/** A group's name fits the data's limit (TabStackRules.MAX_NAME_LENGTH). */
internal const val V5_GROUP_NAME_MAX = 40
