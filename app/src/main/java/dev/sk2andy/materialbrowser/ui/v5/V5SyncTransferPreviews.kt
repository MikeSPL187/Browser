package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* Sync over QR as canvas screens «ф1»–«ф3», light and dark. The code shows as the loading plate. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val Show = V5SyncShowState(code = null, onWifi = true, networkName = "Дом", tabs = 12, bookmarks = 34)
private val Done = V5SyncDoneState(deviceName = "Pixel Анны", tabs = 12, bookmarksAdded = 30, bookmarksExisting = 4)
private val NoActions = V5SyncDoneActions(onOpenGroup = {}, onOpenFavorites = {}, onDone = {}, onUndo = {})

@Preview(name = "Show code · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncShowLightPreview() {
    VolaV5Theme(dark = false) { V5SyncShowCode(Show, onBack = {}) }
}

@Preview(name = "Show code, no Wi-Fi · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncShowNoWifiDarkPreview() {
    VolaV5Theme(dark = true) { V5SyncShowCode(Show.copy(onWifi = false, networkName = null), onBack = {}) }
}

@Preview(name = "Scan code", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncScanPreview() {
    VolaV5Theme(dark = true) {
        V5SyncScan(V5SyncScanStatus.Scanning, onClose = {}) {
            Box(Modifier.fillMaxSize().background(V5Theme.colors.surfaceHighest))
        }
    }
}

@Preview(name = "Scan code, expired", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncScanExpiredPreview() {
    VolaV5Theme(dark = false) { V5SyncScan(V5SyncScanStatus.CodeExpired, onClose = {}) {} }
}

@Preview(name = "Done · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncDoneLightPreview() {
    VolaV5Theme(dark = false) { V5SyncDone(Done, NoActions) }
}

@Preview(name = "Done · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncDoneDarkPreview() {
    VolaV5Theme(dark = true) { V5SyncDone(Done.copy(bookmarksExisting = 0), NoActions) }
}
