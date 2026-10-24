package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.browser.DistractionPickingStep
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The «Hide distracting items» hint as canvas screen «k1»: light, dark, private, after a hide and at the largest font. */

private const val PREVIEW_WIDTH_DP = 390

@Composable
private fun PanelPreview(
    dark: Boolean,
    privateTab: Boolean,
    hasHidden: Boolean,
    canUndo: Boolean = false,
    step: DistractionPickingStep = DistractionPickingStep.Choosing,
) {
    VolaV5Theme(dark = dark, privateBrowsing = privateTab) {
        V5DistractionsPanel(
            visible = true,
            site = "polar-notes.ru",
            privateTab = privateTab,
            hasHidden = hasHidden,
            canUndo = canUndo,
            step = step,
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
    PanelPreview(dark = false, privateTab = false, hasHidden = true, canUndo = true, step = DistractionPickingStep.Hidden)
}

/** A block outlined: the line says a second tap hides it (DS-01). */
@Preview(name = "Hide distractions · selected, dark", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelSelectedPreview() {
    PanelPreview(dark = true, privateTab = false, hasHidden = false, step = DistractionPickingStep.Selected)
}

/** A block outlined, light. */
@Preview(name = "Hide distractions · selected, light", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionsPanelSelectedLightPreview() {
    PanelPreview(dark = false, privateTab = false, hasHidden = false, step = DistractionPickingStep.Selected)
}

/** The largest font: labels wrap to two lines instead of an ellipsis (DS-04). */
@Preview(name = "Hide distractions · largest font, dark", widthDp = PREVIEW_WIDTH_DP, fontScale = 2f)
@Composable
private fun V5DistractionsPanelLargeFontPreview() {
    PanelPreview(dark = true, privateTab = false, hasHidden = true)
}

/** Canvas screen «HideMode»: the card for an outlined block, above the bar. */
@Composable
private fun ChoicePreview(dark: Boolean, canPickLarger: Boolean = true, accent: V5Accent = V5Accent.Default) {
    VolaV5Theme(dark = dark, accent = accent) {
        V5DistractionChoice(visible = true, canPickLarger = canPickLarger, onHide = {}, onLarger = {}, onCancel = {})
    }
}

@Preview(name = "Hide distractions · choice, light", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionChoiceLightPreview() {
    ChoicePreview(dark = false)
}

@Preview(name = "Hide distractions · choice, dark", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionChoiceDarkPreview() {
    ChoicePreview(dark = true)
}

/** Nothing larger around the block: «Cancel» alone under «Hide». Green profile. */
@Preview(name = "Hide distractions · choice, nothing larger, green", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5DistractionChoiceNoLargerPreview() {
    ChoicePreview(dark = false, canPickLarger = false, accent = V5Accent.Green)
}

@Preview(name = "Hide distractions · choice, largest font", widthDp = PREVIEW_WIDTH_DP, fontScale = 2f)
@Composable
private fun V5DistractionChoiceLargeFontPreview() {
    ChoicePreview(dark = true)
}
