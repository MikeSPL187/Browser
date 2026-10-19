package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The «Hide distracting items» hint as canvas screen «k1»: light, dark, private, after a hide and at the largest font. */

private const val PREVIEW_WIDTH_DP = 390

@Composable
private fun PanelPreview(dark: Boolean, privateTab: Boolean, hasHidden: Boolean, canUndo: Boolean = false) {
    VolaV5Theme(dark = dark, privateBrowsing = privateTab) {
        V5DistractionsPanel(
            visible = true,
            site = "polar-notes.ru",
            privateTab = privateTab,
            hasHidden = hasHidden,
            canUndo = canUndo,
            onUndo = {},
            onShowHidden = {},
            onDone = {},
        )
    }
}

@Preview(name = "Hide distractions · light", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelLightPreview() {
    PanelPreview(dark = false, privateTab = false, hasHidden = true)
}

@Preview(name = "Hide distractions · dark", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelDarkPreview() {
    PanelPreview(dark = true, privateTab = false, hasHidden = false)
}

@Preview(name = "Hide distractions · private", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelPrivatePreview() {
    PanelPreview(dark = true, privateTab = true, hasHidden = true)
}

/** Right after a hide: «Undo» brings back that block alone (DS-03). */
@Preview(name = "Hide distractions · undo", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelUndoPreview() {
    PanelPreview(dark = false, privateTab = false, hasHidden = true, canUndo = true)
}

/** The largest font: labels wrap to two lines instead of an ellipsis (DS-04). */
@Preview(name = "Hide distractions · largest font, dark", widthDp = PREVIEW_WIDTH_DP, fontScale = 2f)
@Composable
private fun V5DistractionsPanelLargeFontPreview() {
    PanelPreview(dark = true, privateTab = false, hasHidden = true)
}
