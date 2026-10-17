package dev.sk2andy.materialbrowser.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily

/**
 * The reading view's three controls drawn by the app in its own design system (the v5 slider,
 * segmented choice and switch on the phone); without them the panel uses Material's. Shared code
 * keeps what each control changes, its test tag and what TalkBack reads.
 */
interface ReaderStudioControls {
    @Composable
    fun Slider(
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        modifier: Modifier,
    )

    /**
     * One of [labels] chosen. A label is a sample («Aa» in [fontFamilies]); [descriptions] are the
     * names TalkBack reads.
     */
    @Composable
    fun Choice(
        labels: List<String>,
        descriptions: List<String>,
        fontFamilies: List<FontFamily>,
        selected: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
    )

    /** The switch at the end of a row that toggles as a whole: it shows [checked] and takes no taps. */
    @Composable
    fun Switch(checked: Boolean)
}
