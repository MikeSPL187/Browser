package dev.sk2andy.materialbrowser.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * The reading view's settings drawn by the app in its own design system (the v5 sheet on the phone);
 * without them the panel uses Material's. Shared code keeps what each control changes, its test tag
 * and what TalkBack reads.
 */
interface ReaderStudioControls {
    /**
     * One of [labels] chosen. A label is a sample («Aa» in [fontFamilies]); [descriptions] are the
     * names, which TalkBack reads and the app may show under the samples.
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

    /** The panel around the settings: [content] is its column of rows. */
    @Composable
    fun Panel(modifier: Modifier, content: @Composable () -> Unit)

    /**
     * The text size: [title] and the size in use ([valueText]) over a slider between a small and a
     * large «A» in [sampleFontFamily]. TalkBack reads [title] for the slider.
     */
    @Composable
    fun TextSize(
        title: String,
        valueText: String,
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        sampleFontFamily: FontFamily,
        modifier: Modifier,
    )

    /**
     * The reading themes, each a sample «Aa» in its own [backgrounds] and [contents] colors over its
     * name from [names]; one is [selected].
     */
    @Composable
    fun Themes(
        names: List<String>,
        backgrounds: List<Color>,
        contents: List<Color>,
        sampleFontFamily: FontFamily,
        selected: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
    )

    /** A quiet line under the settings, such as where they apply. */
    @Composable
    fun Footnote(text: String, modifier: Modifier)
}
