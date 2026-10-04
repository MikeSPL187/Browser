package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** «Appearance» by board W-SetAppearance: Frame chosen, the theme follows the system. */
@VolaPreviews
@Composable
private fun AppearanceSettingsPreview() {
    MaterialBrowserTheme {
        AppearanceSettingsPage(
            settings = AppearanceSettings(),
            onSettingsChanged = {},
            onBack = {},
        )
    }
}
