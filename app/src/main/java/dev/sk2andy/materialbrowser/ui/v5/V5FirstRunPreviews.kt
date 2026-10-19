package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 first run as canvas screens «п1»–«п3», light and dark. Not localized. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val Promises = listOf(
    V5FirstRunPromise(VolaIcons.VerifiedUser, V5Tile.Green, "Защита сразу", "Реклама и трекеры блокируются, только HTTPS"),
    V5FirstRunPromise(VolaIcons.Workspaces, V5Tile.Indigo, "Профили", "У работы и дома свои вкладки, история и закреплённые"),
    V5FirstRunPromise(VolaIcons.Lock, V5Tile.Gray, "Всё на устройстве", "Никакой аналитики и рекламных SDK"),
)

@Preview(name = "First run v5 · welcome", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5FirstRunWelcomePreview() {
    VolaV5Theme(dark = false) { V5FirstRunWelcome(promises = Promises, onStart = {}, onImport = {}) }
}

@Preview(name = "First run v5 · welcome, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5FirstRunWelcomeDarkPreview() {
    VolaV5Theme(dark = true) { V5FirstRunWelcome(promises = Promises, onStart = {}, onImport = {}) }
}

@Preview(name = "First run v5 · default browser", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5FirstRunDefaultPreview() {
    VolaV5Theme(dark = false) { V5FirstRunDefaultBrowser(onMakeDefault = {}, onNotNow = {}) }
}

@Preview(name = "First run v5 · default browser, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5FirstRunDefaultDarkPreview() {
    VolaV5Theme(dark = true) { V5FirstRunDefaultBrowser(onMakeDefault = {}, onNotNow = {}) }
}

@Preview(name = "First run v5 · two gestures", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5FirstRunGesturesPreview() {
    VolaV5Theme(dark = false) { V5FirstRunGestures(onDone = {}) }
}

@Preview(name = "First run v5 · two gestures, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5FirstRunGesturesDarkPreview() {
    VolaV5Theme(dark = true) { V5FirstRunGestures(onDone = {}) }
}

// A phone on its side: the card scrolls instead of losing «Got it» under the bar (FR-01).
@Preview(name = "First run v5 · two gestures, landscape", widthDp = PREVIEW_HEIGHT_DP, heightDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FirstRunGesturesLandscapePreview() {
    VolaV5Theme(dark = false) { V5FirstRunGestures(onDone = {}) }
}

@Preview(
    name = "First run v5 · two gestures, largest font, dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    fontScale = 2f,
)
@Composable
private fun V5FirstRunGesturesLargeFontDarkPreview() {
    VolaV5Theme(dark = true) { V5FirstRunGestures(onDone = {}) }
}
