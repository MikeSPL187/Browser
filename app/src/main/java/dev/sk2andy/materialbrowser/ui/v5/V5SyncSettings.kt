package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

object V5SyncSettingsTestTags {
    const val Page = "v5_sync_settings"
    const val ShowCode = "v5_sync_settings_show"
    const val ScanCode = "v5_sync_settings_scan"
    const val Server = "v5_sync_settings_server"
    const val Confirm = "v5_sync_confirm"
    const val ConfirmAccept = "v5_sync_confirm_accept"
}

/** The own server's part of Settings → Sync (V27): none in a build without it, a row to connect, or its state. */
sealed interface V5SyncServer {
    data object Unavailable : V5SyncServer

    data object NotConnected : V5SyncServer

    data class Connected(val state: V5WebDavStatusState) : V5SyncServer
}

class V5SyncServerActions(
    val onConnect: () -> Unit,
    val onSyncNow: () -> Unit,
    val onOpenDevice: (String) -> Unit,
    val onDisconnect: () -> Unit,
)

/**
 * Settings → Sync in v5 (V26, V27; canvas «ф» and «х2»). Once connected, the own server comes
 * first: how it synced and the other devices' tabs; the QR transfer follows, «Disconnect» closes
 * the page. Not connected, the transfer comes first and a row offers the own server. [sent] says
 * the last code shown here was taken, so the page tells it arrived.
 */
@Composable
fun V5SyncSettings(
    sent: Boolean,
    server: V5SyncServer,
    serverActions: V5SyncServerActions,
    onBack: () -> Unit,
    onShowCode: () -> Unit,
    onScanCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5SyncSettingsTestTags.Page),
    ) {
        V5PageBar(title = stringResource(R.string.v5_sync_title), onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
        ) {
            if (server is V5SyncServer.Connected) {
                V5WebDavStatus(server.state, onSyncNow = serverActions.onSyncNow, onOpenDevice = serverActions.onOpenDevice)
            }
            V5ListGroup(
                header = stringResource(R.string.v5_sync_transfer_header),
                footer = stringResource(if (sent) R.string.v5_sync_sent else R.string.v5_sync_transfer_footer),
            ) {
                V5ListRow(
                    title = stringResource(R.string.v5_sync_show_title),
                    icon = V5RowIcon(VolaIcons.Share, V5Tile.Blue),
                    trailing = V5RowTrailing.Chevron,
                    onClick = onShowCode,
                    modifier = Modifier.testTag(V5SyncSettingsTestTags.ShowCode),
                )
                V5ListRow(
                    title = stringResource(R.string.v5_sync_scan_title),
                    icon = V5RowIcon(VolaIcons.Download, V5Tile.Green),
                    trailing = V5RowTrailing.Chevron,
                    onClick = onScanCode,
                    modifier = Modifier.testTag(V5SyncSettingsTestTags.ScanCode),
                )
            }
            when (server) {
                V5SyncServer.Unavailable -> Unit
                V5SyncServer.NotConnected -> V5ListGroup(footer = stringResource(R.string.v5_sync_server_footer)) {
                    V5ListRow(
                        title = stringResource(R.string.v5_sync_server),
                        icon = V5RowIcon(VolaIcons.Sync, V5Tile.Sky),
                        trailing = V5RowTrailing.Chevron,
                        onClick = serverActions.onConnect,
                        modifier = Modifier.testTag(V5SyncSettingsTestTags.Server),
                    )
                }
                is V5SyncServer.Connected -> V5WebDavDisconnectRow(serverActions.onDisconnect)
            }
        }
    }
}

/** What a received code would add, asked before anything lands in the browser. */
data class V5SyncConfirmState(
    val deviceName: String,
    val tabs: Int,
    val bookmarks: Int,
)

/**
 * The question before a received transfer is applied (V26, security review): a code can be
 * shown by anyone, so the device's name and what it brings come first, and nothing lands
 * without «Accept».
 */
@Composable
fun V5SyncConfirmSheet(
    state: V5SyncConfirmState?,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shown = state ?: V5SyncConfirmState("", 0, 0)
    V5PromptSheet(
        visible = state != null,
        title = if (shown.deviceName.isBlank()) {
            stringResource(R.string.v5_sync_confirm_title_unnamed)
        } else {
            stringResource(R.string.v5_sync_confirm_title, shown.deviceName)
        },
        message = V5SyncRules.confirmMessage(
            tabs = pluralStringResource(R.plurals.v5_sync_confirm_tabs, shown.tabs, shown.tabs),
            bookmarks = pluralStringResource(R.plurals.v5_sync_confirm_bookmarks, shown.bookmarks, shown.bookmarks),
            hasTabs = shown.tabs > 0,
            hasBookmarks = shown.bookmarks > 0,
            nothing = stringResource(R.string.v5_sync_confirm_nothing),
        ),
        actions = listOf(
            V5PromptAction(
                label = stringResource(R.string.v5_sync_confirm_accept),
                onClick = onAccept,
                role = V5PromptRole.Primary,
                testTag = V5SyncSettingsTestTags.ConfirmAccept,
            ),
        ),
        onDismiss = onDismiss,
        testTag = V5SyncSettingsTestTags.Confirm,
    )
}
