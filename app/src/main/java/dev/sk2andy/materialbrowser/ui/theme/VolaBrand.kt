package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Vola brand colors, taken from the launcher mark (drawable/ic_launcher_foreground_art.xml).
 *
 * They are for brand moments that must look the same in every palette, such as the mark itself
 * or the onboarding. Regular UI takes its colors from MaterialTheme.colorScheme, so it follows
 * the selected palette, workspace and dark mode.
 */
internal object VolaBrand {
    val VioletLight = Color(0xFFA78BFA)
    val Violet = Color(0xFF6D4CFF)
    val Blue = Color(0xFF3B6BFF)
    val Cyan = Color(0xFF22D3EE)

    /** Cyan dark enough for text, icons and fills under white content (5.3:1 on white). */
    val CyanDeep = Color(0xFF0E7490)

    /** Near-black violet for surfaces that carry the mark. */
    val Ink = Color(0xFF16122B)

    /** The two strokes of the mark as one sweep: violet into blue into cyan. */
    val gradientColors: List<Color> = listOf(VioletLight, Violet, Blue, Cyan)

    fun linearGradient(): Brush = Brush.linearGradient(gradientColors)
}
