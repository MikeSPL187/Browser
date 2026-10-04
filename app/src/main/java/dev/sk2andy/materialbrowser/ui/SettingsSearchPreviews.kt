package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** Settings search before typing: the field and the hint (board W-Settings, the magnifier). */
@VolaPreviews
@Composable
private fun SettingsSearchPreview() {
    MaterialBrowserTheme {
        SettingsSearchPage(onOpen = {}, onBack = {})
    }
}
