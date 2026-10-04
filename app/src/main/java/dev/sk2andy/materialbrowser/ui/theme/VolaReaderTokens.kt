package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reader mode (board W-Reader): three reading themes, Literata, and the «Reading view» panel.
 * The accent comes from the browser's scheme; these are the page colors of each theme.
 */
internal object VolaReader {
    val lightBackground = Color(0xFFFFFFFF)
    val lightContent = Color(0xFF1C1B1F)
    val lightMuted = Color(0xFF5F5B66)
    val lightCard = Color(0xFFF1EEF3)

    val paperBackground = Color(0xFFF7F1E6)
    val paperContent = Color(0xFF3B3226)
    val paperMuted = Color(0xFF6C5F4B)
    val paperCard = Color(0xFFE9E1D3)

    /** Pure black, as everywhere in Vola's dark theme. */
    val darkBackground = Color(0xFF000000)
    val darkContent = Color(0xFFE6E1E5)
    val darkMuted = Color(0xFFA9A4AE)
    val darkCard = Color(0xFF1C1B1F)

    val titleFontSize = 30.sp
    val titleLineHeight = 37.sp
    val progressHeight = 3.dp
    val articlePadding = VolaSpacing.x6
    val wideArticlePadding = 48.dp
    val articleGap = VolaSpacing.x4

    val headerPadding = VolaSpacing.x3
    val headerGap = VolaSpacing.x2
    val siteGemSize = 22.dp
    val siteGemShape = RoundedCornerShape(7.dp)

    val panelMargin = VolaSpacing.x2
    val panelShape = RoundedCornerShape(VolaShapes.sheetRadius)
    val panelElevation = 12.dp
    val panelPadding = VolaSpacing.x4
    val panelGap = VolaSpacing.x3
    val handleWidth = 36.dp
    val handleHeight = 4.dp

    val swatchHeight = 64.dp
    val swatchShape = RoundedCornerShape(18.dp)
    val swatchSelectedBorder = 2.5.dp
    val cardShape = RoundedCornerShape(VolaShapes.cardRadius)
    val cardPaddingHorizontal = VolaSpacing.x4
    val cardPaddingVertical = 10.dp
    val buttonHeight = 56.dp
    val iconSize = 20.dp
}
