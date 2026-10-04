package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.ui.unit.dp

/** Split View on a phone (board W-Split): two page cards, one above the other. */
internal object VolaSplit {
    /** The gap between the cards, where the divider handle sits. */
    val dividerHeight = 16.dp

    val handleWidth = 48.dp
    val handleHeight = 5.dp
    const val HANDLE_ALPHA = 0.45f

    /** The pill on the divider: swap, the handle, close. */
    val controlsHeight = 40.dp
    val controlsPadding = 4.dp
    val controlIconSize = 20.dp
    const val CONTROLS_ALPHA = 0.94f

    /** The active card's outline, in the accent. */
    val activeOutlineWidth = 2.5.dp
}
