package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberUiV5Flag

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

internal object AddressBarWideLayoutRules {
    const val MIN_WINDOW_WIDTH_DP = 600f

    fun usesTabStrip(windowWidthDp: Float): Boolean =
        windowWidthDp.isFinite() && windowWidthDp >= MIN_WINDOW_WIDTH_DP

    /**
     * Whether the browser is laid out wide. The current interface follows the window's width; v5
     * only a tablet's, whose narrow side reaches [MIN_WINDOW_WIDTH_DP]: a phone turned sideways
     * keeps v5's bar, editor and overview (decision 2026-10-15).
     */
    fun isWide(uiV5: Boolean, windowWidthDp: Float, smallestWidthDp: Float): Boolean =
        usesTabStrip(if (uiV5) smallestWidthDp else windowWidthDp)
}

/** [AddressBarWideLayoutRules.isWide] for this window; [windowWidthDp] is the screen's unless given. */
@Composable
internal fun rememberWideBrowserWindow(
    windowWidthDp: Float = LocalConfiguration.current.screenWidthDp.toFloat(),
): Boolean {
    val uiV5 by rememberUiV5Flag()
    return AddressBarWideLayoutRules.isWide(
        uiV5 = uiV5,
        windowWidthDp = windowWidthDp,
        smallestWidthDp = LocalConfiguration.current.smallestScreenWidthDp.toFloat(),
    )
}

/** Whether v5 draws the browser: the flag is on and the screen is not a tablet's. */
@Composable
internal fun rememberDrawsV5(): Boolean {
    val uiV5 by rememberUiV5Flag()
    return uiV5 && !rememberWideBrowserWindow()
}
