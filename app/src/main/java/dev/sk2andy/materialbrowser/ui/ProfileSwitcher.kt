package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

internal object ProfileSwitcherTestTags {
    const val Switcher = "profile_switcher"
    const val Add = "profile_switcher_add"

    fun profile(profileId: String): String = "profile_switcher_profile:$profileId"

    fun syncedBadge(profileId: String): String = "profile_switcher_synced_badge:$profileId"
}

internal fun Modifier.allowTopOverflow(topOverflow: Dp): Modifier = layout { measurable, constraints ->
    val overflowPx = topOverflow.roundToPx().coerceAtLeast(0)
    if (overflowPx == 0 || !constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
    val expandedHeight = (constraints.maxHeight.toLong() + overflowPx)
        .coerceAtMost(Constraints.Infinity.toLong())
        .toInt()
    val placeable = measurable.measure(
        constraints.copy(
            minHeight = expandedHeight,
            maxHeight = expandedHeight,
        ),
    )
    layout(
        width = placeable.width,
        height = constraints.maxHeight,
    ) {
        placeable.placeRelative(0, -overflowPx)
    }
}
