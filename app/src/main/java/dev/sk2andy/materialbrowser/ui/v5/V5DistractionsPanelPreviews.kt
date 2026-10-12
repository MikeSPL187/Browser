package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The «Hide distracting items» hint as canvas screen «k1», light, dark and in a private tab. */

private const val PREVIEW_WIDTH_DP = 390

@Composable
private fun PanelPreview(dark: Boolean, privateTab: Boolean, hiddenCount: Int) {
    VolaV5Theme(dark = dark, privateBrowsing = privateTab) {
        V5DistractionsPanel(
            visible = true,
            site = "polar-notes.ru",
            privateTab = privateTab,
            hiddenCount = hiddenCount,
            onShowHidden = {},
            onDone = {},
        )
    }
}

@Preview(name = "Hide distractions · light", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelLightPreview() {
    PanelPreview(dark = false, privateTab = false, hiddenCount = 1)
}

@Preview(name = "Hide distractions · dark", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelDarkPreview() {
    PanelPreview(dark = true, privateTab = false, hiddenCount = 0)
}

@Preview(name = "Hide distractions · private", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelPrivatePreview() {
    PanelPreview(dark = true, privateTab = true, hiddenCount = 2)
}
