package dev.sk2andy.materialbrowser.shared.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Geometry of the settings home (board W-Settings); colors come from the app's style. */
object SettingsHomeTokens {
    val cardShape = RoundedCornerShape(24.dp)
    val cardGap = 12.dp

    val rowMinHeight = 64.dp
    val rowPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    val rowGap = 16.dp
    val textGap = 2.dp

    val tileSize = 40.dp
    val tileShape = RoundedCornerShape(14.dp)
    val tileIconSize = 22.dp

    /** The hairline between rows starts under the titles, past the tile. */
    val dividerThickness = 1.dp
    val dividerStartInset = 72.dp
    val dividerEndInset = 16.dp

    val defaultCardPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp)
    val defaultCardGap = 14.dp
    val defaultMarkSize = 44.dp
    val defaultMarkShape = RoundedCornerShape(14.dp)
    val defaultMarkIconSize = 32.dp
    val defaultButtonHeight = 40.dp

    /** Room under the header before the first card. */
    val topGap = 4.dp
}
