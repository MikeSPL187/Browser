package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** «Appearance» settings (board W-SetAppearance). */
internal object VolaAppearance {
    val sectionGap = VolaSpacing.x3
    val overlinePadding = VolaSpacing.x1

    /** «Frame» and «Air» cards with a small phone each. */
    val styleCardGap = VolaSpacing.x3
    val styleCardShape = RoundedCornerShape(VolaShapes.cardRadius)
    val styleCardPadding = VolaSpacing.x3
    val styleCardSelectedBorder = 2.dp
    val styleCardHairline = 1.dp
    val phoneWidth = 84.dp
    val phoneHeight = 132.dp
    val phoneShape = RoundedCornerShape(16.dp)
    val phonePadding = 5.dp
    val phonePageShape = RoundedCornerShape(11.dp)
    val phoneLineHeight = 4.dp
    val phoneLineGap = 4.dp
    val phoneImageHeight = 26.dp
    val phoneBarHeight = 12.dp
    val phoneContentPadding = 7.dp
    val styleCardTextGap = VolaSpacing.x1

    /** A card of rows: theme and corners, colors. */
    val groupShape = RoundedCornerShape(VolaShapes.cardRadius)
    val groupPadding = VolaSpacing.x4
    val groupGap = VolaSpacing.x3

    /** Choices in a row: the chosen one is a filled pill (board: connected button group). */
    val choiceHeight = 44.dp
    val choiceGap = 4.dp
    val choiceShape = RoundedCornerShape(12.dp)
    val choiceSelectedShape = RoundedCornerShape(50)
    val choiceIconSize = 18.dp
    val choiceIconGap = 6.dp

    val swatchSize = 48.dp
    val swatchRing = 2.5.dp
    val swatchRingGap = 3.dp
    val swatchLabelGap = VolaSpacing.x1

    /** «Mono»: half white, half black, whatever the theme. */
    val monoLight = Color(0xFFF2F2F2)
    val monoDark = Color(0xFF1C1B1F)
}
