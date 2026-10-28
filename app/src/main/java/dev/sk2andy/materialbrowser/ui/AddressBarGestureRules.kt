package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
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

    /**
     * Under v5, a swipe past the last tab that stretched the rubber band nearly to its end opens a
     * new tab when let go (system-v5.md section 7, `tabSwipe`); a shorter pull only springs back.
     */
    fun opensNewTabPastLast(offsetPx: Float, edgeTravelMaxPx: Float, newTabFraction: Float): Boolean =
        edgeTravelMaxPx > 0f && offsetPx <= -edgeTravelMaxPx * newTabFraction

    fun hasReachedDistance(dragDistance: Float, viewportWidth: Float): Boolean =
        viewportWidth > 0f && dragDistance >= viewportWidth * DISTANCE_FRACTION
}

