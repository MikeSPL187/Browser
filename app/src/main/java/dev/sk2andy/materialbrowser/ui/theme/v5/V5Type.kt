package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.sk2andy.materialbrowser.R

/*
 * v5 type scale (system-v5.md, section 2): Inter Variable, sizes as on iOS with 17 sp body text.
 * res/font/inter.ttf is Inter 4.1 (SIL OFL 1.1, license in assets/inter.LICENSE.txt) cut down to
 * Latin, Cyrillic and punctuation, weights 400-700, with the opsz axis kept.
 * Letter spacing follows Inter's own dynamic metrics, not SF Pro's tracking.
 */

/** Inter's optical size axis covers 14-32; each style draws at the instance nearest its size. */
internal const val INTER_OPSZ_MIN = 14f
internal const val INTER_OPSZ_MAX = 32f

internal fun interOpticalSize(fontSizeSp: Float): Float = fontSizeSp.coerceIn(INTER_OPSZ_MIN, INTER_OPSZ_MAX)

/**
 * Inter at one optical size in the given weights. Weight and opsz are set explicitly: the plain
 * Font(resId, weight) overload only labels the file and Android draws the default instance.
 */
@OptIn(ExperimentalTextApi::class)
private fun interFamily(fontSizeSp: Float, vararg weights: FontWeight): FontFamily = FontFamily(
    weights.map { weight ->
        Font(
            resId = R.font.inter,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.Setting("opsz", interOpticalSize(fontSizeSp)),
            ),
        )
    },
)

/** The semibold-plus weight of `title` (between 600 and 700). */
internal val TitleWeight = FontWeight(650)

/** The seven text styles of the v5 shell. */
@Immutable
data class V5Typography(
    /** Screen title: "Settings", "Private browsing". */
    val largeTitle: TextStyle,
    /** Section and sheet title, the number in the privacy report. */
    val title: TextStyle,
    /** The domain in the bar, row titles, buttons. */
    val headline: TextStyle,
    /** Body text, list rows, the address field. */
    val body: TextStyle,
    /** Row subtitles, hints, descriptions (400; 500 for emphasis). */
    val callout: TextStyle,
    /** Group headers, the domain in the collapsed bar (600), tab card titles (500). */
    val footnote: TextStyle,
    /** Tile labels, counters, tags. */
    val caption: TextStyle,
)

object V5Type {
    val default: V5Typography = V5Typography(
        largeTitle = style(34f, 41f, FontWeight.Bold, -0.022f),
        title = style(22f, 28f, TitleWeight, -0.018f),
        headline = style(17f, 22f, FontWeight.SemiBold, -0.013f),
        body = style(17f, 24f, FontWeight.Normal, -0.013f, FontWeight.Medium),
        callout = style(15f, 20f, FontWeight.Normal, -0.009f, FontWeight.Medium),
        footnote = style(13f, 18f, FontWeight.Normal, -0.003f, FontWeight.Medium, FontWeight.SemiBold),
        caption = style(12f, 16f, FontWeight.Medium, 0f),
    )

    /**
     * From this font scale a bar has no room for everything in one line: the bar grows, and a count
     * that would sit beside a field goes to its own line (system-v5.md 8).
     */
    const val LARGE_FONT_SCALE = 1.5f

    /** Tabular figures, so a counter on the shield or a timer does not jump as it changes. */
    fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

    private fun style(
        size: Float,
        lineHeight: Float,
        weight: FontWeight,
        trackingEm: Float,
        vararg otherWeights: FontWeight,
    ): TextStyle = TextStyle(
        fontFamily = interFamily(size, weight, *otherWeights),
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = trackingEm.em,
    )
}
