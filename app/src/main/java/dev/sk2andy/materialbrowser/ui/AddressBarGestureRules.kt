package dev.sk2andy.materialbrowser.ui

internal enum class AddressBarVerticalAction {
    None,
    OpenTabs,
}

internal object AddressBarGestureRules {
    const val OPEN_TABS_THRESHOLD_DP = 56f
    const val PARKED_OPEN_TABS_THRESHOLD_DP = 24f
    const val PARKED_REPOSITION_LONG_PRESS_MILLIS = 320L

    fun action(dragDistance: Float, threshold: Float): AddressBarVerticalAction = when {
        dragDistance <= -threshold -> AddressBarVerticalAction.OpenTabs
        else -> AddressBarVerticalAction.None
    }
}

internal object AddressBarTabSwitchRules {
    const val DISTANCE_FRACTION = 0.24f

    /** Past the first or last tab the page follows the finger at this fraction, up to the cap. */
    const val EDGE_RESISTANCE = 0.24f
    const val EDGE_TRAVEL_MAX_DP = 42f

    /** A fling switches tabs after this much travel at this speed. */
    const val FLING_MIN_TRAVEL_DP = 24f
    const val FLING_MIN_VELOCITY_DP = 900f

    /** The page settles onto the next tab, or springs back when the swipe falls short. */
    const val SETTLE_MILLIS = 150
    const val CANCEL_DAMPING_RATIO = 0.82f
    const val CANCEL_STIFFNESS = 520f

    fun hasReachedDistance(dragDistance: Float, viewportWidth: Float): Boolean =
        viewportWidth > 0f && dragDistance >= viewportWidth * DISTANCE_FRACTION
}

internal object AddressBarWideLayoutRules {
    const val MIN_WINDOW_WIDTH_DP = 600f

    fun usesTabStrip(windowWidthDp: Float): Boolean =
        windowWidthDp.isFinite() && windowWidthDp >= MIN_WINDOW_WIDTH_DP
}
