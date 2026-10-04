package dev.sk2andy.materialbrowser.shared.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.sk2andy.materialbrowser.reader.ReaderTheme

/** The colors of one reading theme. */
@Immutable
data class ReaderPalette(
    /** Around the article. */
    val background: Color,
    val content: Color,
    val muted: Color,
    /** Quotes and the progress track. */
    val card: Color,
    val accent: Color,
    val onAccent: Color,
)

/**
 * The look of the reader (board W-Reader), passed in by the app from its design tokens: shared
 * code has no tokens of its own, as with the tab grid's card style.
 */
@Immutable
data class ReaderStudioStyle(
    val light: ReaderPalette,
    val paper: ReaderPalette,
    val dark: ReaderPalette,
    val serifFontFamily: FontFamily,
    val sansFontFamily: FontFamily,
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val progressHeight: Dp,
    val articlePadding: Dp,
    /** Article padding with «Wide margins» on. */
    val wideArticlePadding: Dp,
    val articleGap: Dp,
    val headerPadding: Dp,
    val headerGap: Dp,
    val siteGemSize: Dp,
    val siteGemShape: Shape,
    /** Space around the floating «Reading view» panel. */
    val panelMargin: Dp,
    val panelShape: Shape,
    val panelElevation: Dp,
    val panelPadding: Dp,
    val panelGap: Dp,
    val swatchHeight: Dp,
    val swatchShape: Shape,
    val swatchSelectedBorder: Dp,
    val cardShape: Shape,
    val cardPadding: PaddingValues,
    val buttonHeight: Dp,
    val iconSize: Dp,
    val handleWidth: Dp,
    val handleHeight: Dp,
) {
    fun palette(theme: ReaderTheme, browserDark: Boolean): ReaderPalette =
        when (theme.resolved(browserDark)) {
            ReaderTheme.Paper -> paper
            ReaderTheme.Dark -> dark
            ReaderTheme.System,
            ReaderTheme.Light,
            -> light
        }
}
