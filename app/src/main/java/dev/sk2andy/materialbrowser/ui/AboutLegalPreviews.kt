package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** «About & legal» on cards: the Vola mark with its version, then imprint, licenses and sources. */
@VolaPreviews
@Composable
private fun AboutLegalPreview() {
    MaterialBrowserTheme {
        SettingsPage(
            title = stringResource(R.string.settings_section_about_legal),
            onBack = {},
        ) {
            AboutLegalSection(onOpenUrl = {})
        }
    }
}
