package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import dev.sk2andy.materialbrowser.ui.theme.v5.V5ControlColors
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import kotlin.math.roundToInt

/** The pure part of the v5 slider: where the fill ends and which stop the value is on. */
object V5SliderRules {
    fun fraction(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val span = range.endInclusive - range.start
        return if (span <= 0f) 0f else ((value - range.start) / span).coerceIn(0f, 1f)
    }

    /** The stop [value] is on, counting both ends; [steps] are the stops between them, as Material counts. */
    fun stop(value: Float, range: ClosedFloatingPointRange<Float>, steps: Int): Int =
        (fraction(value, range) * (steps + 1)).roundToInt()
}

/**
 * The v5 slider (canvas «ц3»): a 4 dp track filled in the accent up to the value and the toggle's
 * white 28 dp knob, as on iOS. Each stop gives a light tick. It is Material's slider underneath,
 * so dragging, tapping the track, TalkBack and the volume keys move it as before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V5Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val lastStop = remember { intArrayOf(V5SliderRules.stop(value, valueRange, steps)) }
    val fraction = V5SliderRules.fraction(value, valueRange)
    val state = remember(steps, valueRange) { SliderState(value, steps, valueRange) }
    state.value = value
    Slider(
        state = state,
        onValueChange = { candidate: Float ->
            if (steps > 0) {
                val stop = V5SliderRules.stop(candidate, valueRange, steps)
                if (stop != lastStop[0]) {
                    lastStop[0] = stop
                    view.performV5Haptic(V5Haptic.SegmentTick)
                }
            }
            onValueChange(candidate)
        },
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        thumb = { _ -> V5SliderThumb(enabled) },
        track = { _ -> V5SliderTrack(fraction, enabled) },
    )
}

@Composable
private fun V5SliderThumb(enabled: Boolean) {
    Box(
        Modifier
            .size(V5Sizes.sliderThumb)
            .graphicsLayer { alpha = if (enabled) 1f else V5_DISABLED_ALPHA }
            .shadow(
                elevation = V5Sizes.toggleThumbInset,
                shape = V5Theme.shapes.full,
                ambientColor = V5ControlColors.toggleThumbShadow,
                spotColor = V5ControlColors.toggleThumbShadow,
            )
            .background(V5ControlColors.toggleThumb, V5Theme.shapes.full),
    )
}

@Composable
private fun V5SliderTrack(fraction: Float, enabled: Boolean) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(V5Sizes.sliderTrack)
            .graphicsLayer { alpha = if (enabled) 1f else V5_DISABLED_ALPHA }
            .clip(V5Theme.shapes.full)
            .background(colors.fill),
    ) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.accentFill))
    }
}
