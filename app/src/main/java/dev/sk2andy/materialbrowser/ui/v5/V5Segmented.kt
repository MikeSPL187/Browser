package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import dev.sk2andy.materialbrowser.ui.clearSemanticsWhen
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic

object V5SegmentedTestTags {
    fun segment(index: Int) = "v5_segment:$index"
}

/** How wide each segment is. */
internal object V5SegmentedRules {
    /**
     * Equal shares while every label fits its share, as Safari draws them. When one does not (a
     * long word at a large font size), each segment gets the width its label needs instead, so no
     * label is cut while the others leave room.
     */
    fun weights(labelWidths: List<Int>, available: Int): List<Float> {
        if (labelWidths.isEmpty()) return emptyList()
        val share = available / labelWidths.size
        return if (labelWidths.all { it <= share }) {
            List(labelWidths.size) { 1f }
        } else {
            labelWidths.map { it.coerceAtLeast(1).toFloat() }
        }
    }
}

/**
 * A segmented control (system-v5.md, section 3): equal capsules on a fill, the chosen one raised on
 * the surface in semibold. Choosing another segment ticks once. At a large font size a label that
 * does not fit its equal share widens its segment ([V5SegmentedRules.weights]) and, past that, wraps.
 */
@Composable
fun V5Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** What TalkBack reads for each segment when its label is a sample, such as «Aa» in a typeface. */
    descriptions: List<String>? = null,
    /** Each label's own typeface, so a typeface choice shows itself. */
    fontFamilies: List<FontFamily>? = null,
) {
    val colors = V5Theme.colors
    val view = LocalView.current
    val measurer = rememberTextMeasurer()
    val style = V5Theme.type.callout
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val labelPadding = with(LocalDensity.current) { (V5Spacing.space2 * 2).roundToPx() }
        // The row's inner padding is not the segments'.
        val available = constraints.maxWidth - with(LocalDensity.current) { (V5Spacing.space1 * 2).roundToPx() }
        val weights = remember(options, fontFamilies, style, available) {
            val widths = options.mapIndexed { index, label ->
                // The chosen segment is semibold: measure that, so choosing never changes the widths.
                measurer.measure(
                    label,
                    style.copy(fontWeight = FontWeight.SemiBold, fontFamily = fontFamilies?.getOrNull(index)),
                    maxLines = 1,
                    constraints = Constraints(),
                ).size.width + labelPadding
            }
            V5SegmentedRules.weights(widths, available)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(V5Theme.shapes.full)
                .background(colors.fill)
                .padding(V5Spacing.space1)
                .selectableGroup(),
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    Modifier
                        .weight(weights[index])
                        .heightIn(min = V5Sizes.touchTarget)
                        .clip(V5Theme.shapes.full)
                        .background(if (selected) colors.surface else colors.fill)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = {
                                if (!selected) {
                                    view.performV5Haptic(V5Haptic.SegmentTick)
                                    onSelect(index)
                                }
                            },
                        )
                        .then(descriptions?.getOrNull(index)?.let { Modifier.semantics { contentDescription = it } } ?: Modifier)
                        .testTag(V5SegmentedTestTags.segment(index)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = V5Theme.type.callout.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (selected) colors.label else colors.labelSecondary,
                        fontFamily = fontFamilies?.getOrNull(index),
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = V5Spacing.space2)
                            .clearSemanticsWhen(descriptions != null),
                    )
                }
            }
        }
    }
}
