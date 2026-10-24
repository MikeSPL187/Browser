package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioControls
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Materials
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5Slider
import dev.sk2andy.materialbrowser.ui.v5.V5Toggle

/**
 * The reading view's settings as the v5 sheet on the phone (canvas screen «ReaderSettings»): three
 * typeface cards, the text size with its percentage, the themes as round swatches in their own
 * colors, and where the settings apply.
 */
internal object V5ReaderStudioControls : ReaderStudioControls {
    @Composable
    override fun Panel(modifier: Modifier, content: @Composable () -> Unit) {
        V5PromptTheme {
            val colors = V5Theme.colors
            val shape = V5Theme.shapes.sheet
            Column(
                modifier
                    .clip(shape)
                    .background(V5Materials.thick.tint(dark = colors.isDark, blurAvailable = false, reduceTransparency = false), shape)
                    .border(V5Spacing.hairline, colors.edge, shape)
                    .navigationBarsPadding(),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = V5Sizes.sheetHandleTop)
                        .size(V5Sizes.sheetHandleWidth, V5Sizes.sheetHandleHeight)
                        .clip(V5Theme.shapes.full)
                        .background(colors.labelTertiary),
                )
                CompositionLocalProvider(LocalContentColor provides colors.label, content = content)
            }
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
            val colors = V5Theme.colors
            Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                labels.forEachIndexed { index, label ->
                    val chosen = index == selected
                    val shape = V5Theme.shapes.m
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = V5Sizes.readerFontCard)
                            .clip(shape)
                            .background(if (chosen) colors.accentSoft else colors.surface)
                            .border(V5Sizes.readerRing, if (chosen) colors.accent else Color.Transparent, shape)
                            .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(index) })
                            .semantics { contentDescription = descriptions[index] }
                            .padding(V5Spacing.space2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(V5Spacing.space1, Alignment.CenterVertically),
                    ) {
                        Text(
                            label,
                            style = V5Theme.type.title,
                            fontFamily = fontFamilies[index],
                            color = colors.label,
                            maxLines = 1,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                        // The name is shown as well, so a typeface needs no guessing; TalkBack reads it once,
                        // as the card's description.
                        Text(
                            descriptions[index],
                            style = V5Theme.type.caption,
                            fontWeight = if (chosen) FontWeight.SemiBold else null,
                            color = if (chosen) colors.accent else colors.labelSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                    }
                }
            }
        }
    }

    @Composable
    override fun Switch(checked: Boolean) {
        V5PromptTheme { V5Toggle(checked = checked, onCheckedChange = null) }
    }

    @Composable
    override fun TextSize(
        title: String,
        valueText: String,
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        sampleFontFamily: FontFamily,
        modifier: Modifier,
    ) {
        V5PromptTheme {
            val colors = V5Theme.colors
            Column(
                modifier
                    .clip(V5Theme.shapes.m)
                    .background(colors.surface)
                    .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
            ) {
                // The slider reads the title and the value, so the row above it stays silent.
                Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = V5Theme.type.callout, color = colors.label, modifier = Modifier.weight(1f))
                    Text(valueText, style = V5Theme.type.callout, fontWeight = FontWeight.SemiBold, color = colors.label)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
                    Text(
                        "A",
                        style = V5Theme.type.footnote,
                        fontFamily = sampleFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.label,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                    V5Slider(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = title
                            stateDescription = valueText
                        },
                        valueRange = valueRange,
                        steps = steps,
                    )
                    Text(
                        "A",
                        style = V5Theme.type.title,
                        fontFamily = sampleFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.label,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
        }
    }

    @Composable
    override fun Themes(
        names: List<String>,
        backgrounds: List<Color>,
        contents: List<Color>,
        sampleFontFamily: FontFamily,
        selected: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
    ) {
        V5PromptTheme {
            val colors = V5Theme.colors
            Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.SpaceAround) {
                names.forEachIndexed { index, name ->
                    val chosen = index == selected
                    val circle = V5Theme.shapes.full
                    Column(
                        Modifier
                            .clip(V5Theme.shapes.m)
                            .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(index) })
                            .padding(V5Spacing.space1),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(V5Spacing.space1),
                    ) {
                        // The chosen swatch wears an accent ring that stands off it by the ring's width.
                        Box(
                            Modifier
                                .size(V5Sizes.readerThemeSwatch + V5Sizes.readerRing * 4)
                                .border(V5Sizes.readerRing, if (chosen) colors.accent else Color.Transparent, circle)
                                .padding(V5Sizes.readerRing * 2)
                                .clip(circle)
                                .background(backgrounds[index])
                                .border(V5Spacing.hairline, colors.separator, circle),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Aa",
                                style = V5Theme.type.headline,
                                fontFamily = sampleFontFamily,
                                color = contents[index],
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }
                        Text(
                            name,
                            style = V5Theme.type.caption,
                            fontWeight = if (chosen) FontWeight.SemiBold else null,
                            color = if (chosen) colors.accent else colors.labelSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    @Composable
    override fun Footnote(text: String, modifier: Modifier) {
        V5PromptTheme {
            Text(
                text,
                style = V5Theme.type.footnote,
                color = V5Theme.colors.labelSecondary,
                textAlign = TextAlign.Center,
                modifier = modifier,
            )
        }
    }
}
