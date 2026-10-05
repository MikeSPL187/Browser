package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.data.DeveloperSettings
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** Developer options on cards, with HTTP password autofill out of reach of this engine. */
@VolaPreviews
@Composable
private fun DeveloperOptionsPreview() {
    MaterialBrowserTheme {
        DeveloperOptionsSettingsPage(
            settings = DeveloperSettings(forceSafeAreaFallback = true),
            isInputDiagnosticsEnabled = true,
            onSettingsChanged = {},
            onBack = {},
        )
    }
}
