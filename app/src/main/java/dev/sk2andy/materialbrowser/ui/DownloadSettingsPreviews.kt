package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.data.BrowserDownloadSettings
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** Download settings on cards: the built-in downloader saving into a folder of its own. */
@VolaPreviews
@Composable
private fun DownloadSettingsPreview() {
    MaterialBrowserTheme {
        DownloadsSettingsPage(
            settings = BrowserDownloadSettings(downloadSubdirectory = "Vola/Documents"),
            externalManagers = emptyList(),
            onSettingsChanged = {},
            onBack = {},
        )
    }
}
