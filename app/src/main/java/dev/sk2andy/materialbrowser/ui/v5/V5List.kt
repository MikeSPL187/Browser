package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Elevation

/*
 * Grouped lists of the v5 menu and settings (system-v5.md, sections 1.5 and 3.4; canvas screens «д»
 * and «ж»): rounded groups on the screen background, rows with a section tile or a plain glyph, a
 * value on the right and a chevron or a toggle. Dividers start under the text, as in Safari.
 */

/** What a row ends with. */
@Immutable
sealed interface V5RowTrailing {
    data object None : V5RowTrailing

    /** Opens a page or a sheet. */
    data object Chevron : V5RowTrailing

    /** The chosen one of a list, in the accent. */
    data object Check : V5RowTrailing

    /** The whole row is the switch. */
    data class Toggle(
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val tint: V5ToggleTint = V5ToggleTint.Accent,
    ) : V5RowTrailing
}

/** The row's leading glyph: a white glyph on a section tile (settings) or a plain glyph (the menu). */
@Immutable
data class V5RowIcon(val icon: ImageVector, val tile: V5Tile? = null)

/**
 * One group: an optional [header] in footnote over a rounded card of rows ([content]), an optional
 * [footer] under it. Dividers between the rows start at [dividerInset], under the text after a tile;
 * a row that is not emitted leaves no divider behind.
 */
@Composable
fun V5ListGroup(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    dividerInset: Dp = V5ListDefaults.dividerInsetWithIcon,
    content: @Composable () -> Unit,
) {
    val colors = V5Theme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = V5Spacing.marginPhone)) {
        if (header != null) {
            Text(
                header,
                style = V5Theme.type.footnote,
                color = colors.labelSecondary,
                modifier = Modifier
                    .padding(start = V5Spacing.space4, end = V5Spacing.space4, bottom = V5Spacing.space2)
                    .semantics { heading() },
            )
        }
        DividedColumn(
            dividerInset = dividerInset,
            dividerColor = colors.separator,
            modifier = Modifier
                .fillMaxWidth()
                // A group of rows is a card (canvas «Settings», «Shield»): e1 under it.
                .v5Elevation(V5Elevation.e1, V5Theme.shapes.s)
                .clip(V5Theme.shapes.s)
                .background(colors.surface),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                style = V5Theme.type.footnote,
                color = colors.labelSecondary,
                modifier = Modifier.padding(start = V5Spacing.space4, end = V5Spacing.space4, top = V5Spacing.space2),
            )
        }
    }
}

/** Where the dividers go, written by the layout and read when drawing the same frame. */
private class DividerStops {
    var bottoms: IntArray = IntArray(0)
}

/** Rows stacked top to bottom with a hairline between each two, from [dividerInset] at the start. */
@Composable
private fun DividedColumn(
    dividerInset: Dp,
    dividerColor: Color,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val stops = remember { DividerStops() }
    Layout(
        content = content,
        modifier = modifier.drawWithContent {
            drawContent()
            val hairline = V5Spacing.hairline.toPx()
            val inset = dividerInset.toPx()
            val start = if (layoutDirection == LayoutDirection.Ltr) inset else 0f
            stops.bottoms.forEach { bottom ->
                drawRect(
                    color = dividerColor,
                    topLeft = Offset(start, bottom - hairline),
                    size = Size((size.width - inset).coerceAtLeast(0f), hairline),
                )
            }
        },
    ) { measurables, constraints ->
        val rowConstraints = constraints.copy(minHeight = 0)
        val placeables = measurables.map { it.measure(rowConstraints) }
        val height = placeables.sumOf { it.height }
        layout(constraints.maxWidth, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            val bottoms = IntArray((placeables.size - 1).coerceAtLeast(0))
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height
                if (index < bottoms.size) bottoms[index] = y
            }
            stops.bottoms = bottoms
        }
    }
}

object V5ListDefaults {
    /** Dividers under rows with a tile or glyph start under the text. */
    val dividerInsetWithIcon: Dp get() = V5Spacing.space4 + V5Sizes.rowTile + V5Spacing.space3

    /** Dividers under text-only rows start at the text. */
    val dividerInsetText: Dp get() = V5Spacing.space4
}

/**
 * A list row: [icon], [title] over an optional [subtitle], an optional [value] and the [trailing]
 * chevron or toggle. One line is at least 52 dp, with a subtitle 64 dp (section 3.4). It presses
 * in to 0.985 on the press spring; with a toggle the whole row switches.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun V5ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: V5RowIcon? = null,
    /** In place of [icon]: a leading of the row's own, such as a profile's emoji on its color. */
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    trailing: V5RowTrailing = V5RowTrailing.None,
    titleColor: Color = V5Theme.colors.label,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    /** A second action on long press, announced to TalkBack with [onLongClickLabel]. */
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    val colors = V5Theme.colors
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && V5Motion.animationsEnabled) V5Motion.PRESS_SCALE_CARD else 1f,
        animationSpec = V5Motion.press.spec(),
        label = "v5 row press",
    )
    val action = when {
        trailing is V5RowTrailing.Toggle -> Modifier.toggleable(
            value = trailing.checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interaction,
            indication = null,
        ) { on ->
            view.performV5Haptic(if (on) V5Haptic.ToggleOn else V5Haptic.ToggleOff)
            trailing.onCheckedChange(on)
        }
        onClick != null && onLongClick != null -> Modifier.combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        )
        onClick != null -> Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
        else -> Modifier
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle != null) V5Sizes.rowTwoLine else V5Sizes.rowSingleLine)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else V5_DISABLED_ALPHA
            }
            .then(action)
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        if (icon != null) RowIcon(icon) else leading?.invoke()
        // Long names wrap, as Settings' do at a large font, up to a few lines; past that they end in «…» (L-02).
        Column(Modifier.weight(1f)) {
            Text(title, style = V5Theme.type.body, color = titleColor, maxLines = TITLE_MAX_LINES, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                    maxLines = SUBTITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Text(
                value,
                style = V5Theme.type.body,
                color = colors.labelSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when (trailing) {
            V5RowTrailing.None -> Unit
            V5RowTrailing.Chevron -> Icon(
                VolaIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.labelTertiary,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
            V5RowTrailing.Check -> Icon(
                VolaIcons.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
            is V5RowTrailing.Toggle -> V5Toggle(
                checked = trailing.checked,
                onCheckedChange = null,
                enabled = enabled,
                tint = trailing.tint,
            )
        }
    }
}

@Composable
private fun RowIcon(icon: V5RowIcon) {
    val tile = icon.tile
    if (tile == null) {
        Box(Modifier.size(V5Sizes.rowTile), contentAlignment = Alignment.Center) {
            Icon(icon.icon, contentDescription = null, tint = V5Theme.colors.label, modifier = Modifier.size(V5Sizes.icon))
        }
    } else {
        Box(
            Modifier
                .size(V5Sizes.rowTile)
                .clip(V5Theme.shapes.xs)
                .background(tile.color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon.icon, contentDescription = null, tint = tile.glyph, modifier = Modifier.size(V5Sizes.iconSmall))
        }
    }
}

/** A row's title wraps up to this many lines before it is cut. */
private const val TITLE_MAX_LINES = 3

/** A row's second line, which explains, gets a little more. */
private const val SUBTITLE_MAX_LINES = 4
