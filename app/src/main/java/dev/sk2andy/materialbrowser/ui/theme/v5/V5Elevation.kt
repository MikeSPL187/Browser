package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset

/**
 * Lifts what follows to [level] (system-v5.md 5, table «Глубина»): the level's shadows of the current
 * theme under [shape], and in the dark theme its hairline outline instead where it has one. Put it
 * before the element's clip and background, so the shadow falls outside them.
 */
@Composable
fun Modifier.v5Elevation(level: V5ElevationLevel, shape: Shape): Modifier {
    val lift = level.forTheme(V5Theme.colors.isDark)
    val shadowed = lift.shadows.fold(this) { modifier, shadow ->
        modifier.dropShadow(
            shape = shape,
            shadow = Shadow(radius = shadow.blur, color = shadow.color, offset = DpOffset.Zero.copy(y = shadow.y)),
        )
    }
    return lift.outline?.let { outline -> shadowed.border(V5Spacing.hairline, outline, shape) } ?: shadowed
}

/** Puts [V5Glow] in [color] under what follows, shaped by [shape]; before the clip, like [v5Elevation]. */
fun Modifier.v5Glow(color: Color, shape: Shape): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(radius = V5Glow.blur, color = color.copy(alpha = V5Glow.ALPHA), offset = DpOffset.Zero.copy(y = V5Glow.y)),
)
