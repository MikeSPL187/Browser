package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic

object V5SegmentedTestTags {
    fun segment(index: Int) = "v5_segment:$index"
}

/**
 * A segmented control (system-v5.md, section 3): equal capsules on a fill, the chosen one raised on
 * the surface in semibold. Choosing another segment ticks once.
 */
@Composable
fun V5Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    val view = LocalView.current
    Row(
        modifier
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
                    .weight(1f)
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
                    .testTag(V5SegmentedTestTags.segment(index)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = V5Theme.type.callout.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (selected) colors.label else colors.labelSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}
