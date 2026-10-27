package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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

object V5FilterChipsTestTags {
    fun chip(index: Int) = "v5_filter_chip:$index"
}

/**
 * One choice among a list's filters (canvas «Downloads»: «All · Documents · Images · Archives»):
 * capsules in a row that scrolls past the screen's edge, the chosen one in the accent's soft fill.
 * Where the choices fit a fixed width, [V5Segmented] is the control; chips are for a row that may
 * not fit, as with a long translation or the largest font.
 */
@Composable
fun V5FilterChips(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = V5Spacing.marginPhone),
) {
    val colors = V5Theme.colors
    val view = LocalView.current
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            // The capsule is 34 dp as drawn; the touch target around it keeps 48.
            Box(
                Modifier
                    .heightIn(min = V5Sizes.touchTarget)
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
                    .testTag(V5FilterChipsTestTags.chip(index)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .heightIn(min = V5Sizes.filterChip)
                        .clip(V5Theme.shapes.full)
                        .background(if (selected) colors.accentSoft else colors.fill)
                        .padding(horizontal = V5Spacing.space3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = V5Theme.type.callout.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (selected) colors.accent else colors.label,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
