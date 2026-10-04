package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsTileColors
import dev.sk2andy.materialbrowser.ui.theme.LocalVolaDarkTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaColorRules

/**
 * A settings tile in [accent]'s own container colors, as a workspace gem keeps its own color
 * whatever workspace tints the screen around it.
 */
@Composable
@ReadOnlyComposable
internal fun settingsTileColors(accent: WorkspaceAccent): SettingsTileColors {
    val tokens = VolaColorRules.schemeSet(accent, privateMode = false)
        .select(dark = LocalVolaDarkTheme.current, highContrast = false)
    return SettingsTileColors(
        container = Color(tokens.primaryContainer),
        content = Color(tokens.onPrimaryContainer),
    )
}
