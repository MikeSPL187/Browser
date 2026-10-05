package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserColorPalette
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** «Appearance» by board W-SetAppearance: Frame chosen, the theme follows the system. */
@VolaPreviews
@Composable
private fun AppearanceSettingsPreview() {
    MaterialBrowserTheme {
        AppearanceSettingsPage(
            settings = AppearanceSettings(),
            workspaceAccent = WorkspaceAccent.Default,
            onSettingsChanged = {},
            onOpenThemes = {},
            onBack = {},
        )
    }
}

/** «Themes» by board W-Themes: Ice with one coral accent for every space. */
@VolaPreviews
@Composable
private fun ThemesSettingsPreview() {
    val settings = AppearanceSettings(
        colorPalette = BrowserColorPalette.Ice,
        accentOverride = WorkspaceAccent.Coral,
    )
    MaterialBrowserTheme(settings = settings) {
        ThemesSettingsPage(
            settings = settings,
            workspaceAccent = WorkspaceAccent.Teal,
            onSettingsChanged = {},
            onBack = {},
        )
    }
}
