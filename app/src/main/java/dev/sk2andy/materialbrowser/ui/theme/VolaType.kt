package dev.sk2andy.materialbrowser.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
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
 * Both fonts ship as variable TTFs in res/font (SIL OFL 1.1, licenses in assets). Nothing is
 * downloaded at runtime.
 */

/**
 * One weight of a variable font. The weight axis must be set explicitly: the plain
 * Font(resId, weight) overload only labels the file and Android then draws the font's default
 * instance, which for Manrope is ExtraLight.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(@FontRes resId: Int, weight: FontWeight): Font = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Manrope: the interface font. */
internal val ManropeFontFamily = FontFamily(
    variableFont(R.font.manrope, FontWeight.Normal),
    variableFont(R.font.manrope, FontWeight.Medium),
    variableFont(R.font.manrope, FontWeight.SemiBold),
    variableFont(R.font.manrope, FontWeight.Bold),
)

/** Literata: the reading font for articles and reader mode. */
internal val LiterataFontFamily = FontFamily(
    variableFont(R.font.literata, FontWeight.Normal),
    variableFont(R.font.literata, FontWeight.Medium),
    variableFont(R.font.literata, FontWeight.Bold),
)

/** The v4 type scale, one style per `.ty-*` class of vola4.css. Weights stay within 500–700. */
internal object VolaTypeScale {
    val display = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.025).em,
    )
    val headline = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.02).em,
    )
    val titleLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    )
    val title = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.005).em,
    )
    val body = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
    val label = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
    val caption = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
    )

    /** Section overline. TextStyle has no text transform: pass the text through uppercase(). */
    val overline = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.06.em,
    )
    val reading = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
    )
}

/**
 * Material roles on the v4 scale. Roles the scale has no step for keep Material's sizes in the
 * v4 fonts and weights, so existing screens keep their layout.
 */
internal val VolaTypography: Typography = with(VolaTypeScale) {
    Typography(
        displayLarge = display.copy(fontSize = 57.sp, lineHeight = 64.sp),
        displayMedium = display.copy(fontSize = 45.sp, lineHeight = 52.sp),
        displaySmall = display,
        headlineLarge = headline.copy(fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium = headline,
        headlineSmall = headline.copy(
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.015).em,
        ),
        titleLarge = titleLarge,
        titleMedium = title,
        titleSmall = title.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.em),
        bodyLarge = body,
        bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = caption,
        labelLarge = label,
        labelMedium = label.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = label.copy(fontSize = 11.sp, lineHeight = 16.sp),
    )
}
