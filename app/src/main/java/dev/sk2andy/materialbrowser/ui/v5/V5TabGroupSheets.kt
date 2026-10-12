package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
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
            NameField(name, onNameChange = { name = it }, onDone = save)
            ColorRow(colors, colorIndex, onSelect = { colorIndex = it })
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

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit, onDone: () -> Unit) {
    val colors = V5Theme.colors
    val label = stringResource(R.string.v5_group_name)
    Box(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowSingleLine)
            .clip(V5Theme.shapes.s)
            .background(colors.surface)
            .padding(horizontal = V5Spacing.space4),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (name.isEmpty()) {
            Text(label, style = V5Theme.type.body, color = colors.labelTertiary)
        }
        BasicTextField(
            value = name,
            onValueChange = { onNameChange(it.take(V5_GROUP_NAME_MAX)) },
            singleLine = true,
            textStyle = V5Theme.type.body.copy(color = colors.label),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label }
                .testTag(V5TabGroupSheetTestTags.NameField),
        )
    }
}

/** The group colors as round swatches; the chosen one is ringed in the label color. */
@Composable
private fun ColorRow(colors: List<V5Tile>, selected: Int, onSelect: (Int) -> Unit) {
    val names = colors.map { stringResource(it.groupColorName()) }
    Column(Modifier.padding(horizontal = V5Spacing.marginPhone)) {
        Text(
            stringResource(R.string.v5_group_color),
            style = V5Theme.type.footnote,
            color = V5Theme.colors.labelSecondary,
            modifier = Modifier.padding(start = V5Spacing.space4, bottom = V5Spacing.space2),
        )
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            colors.forEachIndexed { index, tile ->
                val chosen = index == selected
                Box(
                    Modifier
                        .size(V5Sizes.touchTarget)
                        .clip(V5Theme.shapes.full)
                        .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(index) })
                        .semantics { contentDescription = names[index] }
                        .testTag(V5TabGroupSheetTestTags.color(index)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(V5Sizes.suggestionTile)
                            .then(
                                if (chosen) {
                                    Modifier.border(V5Sizes.groupBorder, V5Theme.colors.label, V5Theme.shapes.full)
                                } else {
                                    Modifier
                                },
                            )
                            .padding(V5Sizes.groupBorder * 2)
                            .clip(V5Theme.shapes.full)
                            .background(tile.color),
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
