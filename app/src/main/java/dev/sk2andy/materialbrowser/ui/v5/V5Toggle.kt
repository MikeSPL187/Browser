package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import dev.sk2andy.materialbrowser.ui.theme.v5.V5ControlColors
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic

/** What an «on» toggle shows: the profile accent (settings) or success green (protection switches). */
enum class V5ToggleTint { Accent, Success }

/** Disabled controls keep this much of their opacity. */
internal const val V5_DISABLED_ALPHA = 0.38f

/**
 * The v5 capsule toggle (system-v5.md, section 3.4; canvas `.sw`): 51 × 31 with a white thumb. With
 * [onCheckedChange] it is its own switch with a 48 dp target and the toggle haptics; without it only
 * draws, for a row that is the switch itself ([V5ListRow] with [V5RowTrailing.Toggle]).
 */
@Composable
fun V5Toggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: V5ToggleTint = V5ToggleTint.Accent,
) {
    val view = LocalView.current
    val interactive = if (onCheckedChange != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) { on ->
                view.performV5Haptic(if (on) V5Haptic.ToggleOn else V5Haptic.ToggleOff)
                onCheckedChange(on)
            }
    } else {
        Modifier
    }
    Box(modifier.then(interactive), contentAlignment = Alignment.Center) {
        V5ToggleTrack(checked, enabled, tint)
    }
}

@Composable
private fun V5ToggleTrack(checked: Boolean, enabled: Boolean, tint: V5ToggleTint) {
    val colors = V5Theme.colors
    val on = when (tint) {
        V5ToggleTint.Accent -> colors.accentFill
        V5ToggleTint.Success -> colors.success
    }
    val moves = V5Motion.animationsEnabled
    val track by animateColorAsState(
        targetValue = if (checked) on else colors.surfaceHighest,
        animationSpec = if (moves) V5Motion.effects.spec() else V5Motion.reducedFade(),
        label = "v5 toggle track",
    )
    val travel = V5Sizes.toggleWidth - V5Sizes.toggleThumb - V5Sizes.toggleThumbInset * 2
    val thumbTravel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (moves) V5Motion.press.spec() else V5Motion.reducedFade(),
        label = "v5 toggle thumb",
    )
    Box(
        Modifier
            .size(V5Sizes.toggleWidth, V5Sizes.toggleHeight)
            .graphicsLayer { alpha = if (enabled) 1f else V5_DISABLED_ALPHA }
            .clip(V5Theme.shapes.full)
            .background(track)
            .padding(V5Sizes.toggleThumbInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = travel * thumbTravel)
                .size(V5Sizes.toggleThumb)
                .shadow(
                    elevation = V5Sizes.toggleThumbInset,
                    shape = V5Theme.shapes.full,
                    ambientColor = V5ControlColors.toggleThumbShadow,
                    spotColor = V5ControlColors.toggleThumbShadow,
                )
                .background(V5ControlColors.toggleThumb, V5Theme.shapes.full),
        )
    }
}
