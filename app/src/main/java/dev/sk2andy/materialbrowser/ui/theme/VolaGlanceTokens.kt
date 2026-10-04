package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Glance (board W-Glance, proposal П4): the live link preview card over the page, with a handle
 * to pull it up into a tab and a note that it stays out of history until opened.
 */
internal object VolaGlance {
    val handleWidth = 40.dp
    val handleHeight = 5.dp
    val handleTouchHeight = 24.dp
    /** Light on the dark scrim in both themes, as on the board. */
    val handleColor = Color.White.copy(alpha = 0.9f)

    /** How far the card goes up before letting go opens the link. */
    val pullOpenThreshold = 96.dp

    val headerPaddingStart = VolaSpacing.x4
    val headerPaddingEnd = 6.dp
    val headerGap = VolaSpacing.x2
    val headerMinHeight = 52.dp

    val noteShape = RoundedCornerShape(14.dp)
    val noteMargin = VolaSpacing.x3
    val noteMinHeight = 36.dp
    val notePaddingHorizontal = VolaSpacing.x3
    val noteGap = VolaSpacing.x2
    val noteIconSize = 16.dp

    /** The download rows under the note are text buttons; they still get a full touch target. */
    val minTouchTarget = 48.dp
}
