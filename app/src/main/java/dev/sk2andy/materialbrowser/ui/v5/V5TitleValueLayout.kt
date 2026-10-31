package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.max

/**
 * A settings row's title and its value, as iOS lays them out: side by side while the value fits in
 * [VALUE_SHARE] of the width on one line, otherwise the value goes under the title at the start.
 * The value was capped at a fixed width beside the title before, and a long one («Только HTTPS во
 * всех окнах») squeezed the title to a word per line (screenshot review, 31.10).
 */
@Composable
internal fun V5TitleValueLayout(
    gap: Dp,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    value: @Composable () -> Unit,
) {
    Layout(contents = listOf(title, value), modifier = modifier) { (titles, values), constraints ->
        val titleMeasurable = titles.single()
        val valueMeasurable = values.single()
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val valueWidth = valueMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        if (width == Constraints.Infinity || valueWidth + gapPx <= width * VALUE_SHARE) {
            val valuePlaceable = valueMeasurable.measure(loose)
            val titleMax = if (width == Constraints.Infinity) width else max(0, width - valuePlaceable.width - gapPx)
            val titlePlaceable = titleMeasurable.measure(loose.copy(maxWidth = titleMax))
            val layoutWidth = if (width == Constraints.Infinity) titlePlaceable.width + gapPx + valuePlaceable.width else width
            val height = max(titlePlaceable.height, valuePlaceable.height)
            layout(layoutWidth, max(height, constraints.minHeight)) {
                titlePlaceable.placeRelative(0, (height - titlePlaceable.height) / 2)
                valuePlaceable.placeRelative(layoutWidth - valuePlaceable.width, (height - valuePlaceable.height) / 2)
            }
        } else {
            val titlePlaceable = titleMeasurable.measure(loose)
            val valuePlaceable = valueMeasurable.measure(loose)
            val height = titlePlaceable.height + valuePlaceable.height
            layout(width, max(height, constraints.minHeight)) {
                titlePlaceable.placeRelative(0, 0)
                valuePlaceable.placeRelative(0, titlePlaceable.height)
            }
        }
    }
}

/** The widest a value may be and still stand beside its title. */
private const val VALUE_SHARE = 0.45f
