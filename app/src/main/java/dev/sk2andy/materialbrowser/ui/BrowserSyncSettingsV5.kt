package dev.sk2andy.materialbrowser.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.transfer.AndroidLocalTransferPort
import dev.sk2andy.materialbrowser.browser.transfer.LocalTransferViewfinder
import dev.sk2andy.materialbrowser.browser.transfer.applyTransfer
import dev.sk2andy.materialbrowser.browser.transfer.localTransferDeviceName
import dev.sk2andy.materialbrowser.browser.transfer.localTransferPayload
import dev.sk2andy.materialbrowser.browser.transfer.localTransferQrBitmap
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferFailure
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferHandle
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferMergeRules
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferOfferState
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayload
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayloadCodec
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferReceiveResult
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SyncColors
import dev.sk2andy.materialbrowser.ui.v5.V5SyncConfirmSheet
import dev.sk2andy.materialbrowser.ui.v5.V5SyncConfirmState
import dev.sk2andy.materialbrowser.ui.v5.V5SyncDone
import dev.sk2andy.materialbrowser.ui.v5.V5SyncDoneActions
import dev.sk2andy.materialbrowser.ui.v5.V5SyncDoneState
import dev.sk2andy.materialbrowser.ui.v5.V5SyncScan
import dev.sk2andy.materialbrowser.ui.v5.V5SyncScanStatus
import dev.sk2andy.materialbrowser.ui.v5.V5SyncSettings
import dev.sk2andy.materialbrowser.ui.v5.V5SyncShowCode
import dev.sk2andy.materialbrowser.ui.v5.V5SyncShowState

/*
 * Settings → Sync in v5 (V26, canvas «ф») bound to the local transfer: show a code, scan one,
 * confirm, done. The sync server keeps its page ([serverPage]). The requirements of the transfer's
 * security review hold here: the code's screen is FLAG_SECURE and has no «Copy», a received
 * transfer lands only after «Accept», tabs arrive unloaded, and every offer or receive is
 * cancelled when its screen goes.
 */

@Composable
internal fun BrowserSyncSettingsV5(
    controller: BrowserController,
    onBack: () -> Unit,
    serverPage: @Composable (onBack: () -> Unit) -> Unit,
    onOpenTabOverview: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
) {
    var page by rememberSaveable { mutableStateOf(BrowserSyncRules.Page.Main) }
    var sent by rememberSaveable { mutableStateOf(false) }
    var done by remember { mutableStateOf<BrowserSyncRules.Applied?>(null) }
    var undoFailed by remember { mutableStateOf(false) }
    val toMain = { page = BrowserSyncRules.Page.Main }
    when (page) {
        BrowserSyncRules.Page.Main -> V5SyncSettings(
            sent = sent,
            onBack = onBack,
            onShowCode = {
                sent = false
                page = BrowserSyncRules.Page.Show
            },
            onScanCode = {
                sent = false
                page = BrowserSyncRules.Page.Scan
            },
            onServer = { page = BrowserSyncRules.Page.Server },
        )
        BrowserSyncRules.Page.Server -> {
            BackHandler(onBack = toMain)
            serverPage(toMain)
        }
        BrowserSyncRules.Page.Show -> {
            BackHandler(onBack = toMain)
            SyncShowCode(controller, onBack = toMain) {
                sent = true
                page = BrowserSyncRules.Page.Main
            }
        }
        BrowserSyncRules.Page.Scan -> {
            BackHandler(onBack = toMain)
            SyncScan(controller, onClose = toMain) { result ->
                done = result
                undoFailed = false
                page = BrowserSyncRules.Page.Done
            }
        }
        BrowserSyncRules.Page.Done -> {
            val applied = done
            if (applied == null) {
                // The result does not outlive the process; the page it was on goes with it.
                LaunchedEffect(Unit) { toMain() }
            } else {
                val finish = {
                    done = null
                    undoFailed = false
                    toMain()
                }
                BackHandler(onBack = finish)
                V5SyncDone(
                    state = applied.state.copy(undoFailed = undoFailed),
                    actions = V5SyncDoneActions(
                        // The tabs came as one group of the tab overview; the bookmarks into Favorites.
                        onOpenGroup = {
                            finish()
                            onOpenTabOverview()
                        },
                        onOpenFavorites = {
                            finish()
                            onOpenFavorites()
                        },
                        onDone = finish,
                        // A failed undo stays on the page and says so; the button goes, as trying
                        // again would meet the same favorites.
                        onUndo = if (undoFailed) null else {
                            { if (applied.undo()) finish() else undoFailed = true }
                        },
                    ),
                )
            }
        }
    }
}

/** «Show code»: one offer at a time, a new code when the old one expires, cancelled on leaving. */
@Composable
private fun SyncShowCode(controller: BrowserController, onBack: () -> Unit, onSent: () -> Unit) {
    val context = LocalContext.current
    val port = remember(context) { AndroidLocalTransferPort.create(context.applicationContext) }
    val payload = remember(controller) { controller.localTransferPayload(localTransferDeviceName(context)) }
    val sizePx = with(LocalDensity.current) { V5Sizes.syncCode.roundToPx() }
    val currentOnSent by rememberUpdatedState(onSent)
    var code by remember { mutableStateOf<ImageBitmap?>(null) }
    var noWifi by remember { mutableStateOf(false) }
    // Each round is one offer: a code that expired or met too many wrong requests is replaced,
    // and Wi-Fi coming back starts one where there was none.
    var round by remember { mutableIntStateOf(0) }
    SecureWindow()
    WifiArrivals { if (noWifi) round++ }
    DisposableEffect(port, round) {
        code = null
        noWifi = false
        val handle = port.offer(payload) { state ->
            when (state) {
                is LocalTransferOfferState.Waiting -> code = localTransferQrBitmap(
                    state.code,
                    sizePx,
                    V5SyncColors.codeInk.toArgb(),
                    V5SyncColors.codePaper.toArgb(),
                ).asImageBitmap()
                LocalTransferOfferState.Sent -> currentOnSent()
                is LocalTransferOfferState.Failed ->
                    if (BrowserSyncRules.offersAgain(state.reason)) round++ else noWifi = true
            }
        }
        onDispose { handle.cancel() }
    }
    V5SyncShowCode(
        state = V5SyncShowState(
            code = code,
            onWifi = !noWifi,
            // Android names the Wi-Fi only with the location permission; the page says «the same
            // Wi-Fi» instead of asking for it.
            networkName = null,
            tabs = payload.tabs.size.coerceAtMost(LocalTransferPayloadCodec.MAX_TABS),
            bookmarks = payload.bookmarks.size.coerceAtMost(LocalTransferPayloadCodec.MAX_BOOKMARKS),
        ),
        onBack = onBack,
    )
}

/** «Scan code»: the camera after its permission, then the confirm sheet before anything lands. */
@Composable
private fun SyncScan(controller: BrowserController, onClose: () -> Unit, onApplied: (BrowserSyncRules.Applied) -> Unit) {
    val context = LocalContext.current
    val port = remember(context) { AndroidLocalTransferPort.create(context.applicationContext) }
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var status by rememberSaveable { mutableStateOf(V5SyncScanStatus.Scanning) }
    var received by remember { mutableStateOf<LocalTransferPayload?>(null) }
    var handle by remember { mutableStateOf<LocalTransferHandle?>(null) }
    var applying by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed
        if (!allowed) status = V5SyncScanStatus.NoCamera
    }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    DisposableEffect(port) { onDispose { handle?.cancel() } }
    val plan = remember(received) {
        received?.let { payload -> LocalTransferMergeRules.plan(payload, controller.favorites.map { it.url }) }
    }
    V5SyncScan(status = status, onClose = onClose) {
        if (granted) {
            LocalTransferViewfinder(
                onCode = { scanned ->
                    if (status == V5SyncScanStatus.Scanning) {
                        status = V5SyncScanStatus.Receiving
                        handle = port.receive(scanned) { result ->
                            handle = null
                            when (result) {
                                is LocalTransferReceiveResult.Received -> received = result.payload
                                is LocalTransferReceiveResult.Failed -> status = BrowserSyncRules.scanStatus(result.reason)
                            }
                        }
                    }
                },
                onNoCamera = { status = V5SyncScanStatus.NoCamera },
            )
        }
    }
    V5SyncConfirmSheet(
        state = plan?.takeIf { !applying }?.let { V5SyncConfirmState(it.tabGroupDeviceName, it.tabs.size, it.bookmarks.size) },
        onAccept = {
            val payload = received
            if (plan != null && payload != null && !applying) {
                applying = true
                val applied = controller.applyTransfer(plan)
                onApplied(
                    BrowserSyncRules.Applied(
                        state = V5SyncDoneState(
                            deviceName = plan.tabGroupDeviceName,
                            tabs = applied.tabsOpened,
                            bookmarksAdded = applied.bookmarksAdded,
                            bookmarksExisting = payload.bookmarks.size - plan.bookmarks.size,
                            bookmarksSkipped = applied.bookmarksSkipped,
                        ),
                        undo = applied.undo,
                    ),
                )
            }
        },
        onDismiss = {
            received = null
            onClose()
        },
    )
}

/** Keeps the window out of screenshots, screen recording and Recents while the code is shown. */
@Composable
private fun SecureWindow() {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val alreadySecure = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        if (!alreadySecure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!alreadySecure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/** Calls [onAvailable] on the main thread whenever a Wi-Fi network comes up while this is shown. */
@Composable
private fun WifiArrivals(onAvailable: () -> Unit) {
    val context = LocalContext.current
    val current by rememberUpdatedState(onAvailable)
    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = current()
        }
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val registered = manager != null &&
            runCatching { manager.registerNetworkCallback(request, callback, Handler(Looper.getMainLooper())) }.isSuccess
        onDispose { if (registered) runCatching { manager?.unregisterNetworkCallback(callback) } }
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Pure decisions of the sync binding, for unit tests. */
internal object BrowserSyncRules {
    enum class Page { Main, Server, Show, Scan, Done }

    /** A transfer that landed: what the done page shows and how to take it back. */
    class Applied(val state: V5SyncDoneState, val undo: () -> Boolean)

    /** Whether a failed offer is simply replaced by a new code rather than shown as «no Wi-Fi». */
    fun offersAgain(reason: LocalTransferFailure): Boolean =
        reason == LocalTransferFailure.Expired || reason == LocalTransferFailure.TooManyAttempts

    /** What the scan screen says about a failed receive. */
    fun scanStatus(reason: LocalTransferFailure): V5SyncScanStatus = when (reason) {
        LocalTransferFailure.Expired -> V5SyncScanStatus.CodeExpired
        LocalTransferFailure.Unreachable, LocalTransferFailure.Unavailable -> V5SyncScanStatus.NotFound
        LocalTransferFailure.InvalidCode,
        LocalTransferFailure.TooManyAttempts,
        LocalTransferFailure.Corrupt,
        LocalTransferFailure.TooLarge,
        -> V5SyncScanStatus.Failed
    }
}
