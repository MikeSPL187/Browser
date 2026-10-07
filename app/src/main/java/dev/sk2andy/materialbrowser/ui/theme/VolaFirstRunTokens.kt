package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The first run (boards W-Welcome, W-Setup): a hero of pages, then the setup card. */
internal object VolaFirstRunTokens {
    val screenPadding = PaddingValues(horizontal = VolaSpacing.x5, vertical = VolaSpacing.x4)
    val sectionGap = VolaSpacing.x4
    val textGap = VolaSpacing.x3
    val buttonGap = VolaSpacing.x3
    /** The least height of a button; a two-line label at a large font makes it taller. */
    val buttonHeight = 56.dp
    val buttonIconGap = VolaSpacing.x2

    /** Three tilted pages above the title: one per space, the middle one in front. */
    val heroHeight = 300.dp
    val heroPageWidth = 132.dp
    val heroPageHeight = 230.dp
    val heroPageShape = RoundedCornerShape(28.dp)
    val heroPagePadding = VolaSpacing.x3
    val heroPageOffset = 92.dp
    val heroPageLift = 14.dp
    val heroLineHeight = 7.dp
    val heroImageHeight = 64.dp
    val heroLineGap = 6.dp
    const val HERO_SIDE_ROTATION = 10f
    const val HERO_SIDE_SCALE = 0.9f
    const val HERO_LINE_ALPHA = 0.18f
    const val GLOW_ALPHA = 0.32f

    /** Where the user is: a pill for the current step, dots for the rest. */
    val dotSize = 6.dp
    val dotActiveWidth = 22.dp
    val dotGap = 6.dp
    val progressHeight = 4.dp
    val progressGap = 6.dp

    /** Light, dark and auto: little phones in each look; the chosen one is ringed. */
    val themeTileHeight = 120.dp
    val themeTileShape = RoundedCornerShape(20.dp)
    val themeTileRing = 2.5.dp
    val themeTilePadding = 10.dp
    val themeTileLineHeight = 8.dp
    val themeTileBlockHeight = 30.dp
    val themeTileGap = VolaSpacing.x3
    val themeLabelGap = 6.dp
    val themeLight = Color(0xFFFFFFFF)
    val themeLightLine = Color(0xFFE4EAEC)
    val themeDark = Color(0xFF000000)
    val themeDarkLine = Color(0xFF1E2A30)

    val cardShape = RoundedCornerShape(24.dp)
    val cardPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    val rowPadding = PaddingValues(vertical = 10.dp)
    val rowGap = VolaSpacing.x3
}
