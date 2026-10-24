package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5NewTabFreshTestTags {
    const val Title = "v5_fresh_title"
    const val Copy = "v5_fresh_copy"
    const val Move = "v5_fresh_move"
    const val MoveEmpty = "v5_fresh_move_empty"
    fun movable(id: String) = "v5_fresh_movable:$id"
}

/**
 * A profile with nothing of its own yet (canvas screen «EmptyStart»): the new tab says so and offers
 * a quicker start. [copyFrom] names the profile whose bookmarks can be copied, null when none has any.
 */
@Immutable
data class V5NewTabFresh(val profileName: String, val copyFrom: String?, val canMoveTabs: Boolean)

/** Open tabs of one other profile, offered to move here. */
@Immutable
data class V5MovableTabs(val profileName: String, val tabs: List<V5RecentTab>)

/** «A clean slate» and why: the profile keeps its own cookies and history. */
@Composable
internal fun V5FreshTitle(fresh: V5NewTabFresh) {
    val colors = V5Theme.colors
    Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
        Text(
            stringResource(R.string.v5_fresh_title),
            style = V5Theme.type.largeTitle,
            color = colors.label,
            modifier = Modifier.semantics { heading() }.testTag(V5NewTabFreshTestTags.Title),
        )
        Text(
            stringResource(R.string.v5_fresh_message, fresh.profileName),
            style = V5Theme.type.body,
            color = colors.labelSecondary,
        )
    }
}

/** An empty pinned slot: a dashed outline with a bar where the name will be. */
@Composable
internal fun V5FreshSlot(modifier: Modifier) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.m
    Column(
        modifier
            .padding(vertical = V5Spacing.space1)
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Box(
            Modifier
                .size(V5Sizes.pinnedTile)
                .drawBehind {
                    val stroke = V5Sizes.freshSlotBorder.toPx()
                    val dash = V5Sizes.freshSlotDash.toPx()
                    drawOutline(
                        outline = shape.createOutline(size, layoutDirection, this),
                        color = colors.separator,
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                    )
                },
        )
        Box(
            Modifier
                .width(V5Sizes.freshSlotName)
                .height(V5Sizes.dot)
                .clip(V5Theme.shapes.full)
                .background(colors.fill),
        )
    }
}

/** «Get started faster»: copy another profile's bookmarks, move tabs here. */
@Composable
internal fun V5FreshCard(fresh: V5NewTabFresh, onCopy: () -> Unit, onMove: () -> Unit) {
    val colors = V5Theme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(V5Theme.shapes.l)
            .background(colors.surface)
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Text(
            stringResource(R.string.v5_fresh_card_title),
            style = V5Theme.type.headline,
            color = colors.label,
            modifier = Modifier.semantics { heading() },
        )
        fresh.copyFrom?.let { source ->
            FreshAction(
                icon = VolaIcons.Bookmark,
                label = stringResource(R.string.v5_fresh_copy, source),
                onClick = onCopy,
                tag = V5NewTabFreshTestTags.Copy,
            )
        }
        if (fresh.canMoveTabs) {
            FreshAction(
                icon = VolaIcons.ArrowForward,
                label = stringResource(R.string.v5_fresh_move),
                onClick = onMove,
                tag = V5NewTabFreshTestTags.Move,
            )
        }
    }
}

@Composable
private fun FreshAction(icon: ImageVector, label: String, onClick: () -> Unit, tag: String) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowSingleLine)
            .clip(V5Theme.shapes.m)
            .background(colors.fill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.iconSmall))
        Text(label, style = V5Theme.type.callout, color = colors.label, modifier = Modifier.weight(1f))
    }
}

/**
 * «Move tabs here»: the open tabs of the other profiles, under their profile's name. A tap moves the
 * tab and keeps the sheet open for the next; once none is left the sheet says so.
 */
@Composable
fun V5MoveTabsSheet(
    visible: Boolean,
    groups: List<V5MovableTabs>,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = V5Theme.colors
    val title = stringResource(R.string.v5_fresh_move_title)
    val moveLabel = stringResource(R.string.v5_fresh_move_action)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = V5Spacing.marginPhone)
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            if (groups.none { it.tabs.isNotEmpty() }) {
                Text(
                    stringResource(R.string.v5_fresh_move_empty),
                    style = V5Theme.type.callout,
                    color = colors.labelSecondary,
                    modifier = Modifier
                        .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3)
                        .testTag(V5NewTabFreshTestTags.MoveEmpty),
                )
                V5StatePrimaryAction(V5StateAction(label = stringResource(R.string.v5_fresh_move_done), onClick = onDismiss))
            }
            groups.filter { it.tabs.isNotEmpty() }.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                    Text(
                        group.profileName,
                        style = V5Theme.type.footnote,
                        color = colors.labelSecondary,
                        modifier = Modifier
                            .padding(horizontal = V5Spacing.space4)
                            .semantics { heading() },
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(V5Theme.shapes.m)
                            .background(colors.surface),
                    ) {
                        group.tabs.forEach { tab -> MovableRow(tab, moveLabel, onMove) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovableRow(tab: V5RecentTab, moveLabel: String, onMove: (String) -> Unit) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowTwoLine)
            .clickable(role = Role.Button, onClickLabel = moveLabel, onClick = { onMove(tab.id) })
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2)
            .testTag(V5NewTabFreshTestTags.movable(tab.id)),
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
        Icon(VolaIcons.ArrowForward, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.iconSmall))
    }
}
