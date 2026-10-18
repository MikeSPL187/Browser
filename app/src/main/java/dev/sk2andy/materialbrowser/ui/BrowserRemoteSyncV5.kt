package dev.sk2andy.materialbrowser.ui

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.sync.webdav.VolaRemoteSync
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncConnectResult
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncDevice
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncFailure
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncPort
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncState
import dev.sk2andy.materialbrowser.ui.v5.V5SyncServer
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavConnect
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavConnectActions
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavConnectState
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavDevice
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavDeviceTabs
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavError
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavStatusState
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavTab
import dev.sk2andy.materialbrowser.ui.v5.v5BarDomain

/*
 * The own server's screens (V27, canvas «х») bound to the engine's sync port. GeckoView builds
 * only, as the adapter is; a System WebView build shows no own server at all. The phrase and the
 * app password live in this screen's memory while typed, never in saved state.
 */

/** The sync port and its state as Compose state, for Settings → Sync. */
@Stable
internal class BrowserRemoteSync(val port: RemoteSyncPort, val available: Boolean) {
    var state by mutableStateOf(port.state)
        internal set

    val server: V5SyncServer
        get() = when {
            !available -> V5SyncServer.Unavailable
            !state.connected -> V5SyncServer.NotConnected
            else -> V5SyncServer.Connected(BrowserRemoteSyncRules.status(state, ::relativeTime))
        }

    private fun relativeTime(millis: Long): String =
        DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
}

/** The sync for [controller]'s build, following the port while this is on screen. */
@Composable
internal fun rememberBrowserRemoteSync(controller: BrowserController): BrowserRemoteSync {
    val context = LocalContext.current
    val sync = remember(controller) {
        val gecko = controller.browserEngineKind == AndroidBrowserEngineKind.GeckoView
        BrowserRemoteSync(if (gecko) VolaRemoteSync.port(context) else RemoteSyncPort.Unavailable, available = gecko)
    }
    DisposableEffect(sync) {
        sync.state = sync.port.state
        val handle = sync.port.addListener { state -> sync.state = state }
        onDispose { handle.cancel() }
    }
    return sync
}

/**
 * «Own server» (canvas «х1»): what is typed goes to the port on «Connect». A phrase the server
 * does not open goes to [onMismatch], with what was typed kept for «Type the phrase again».
 */
@Composable
internal fun BrowserRemoteSyncConnect(
    sync: BrowserRemoteSync,
    form: BrowserRemoteSyncForm,
    onBack: () -> Unit,
    onConnected: () -> Unit,
    onMismatch: (host: String) -> Unit,
) {
    V5WebDavConnect(
        state = form.state,
        actions = V5WebDavConnectActions(
            onBack = onBack,
            onAddress = { form.state = form.state.copy(address = it, error = null) },
            onLogin = { form.state = form.state.copy(login = it, error = null) },
            onPassword = { form.state = form.state.copy(password = it, error = null) },
            onPhrase = { form.state = form.state.copy(phrase = it) },
            onPhraseRepeat = { form.state = form.state.copy(phraseRepeat = it) },
            onConnect = {
                val typed = form.state
                form.state = typed.copy(connecting = true, error = null)
                sync.port.connect(typed.address.trim(), typed.login.trim(), typed.password, typed.phrase) { result ->
                    form.state = form.state.copy(connecting = false)
                    when (result) {
                        RemoteSyncConnectResult.Connected -> {
                            form.clear()
                            onConnected()
                        }
                        is RemoteSyncConnectResult.Failed ->
                            if (result.reason == RemoteSyncFailure.PhraseMismatch) {
                                // The phrase is typed again; the server and login stay.
                                form.state = form.state.copy(phrase = "", phraseRepeat = "")
                                onMismatch(BrowserRemoteSyncRules.host(typed.address))
                            } else {
                                form.state = form.state.copy(error = BrowserRemoteSyncRules.error(result.reason))
                            }
                    }
                }
            },
        ),
    )
}

/** The connect form, kept across the connect, mismatch and connect-again pages, but not saved. */
@Stable
internal class BrowserRemoteSyncForm {
    var state by mutableStateOf(V5WebDavConnectState())

    fun clear() {
        state = V5WebDavConnectState()
    }
}

/** Another device's tabs (canvas «х3»); a tap opens the page in a new tab here. */
@Composable
internal fun BrowserRemoteSyncDevice(
    sync: BrowserRemoteSync,
    deviceId: String,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val connected = sync.server as? V5SyncServer.Connected
    val device = connected?.state?.devices?.firstOrNull { it.id == deviceId }
    if (device == null) {
        // The device left the server (or the phone disconnected) while its page was open.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val tabs = remember(deviceId, sync.state) {
        sync.port.deviceTabs(deviceId).map { tab ->
            V5WebDavTab(tab.url, tab.title, v5BarDomain(tab.url) ?: tab.url)
        }
    }
    V5WebDavDeviceTabs(device = device, tabs = tabs, onBack = onBack, onOpen = onOpenUrl)
}

/** The device id of the page «Tabs of a device», kept across a rotation. */
@Composable
internal fun rememberRemoteSyncDeviceId() = rememberSaveable { mutableStateOf<String?>(null) }

/** Pure decisions of the own-server binding, for unit tests. */
internal object BrowserRemoteSyncRules {
    fun status(state: RemoteSyncState, relativeTime: (Long) -> String): V5WebDavStatusState = V5WebDavStatusState(
        host = state.server.orEmpty(),
        lastSynced = state.lastSyncedAtMillis?.let(relativeTime),
        syncing = state.syncing,
        error = state.lastError?.let(::error),
        devices = state.devices.map { device(it, relativeTime) },
    )

    fun device(device: RemoteSyncDevice, relativeTime: (Long) -> String) =
        V5WebDavDevice(device.id, device.name, device.tabCount, relativeTime(device.updatedAtMillis))

    fun error(failure: RemoteSyncFailure): V5WebDavError = when (failure) {
        RemoteSyncFailure.Unreachable -> V5WebDavError.Unreachable
        RemoteSyncFailure.LoginRejected -> V5WebDavError.LoginRejected
        RemoteSyncFailure.NotHttps -> V5WebDavError.NotHttps
        RemoteSyncFailure.NotWebDav -> V5WebDavError.NotWebDav
        RemoteSyncFailure.PhraseMismatch -> V5WebDavError.PhraseMismatch
        RemoteSyncFailure.NewerFormat -> V5WebDavError.NewerFormat
        RemoteSyncFailure.FolderNotFound -> V5WebDavError.FolderNotFound
        RemoteSyncFailure.ServerFull -> V5WebDavError.ServerFull
        RemoteSyncFailure.ServerBusy -> V5WebDavError.ServerBusy
        RemoteSyncFailure.ServerError -> V5WebDavError.ServerError
    }

    /** The server's host for «The phrase does not fit»: the typed address without scheme and path. */
    fun host(address: String): String =
        address.trim().substringAfter("://").substringBefore('/').substringAfterLast('@').ifBlank { address.trim() }
}
