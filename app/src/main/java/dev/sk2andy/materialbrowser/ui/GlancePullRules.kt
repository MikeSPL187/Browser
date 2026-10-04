package dev.sk2andy.materialbrowser.ui

/**
 * Pulling the Glance card up opens the link in a tab (proposal П4, board W-Glance). The card
 * follows the finger upward only; pulled down, it stays put, since back and the scrim close it.
 */
internal object GlancePullRules {
    /** The card's offset after a drag of [deltaPx], never below its resting place. */
    fun offsetAfterDrag(currentOffsetPx: Float, deltaPx: Float): Float =
        (currentOffsetPx + deltaPx).coerceAtMost(0f)

    /** Whether letting go at [offsetPx] opens the link: the card went up by [thresholdPx]. */
    fun opensOnRelease(offsetPx: Float, thresholdPx: Float): Boolean =
        thresholdPx > 0f && offsetPx <= -thresholdPx
}
