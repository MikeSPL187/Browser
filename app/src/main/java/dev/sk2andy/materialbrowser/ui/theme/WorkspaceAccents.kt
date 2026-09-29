package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent

/**
 * Accent colors of workspaces. Each accent has a light-theme tone (Material tone 40) and a
 * dark-theme tone (tone 80), so a swatch keeps enough contrast on the surface it sits on.
 */
internal object WorkspaceAccentTokens {
    private val light = mapOf(
        WorkspaceAccent.Violet to Color(0xFF5E4EB7),
        WorkspaceAccent.Blue to Color(0xFF2F5BD3),
        WorkspaceAccent.Teal to Color(0xFF006877),
        WorkspaceAccent.Green to Color(0xFF2B6C3F),
        WorkspaceAccent.Amber to Color(0xFF7C5800),
        WorkspaceAccent.Coral to Color(0xFFA23F2B),
        WorkspaceAccent.Rose to Color(0xFFA0305B),
        WorkspaceAccent.Graphite to Color(0xFF55595F),
    )

    private val dark = mapOf(
        WorkspaceAccent.Violet to Color(0xFFC9BFFF),
        WorkspaceAccent.Blue to Color(0xFFB3C5FF),
        WorkspaceAccent.Teal to Color(0xFF75D4E7),
        WorkspaceAccent.Green to Color(0xFF93D6A0),
        WorkspaceAccent.Amber to Color(0xFFF7BD48),
        WorkspaceAccent.Coral to Color(0xFFFFB4A5),
        WorkspaceAccent.Rose to Color(0xFFFFB0C8),
        WorkspaceAccent.Graphite to Color(0xFFC3C7CE),
    )

    fun color(accent: WorkspaceAccent, dark: Boolean): Color =
        (if (dark) this.dark else light).getValue(accent)
}

/** The accent's tone for the current theme. */
@Composable
@ReadOnlyComposable
internal fun WorkspaceAccent.color(): Color = WorkspaceAccentTokens.color(
    accent = this,
    dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
)
