package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioControls
import dev.sk2andy.materialbrowser.ui.v5.V5Segmented
import dev.sk2andy.materialbrowser.ui.v5.V5Slider
import dev.sk2andy.materialbrowser.ui.v5.V5Toggle

/** The reading view's slider, typeface choice and switch as v5 draws them on the phone (canvas «ц3»). */
internal object V5ReaderStudioControls : ReaderStudioControls {
    @Composable
    override fun Slider(
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        modifier: Modifier,
    ) {
        V5PromptTheme {
            V5Slider(value = value, onValueChange = onValueChange, modifier = modifier, valueRange = valueRange, steps = steps)
        }
    }

    @Composable
    override fun Choice(
        labels: List<String>,
        descriptions: List<String>,
        fontFamilies: List<FontFamily>,
        selected: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
    ) {
        V5PromptTheme {
            V5Segmented(
                options = labels,
                selectedIndex = selected,
                onSelect = onSelect,
                modifier = modifier,
                descriptions = descriptions,
                fontFamilies = fontFamilies,
            )
        }
    }

    @Composable
    override fun Switch(checked: Boolean) {
        V5PromptTheme { V5Toggle(checked = checked, onCheckedChange = null) }
    }
}
