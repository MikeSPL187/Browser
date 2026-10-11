package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The site's color in the bar (system-v5.md, section 1.3), the same rule as siteTint() in the v5
 * canvas: in OKLCH, acid colors are softened (chroma above 0.15 is compressed to a quarter), a
 * color barely off the neutral bar stays neutral, and the text is white whenever darkening the
 * color by at most 0.12 in lightness reaches 4.5 : 1; otherwise the text is dark and the color is
 * lightened until it reaches 4.5 : 1. Every step rounds to an sRGB hex color like the canvas, so
 * the results match its table exactly.
 */

/** The bar's colors for one site. [neutral] means the plain `chrome` bar with a separator. */
@Immutable
data class V5SiteTint(
    val container: Color,
    val onTint: Color,
    val neutral: Boolean,
) {
    /** Icons and the count on the shield (onTint at 72 %, at least 3 : 1). */
    val onTintSecondary: Color get() = onTint.copy(alpha = SECONDARY_ALPHA)

    /** The address capsule and the round buttons: onTint at 12 % over the color. */
    val field: Color get() = onTint.copy(alpha = FIELD_ALPHA).compositeOver(container)

    /** Pressed state: onTint at 16 % over the color. */
    val pressed: Color get() = onTint.copy(alpha = PRESSED_ALPHA).compositeOver(container)

    private companion object {
        const val SECONDARY_ALPHA = 0.72f
        const val FIELD_ALPHA = 0.12f
        const val PRESSED_ALPHA = 0.16f
    }
}

object V5SiteTints {
    private const val CHROMA_KNEE = 0.15
    private const val CHROMA_SLOPE = 0.25
    private const val NEUTRAL_LIGHTNESS = 0.03
    private const val NEUTRAL_CHROMA = 0.02
    private const val STEP = 0.005
    private const val MAX_DARKEN = 0.12
    private const val MAX_LIGHTEN = 0.6
    private const val TEXT_CONTRAST = 4.5
    private const val White = 0xFFFFFF
    private const val DarkText = 0x111114

    /**
     * The tint for [siteColorRgb] (0xRRGGBB, alpha ignored) on a bar whose neutral color is
     * [chrome]; null [siteColorRgb] or a color too close to [chrome] gives the neutral bar.
     */
    fun of(siteColorRgb: Int?, chrome: Color, neutralText: Color): V5SiteTint {
        val neutral = V5SiteTint(container = chrome, onTint = neutralText, neutral = true)
        siteColorRgb ?: return neutral
        val (lightness, a, b) = toOklab(siteColorRgb and 0xFFFFFF)
        var chroma = hypot(a, b)
        val hue = atan2(b, a)
        if (chroma > CHROMA_KNEE) chroma = CHROMA_KNEE + (chroma - CHROMA_KNEE) * CHROMA_SLOPE
        val chromeLightness = toOklab(chrome.toRgb())[0]
        if (kotlin.math.abs(lightness - chromeLightness) < NEUTRAL_LIGHTNESS && chroma < NEUTRAL_CHROMA) {
            return neutral
        }
        fun at(l: Double): Int = fromOklab(l, chroma * cos(hue), chroma * sin(hue))
        val base = at(lightness)
        if (contrast(White, base) >= TEXT_CONTRAST) return tint(base, White)
        var darken = STEP
        while (darken <= MAX_DARKEN + EPSILON) {
            val darker = at(lightness - darken)
            if (contrast(White, darker) >= TEXT_CONTRAST) return tint(darker, White)
            darken += STEP
        }
        var lighten = 0.0
        while (contrast(DarkText, at(lightness + lighten)) < TEXT_CONTRAST && lighten < MAX_LIGHTEN) {
            lighten += STEP
        }
        return tint(at(lightness + lighten), DarkText)
    }

    /** WCAG 2.1 contrast of two 0xRRGGBB colors. */
    internal fun contrast(first: Int, second: Int): Double {
        val x = luminance(first)
        val y = luminance(second)
        return (max(x, y) + LUMINANCE_FLARE) / (min(x, y) + LUMINANCE_FLARE)
    }

    internal fun toOklab(rgb: Int): DoubleArray {
        val r = linear(channel(rgb, RED_SHIFT))
        val g = linear(channel(rgb, GREEN_SHIFT))
        val bl = linear(channel(rgb, BLUE_SHIFT))
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * bl)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * bl)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * bl)
        return doubleArrayOf(
            0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
        )
    }

    /** OKLab to an sRGB color rounded to 8 bits per channel, clipped to the gamut like the canvas. */
    internal fun fromOklab(lightness: Double, a: Double, b: Double): Int {
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s = (lightness - 0.0894841775 * a - 1.291485548 * b).pow(3)
        val r = gamma(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s)
        val g = gamma(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s)
        val bl = gamma(-0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s)
        return (byte(r) shl RED_SHIFT) or (byte(g) shl GREEN_SHIFT) or byte(bl)
    }

    private fun tint(rgb: Int, text: Int) = V5SiteTint(
        container = Color(OPAQUE or rgb),
        onTint = Color(OPAQUE or text),
        neutral = false,
    )

    private fun luminance(rgb: Int): Double =
        0.2126 * linear(channel(rgb, RED_SHIFT)) +
            0.7152 * linear(channel(rgb, GREEN_SHIFT)) +
            0.0722 * linear(channel(rgb, BLUE_SHIFT))

    private fun channel(rgb: Int, shift: Int): Double = ((rgb shr shift) and 0xFF) / 255.0

    private fun linear(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun gamma(c: Double): Double =
        if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055

    private fun byte(value: Double): Int = (value.coerceIn(0.0, 1.0) * 255).roundToInt()

    private fun Color.toRgb(): Int =
        (byte(red.toDouble()) shl RED_SHIFT) or (byte(green.toDouble()) shl GREEN_SHIFT) or byte(blue.toDouble())

    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BLUE_SHIFT = 0
    private const val OPAQUE = -0x1000000
    private const val EPSILON = 1e-9
    private const val LUMINANCE_FLARE = 0.05
}
