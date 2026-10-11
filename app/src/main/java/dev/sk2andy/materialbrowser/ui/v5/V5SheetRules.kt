package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion

/*
 * How a v5 sheet follows the finger (system-v5.md, sections 3.3 and 5): dragged down it moves 1 : 1
 * and closes past a quarter of its height or on a downward fling; dragged up it resists. The scrim
 * fades with the travel. No Android types, so the rules are unit-tested.
 */
object V5SheetRules {
    /** The sheet's offset below its rest after a drag of [deltaPx]; up past the rest it resists. */
    fun drag(offsetPx: Float, deltaPx: Float): Float {
        val next = offsetPx + deltaPx
        if (next >= 0f) return next
        // Above the rest only the part of the move that goes further up is damped.
        val above = if (offsetPx < 0f) deltaPx else next
        return minOf(offsetPx, 0f) + above * V5Motion.SHEET_OVERDRAG_RESISTANCE
    }

    /** Whether a sheet let go at [offsetPx] with [velocityPxPerSecond] (down is positive) closes. */
    fun dismisses(offsetPx: Float, velocityPxPerSecond: Float, heightPx: Float, density: Float): Boolean {
        if (heightPx <= 0f) return false
        if (velocityPxPerSecond <= -V5Motion.SHEET_DISMISS_VELOCITY_DP * density) return false
        if (offsetPx >= heightPx * V5Motion.SHEET_DISMISS_FRACTION) return true
        return offsetPx > 0f && velocityPxPerSecond >= V5Motion.SHEET_DISMISS_VELOCITY_DP * density
    }

    /** The scrim behind a sheet at [presence] (0 hidden, 1 shown) dragged [offsetPx] down. */
    fun scrimAlpha(presence: Float, offsetPx: Float, heightPx: Float): Float {
        val shown = presence.coerceIn(0f, 1f)
        if (heightPx <= 0f) return shown
        return shown * (1f - (offsetPx / heightPx).coerceIn(0f, 1f))
    }
}
