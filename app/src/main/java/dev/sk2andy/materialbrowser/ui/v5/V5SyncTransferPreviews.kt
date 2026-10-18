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
private val NoServerActions = V5SyncServerActions(onConnect = {}, onSyncNow = {}, onOpenDevice = {}, onDisconnect = {})
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

@Preview(name = "Done, some did not fit, undo failed · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncDoneUndoFailedPreview() {
    VolaV5Theme(dark = false) {
        V5SyncDone(
            Done.copy(bookmarksSkipped = 6, undoFailed = true),
            V5SyncDoneActions(onOpenGroup = null, onOpenFavorites = null, onDone = {}, onUndo = null),
        )
    }
}

@Preview(name = "Sync settings · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncSettingsLightPreview() {
    VolaV5Theme(dark = false) { V5SyncSettings(sent = false, server = V5SyncServer.NotConnected, serverActions = NoServerActions, onBack = {}, onShowCode = {}, onScanCode = {}) }
}

@Preview(name = "Sync settings, own server connected, sent · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncSettingsSentDarkPreview() {
    VolaV5Theme(dark = true) { V5SyncSettings(
            sent = true,
            server = V5SyncServer.Connected(
                V5WebDavStatusState(
                    host = "cloud.anna.ru",
                    lastSynced = "2 минуты назад",
                    syncing = false,
                    error = null,
                    devices = listOf(V5WebDavDevice("tablet", "Планшет Анны", tabs = 8, updated = "сегодня в 12:10")),
                ),
            ),
            serverActions = NoServerActions,
            onBack = {},
            onShowCode = {},
            onScanCode = {},
        ) }
}

@Preview(name = "Confirm transfer · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncConfirmLightPreview() {
    VolaV5Theme(dark = false) {
        V5SyncConfirmSheet(V5SyncConfirmState("Pixel Анны", tabs = 12, bookmarks = 30), onAccept = {}, onDismiss = {})
    }
}

@Preview(name = "Confirm transfer · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncConfirmDarkPreview() {
    VolaV5Theme(dark = true) {
        V5SyncConfirmSheet(V5SyncConfirmState("Pixel Анны", tabs = 0, bookmarks = 3), onAccept = {}, onDismiss = {})
    }
}

@Preview(name = "Scan code, no camera, with «Open settings» · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncScanNoCameraDarkPreview() {
    VolaV5Theme(dark = true) { V5SyncScan(V5SyncScanStatus.NoCamera, onClose = {}) {} }
}

@Preview(name = "Scan code, not found, with «Try again» · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SyncScanNotFoundDarkPreview() {
    VolaV5Theme(dark = true) { V5SyncScan(V5SyncScanStatus.NotFound, onClose = {}) {} }
}
