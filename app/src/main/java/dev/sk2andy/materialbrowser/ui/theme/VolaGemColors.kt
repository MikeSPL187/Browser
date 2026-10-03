package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent

/** The fill and icon color of a workspace gem (`.gem` in vola4.css). */
@Immutable
internal data class VolaGemColors(
    val fill: Brush,
    val content: Color,
    /** Plain primary, for what a gradient cannot fill: buttons and rings. */
    val solid: Color,
)

/**
 * A gem in the colors of [accent]'s own scheme, whatever workspace tints the screen around it:
 * the gem of «Аниме» stays terracotta inside «Работа».
 */
@Composable
@ReadOnlyComposable
internal fun volaGemColors(accent: WorkspaceAccent, privateMode: Boolean = false): VolaGemColors {
    val tokens = VolaColorRules.schemeSet(accent, privateMode)
        .select(dark = LocalVolaDarkTheme.current, highContrast = false)
    val primary = Color(tokens.primary)
    return VolaGemColors(
        fill = Brush.linearGradient(
            0f to lerp(primary, Color.White, VolaGem.HIGHLIGHT_FRACTION),
            VolaGem.PRIMARY_STOP to primary,
        ),
        content = Color(tokens.onPrimary),
        solid = primary,
    )
}
