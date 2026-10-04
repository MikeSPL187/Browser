package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt

/** The pure part of a whole-number slider on a settings card. */
internal object SettingsSliderRules {
    /** Material counts the stops between the ends, not the values. */
    fun steps(range: IntRange, step: Int): Int =
        ((range.last - range.first) / step - 1).coerceAtLeast(0)
}

/**
 * A slider over whole numbers for a settings card row. [onValueChange] follows the finger;
 * [onValueChangeFinished] reports the value once, when the finger lifts.
 */
@Composable
internal fun SettingsCardSlider(
    value: Int,
    range: IntRange,
    label: String,
    onValueChangeFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit = {},
) {
    val state = remember(value, range, step) {
        SliderState(
            value = value.toFloat(),
            steps = SettingsSliderRules.steps(range, step),
            trackRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
    Slider(
        state = state,
        onValueChange = { candidate ->
            state.value = candidate
            onValueChange(state.value.roundToInt())
        },
        // TalkBack reads the row's title with the value, not a bare number.
        modifier = modifier.semantics { contentDescription = label },
        enabled = enabled,
        onValueChangeFinished = { onValueChangeFinished(state.value.roundToInt()) },
    )
}
