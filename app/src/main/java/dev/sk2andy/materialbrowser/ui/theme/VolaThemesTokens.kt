package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** «Themes» (board W-Themes): a live preview, the theme tiles, then accent and corners. */
internal object VolaThemesTokens {
    val sectionGap = VolaSpacing.x4

    /** A small phone on the aura that follows every choice on the page. */
    val previewHeight = 230.dp
    val previewShape = RoundedCornerShape(VolaShapes.extraLargeRadius)
    val previewPhoneWidth = 180.dp
    val previewPhoneHeight = 210.dp
    val previewPhonePadding = VolaSpacing.x3
    val previewPhoneGap = VolaSpacing.x2
    val previewTitleHeight = 9.dp
    val previewLineHeight = 6.dp
    val previewImageHeight = 60.dp
    val previewIslandHeight = 34.dp
    val previewIslandPadding = 5.dp
    val previewGemSize = 24.dp
    const val PREVIEW_TITLE_ALPHA = 0.8f

    /** Theme tiles in rows of three; the chosen one is ringed. */
    val tileSize = 58.dp
    val tileShape = RoundedCornerShape(VolaShapes.largeRadius)
    val tileRing = 2.5.dp
    val tileRingGap = 3.dp
    val tileRingShape = RoundedCornerShape(VolaShapes.largeRadius + tileRing + tileRingGap)
    val tileLabelGap = 6.dp
    val tileRowGap = VolaSpacing.x3
    const val TILES_PER_ROW = 3

    /** The entry on «Appearance»: a small tile of the current theme. */
    val entryTileSize = 40.dp
    val entryTileShape = RoundedCornerShape(VolaShapes.mediumRadius)
    val entryMinHeight = 64.dp
    val entryGap = VolaSpacing.x3

    /** Accent and corners on one card. */
    val cardShape = VolaShapes.card
    val cardPadding = VolaSpacing.x4
    val cardGap = VolaSpacing.x3
    val accentTarget = 48.dp
    val accentDot = 32.dp
    val accentDotSelected = 26.dp
    val accentRing = 2.dp
    val accentCheckSize = 16.dp
    const val ACCENTS_PER_ROW = 5

    /** The tiles of the board: each theme's light end and deeper end. */
    val iceTile = listOf(Color(0xFFDCEFF5), Color(0xFFA9D8E6))
    val duskTile = listOf(Color(0xFF2B2140), Color(0xFF7A4B8C))
    val paperTile = listOf(Color(0xFFF7F1E6), Color(0xFFE6D8BE))
    val monoTile = listOf(Color(0xFFF2F3F4), Color(0xFF9AA0A3))
}
