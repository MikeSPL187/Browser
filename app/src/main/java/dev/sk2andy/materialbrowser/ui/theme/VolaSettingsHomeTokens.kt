package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeIcon
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeStyle
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeTileColors

/**
 * Settings home (board W-Settings): each category has its own soft tile, light in the light
 * theme and deep on pure black in the dark one.
 */
internal object VolaSettingsHome {
    private val Violet = Tile(Color(0xFFE6DEFF), Color(0xFF4A3A9E), Color(0xFF332A66), Color(0xFFD9CFFF))
    private val Green = Tile(Color(0xFFD3F0D9), Color(0xFF2B6C3F), Color(0xFF1D4729), Color(0xFFB4E6C0))
    private val Blue = Tile(Color(0xFFD8EEFF), Color(0xFF1E5E8C), Color(0xFF143F5E), Color(0xFFBEE1FF))
    private val Amber = Tile(Color(0xFFFFEBC2), Color(0xFF6E4F00), Color(0xFF4A3500), Color(0xFFFFDC94))
    private val Pink = Tile(Color(0xFFFFDDEA), Color(0xFF962F59), Color(0xFF63203B), Color(0xFFFFC1D6))
    private val Coral = Tile(Color(0xFFFFE1D6), Color(0xFF9A3A1E), Color(0xFF662714), Color(0xFFFFC6B3))
    private val Teal = Tile(Color(0xFFD4F1EC), Color(0xFF00665A), Color(0xFF00443C), Color(0xFFA6E9DD))
    private val Indigo = Tile(Color(0xFFDDE3FF), Color(0xFF2F4AA8), Color(0xFF203270), Color(0xFFC3CDFF))
    private val Neutral = Tile(Color(0xFFE2E2E6), Color(0xFF303036), Color(0xFF2E2E34), Color(0xFFE2E2E6))

    private fun tileFor(icon: SettingsHomeIcon): Tile = when (icon) {
        SettingsHomeIcon.ProtectionAndData -> Green
        SettingsHomeIcon.Sync -> Blue
        SettingsHomeIcon.Appearance -> Pink
        SettingsHomeIcon.TabsAndGestures -> Coral
        SettingsHomeIcon.Search -> Teal
        SettingsHomeIcon.FirefoxExtensions,
        SettingsHomeIcon.Userscripts,
        -> Neutral
        SettingsHomeIcon.SiteCapsules -> Amber
        SettingsHomeIcon.Downloads -> Green
        SettingsHomeIcon.Browser -> Indigo
        SettingsHomeIcon.DeveloperOptions -> Violet
        SettingsHomeIcon.AboutLegal -> Neutral
    }

    /** The Vola mark sits on white in both themes, as an app icon would. */
    private val MarkBackground = Color.White

    @Composable
    fun style(): SettingsHomeStyle {
        val colors = MaterialTheme.colorScheme
        val dark = LocalVolaDarkTheme.current
        return SettingsHomeStyle(
            cardColor = colors.surfaceContainerLow,
            dividerColor = colors.surfaceContainerHigh,
            defaultCardColor = colors.primaryContainer,
            defaultCardContentColor = colors.onPrimaryContainer,
            defaultMarkColor = MarkBackground,
            tileColors = { icon -> tileFor(icon).colors(dark) },
        )
    }

    private class Tile(
        private val lightContainer: Color,
        private val lightContent: Color,
        private val darkContainer: Color,
        private val darkContent: Color,
    ) {
        fun colors(dark: Boolean): SettingsHomeTileColors = if (dark) {
            SettingsHomeTileColors(darkContainer, darkContent)
        } else {
            SettingsHomeTileColors(lightContainer, lightContent)
        }
    }
}
