package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** The settings home (board W-Settings) beyond its cards, which live in shared code. */
internal object VolaSettingsHomeTokens {
    /** «Make Vola your default» above the cards, in the accent's container. */
    val bannerShape = RoundedCornerShape(24.dp)
    val bannerMinHeight = 72.dp
    val bannerPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp)
    val bannerGap = 14.dp
    val bannerTextGap = 2.dp

    /** The Vola mark on a light tile, as on the launcher. */
    val logoTileSize = 44.dp
    val logoTileShape = RoundedCornerShape(14.dp)
    val logoSize = 32.dp
}
