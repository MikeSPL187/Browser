package dev.sk2andy.materialbrowser.ui.v5

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic

/*
 * Tab groups in the v5 overview (V12, canvas screen «г», system-v5.md section 10): a group's color
 * is only its dot and the frame around the group (V5GroupFrames), never a fill. An open group is a header over its
 * cards; a folded one is a single framed row with a fan of its pages.
 */

object V5TabGroupTestTags {
    fun header(id: String) = "v5_group_header:$id"
    fun collapsed(id: String) = "v5_group_collapsed:$id"
}

/** An open group: its dot, name and tab count; a tap folds it, a long press edits it. */
@Composable
internal fun GroupHeader(
    group: V5OverviewGroup,
    count: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = V5Theme.colors
    val description = pluralStringResource(R.plurals.v5_overview_group_description, count, group.name, count)
    val action = stringResource(R.string.v5_overview_group_collapse)
    val edit = stringResource(R.string.v5_group_edit_title)
    val view = LocalView.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.s)
            .groupClickable(view, onToggle, onLongPress)
            .testTag(V5TabGroupTestTags.header(group.id))
            .clearAndSetSemantics {
                contentDescription = description
                heading()
                role = Role.Button
                onClick(label = action) { onToggle(); true }
                if (onLongPress != null) onLongClick(label = edit) { onLongPress(); true }
            }
            .padding(horizontal = V5Spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        GroupDot(group)
        // The name reads as the title of what the frame holds (canvas «OverviewV2»).
        Text(
            group.name,
            style = V5Theme.type.callout.copy(fontWeight = FontWeight.SemiBold),
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(count.toString(), style = V5Theme.type.callout, color = colors.labelSecondary)
        Box(Modifier.weight(1f))
        Icon(VolaIcons.KeyboardArrowUp, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
    }
}

/** A folded group: one row framed in the group's color, with a fan of up to three of its pages. */
@Composable
internal fun CollapsedGroup(
    group: V5OverviewGroup,
    count: Int,
    previews: List<V5TabCard>,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.m
    val description = pluralStringResource(R.plurals.v5_overview_group_description, count, group.name, count)
    val action = stringResource(R.string.v5_overview_group_expand)
    val edit = stringResource(R.string.v5_group_edit_title)
    val view = LocalView.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowTwoLine)
            .clip(shape)
            .background(colors.surface)
            .border(V5Sizes.groupBorder, group.tile.color, shape)
            .groupClickable(view, onToggle, onLongPress)
            .testTag(V5TabGroupTestTags.collapsed(group.id))
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = action) { onToggle(); true }
                if (onLongPress != null) onLongClick(label = edit) { onLongPress(); true }
            }
            .padding(horizontal = V5Spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        GroupDot(group)
        Text(
            group.name,
            style = V5Theme.type.headline,
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(count.toString(), style = V5Theme.type.body, color = colors.labelSecondary)
        Box(Modifier.weight(1f))
        PreviewFan(previews)
        Icon(VolaIcons.KeyboardArrowDown, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
    }
}

/** «No group» above the tabs outside every group. */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = V5Theme.type.footnote,
        color = V5Theme.colors.labelSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.space1, vertical = V5Spacing.space2)
            .semantics { heading() },
    )
}

/** A tap folds or unfolds the group; a long press, where given, edits it with the long-press tick. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.groupClickable(view: View, onToggle: () -> Unit, onLongPress: (() -> Unit)?): Modifier =
    combinedClickable(
        onClick = onToggle,
        onLongClick = onLongPress?.let { press ->
            {
                view.performV5Haptic(V5Haptic.LongPress)
                press()
            }
        },
    )

@Composable
private fun GroupDot(group: V5OverviewGroup) {
    Box(
        Modifier
            .size(V5Sizes.dot)
            .clip(V5Theme.shapes.full)
            .background(group.tile.color),
    )
}

/** The group's pages overlapping right to left, each in a small rounded thumbnail. */
@Composable
private fun PreviewFan(previews: List<V5TabCard>) {
    if (previews.isEmpty()) return
    val colors = V5Theme.colors
    val step = V5Sizes.groupPreview - V5Sizes.groupPreviewOverlap
    Box(Modifier.size(width = V5Sizes.groupPreview + step * (previews.size - 1), height = V5Sizes.groupPreview)) {
        previews.forEachIndexed { index, card ->
            Box(
                Modifier
                    .offset(x = step * index)
                    .size(V5Sizes.groupPreview)
                    .clip(V5Theme.shapes.xs)
                    .background(colors.surfaceHighest)
                    .border(V5Spacing.hairline, colors.edge, V5Theme.shapes.xs),
                contentAlignment = Alignment.Center,
            ) {
                val image = card.preview ?: card.favicon
                if (image != null) {
                    Image(
                        image,
                        contentDescription = null,
                        contentScale = if (card.preview != null) ContentScale.Crop else ContentScale.Fit,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.size(if (card.preview != null) V5Sizes.groupPreview else V5Sizes.overviewFavicon),
                    )
                }
            }
        }
    }
}
