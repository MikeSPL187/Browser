package dev.sk2andy.materialbrowser.browser.engine

import dev.sk2andy.materialbrowser.browser.BrowserEngineAndroidPermissionRequest
import dev.sk2andy.materialbrowser.browser.BrowserEngineAuthPromptRequest
import dev.sk2andy.materialbrowser.browser.BrowserEngineContentPermissionRequest
import dev.sk2andy.materialbrowser.browser.BrowserEngineDownloadResponse
import dev.sk2andy.materialbrowser.browser.BrowserEngineFilePromptRequest
import dev.sk2andy.materialbrowser.browser.BrowserEngineMediaPermissionRequest
import dev.sk2andy.materialbrowser.browser.BrowserEngineNavigationTarget
import dev.sk2andy.materialbrowser.browser.BrowserEngineWebPromptRequest
import dev.sk2andy.materialbrowser.browser.gecko.BrowserEnginePreparedSession
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadCancellation
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadTransferListener
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind

internal fun interface GeckoBrowserSessionStateListener {
    fun onStateChanged(state: GeckoBrowserSessionState)
}

internal data class GeckoBrowserHistoryState(
    val urls: List<String>,
    val currentIndex: Int,
    val currentTitle: String?,
)

internal fun interface GeckoBrowserHistoryStateListener {
    fun onHistoryStateChanged(state: GeckoBrowserHistoryState)
}

internal data class GeckoMainFrameNavigationRequest(
    val url: String,
    val isRedirect: Boolean,
    val hasUserGesture: Boolean,
    val isDirectNavigation: Boolean,
    val target: BrowserEngineNavigationTarget = BrowserEngineNavigationTarget.Current,
)

internal enum class GeckoNavigationRequestDecision {
    Allow,
    Deny,
}

internal fun interface GeckoNavigationRequestListener {
    fun onNavigationRequest(request: GeckoMainFrameNavigationRequest): GeckoNavigationRequestDecision
}

internal data class GeckoNewSessionRequest(
    val url: String,
    /** Unopened engine session that Candy must adopt before accepting the new window. */
    val session: BrowserEnginePreparedSession,
)

internal fun interface GeckoNewSessionListener {
    fun onNewSession(request: GeckoNewSessionRequest): Boolean
}

internal class GeckoExternalDownloadResponse(
    val metadata: BrowserEngineDownloadResponse,
    private val startTransfer: (GeckoDownloadTransferListener) -> GeckoDownloadCancellation?,
    private val discard: () -> Unit,
) : AutoCloseable {
    private var consumed = false

    fun start(listener: GeckoDownloadTransferListener): GeckoDownloadCancellation? {
        if (consumed) return null
        consumed = true
        return startTransfer(listener)
    }

    override fun close() {
        if (consumed) return
        consumed = true
        discard()
    }
}

internal fun interface GeckoDownloadResponseListener {
    /** The receiver must start or close the one-shot response. */
    fun onDownloadResponse(response: GeckoExternalDownloadResponse)
}

internal data class GeckoContextDownloadRequest(
    val url: String,
    val suggestedFileName: String? = null,
    val referrer: String? = null,
)

internal fun interface GeckoFilePromptListener {
    fun onFilePrompt(request: BrowserEngineFilePromptRequest)
}

internal fun interface GeckoAndroidPermissionRequestListener {
    fun onAndroidPermissionRequest(request: BrowserEngineAndroidPermissionRequest)
}

internal fun interface GeckoContentPermissionRequestListener {
    fun onContentPermissionRequest(request: BrowserEngineContentPermissionRequest)
}

internal fun interface GeckoMediaPermissionRequestListener {
    fun onMediaPermissionRequest(request: BrowserEngineMediaPermissionRequest)
}

internal fun interface GeckoAuthPromptListener {
    fun onAuthPrompt(request: BrowserEngineAuthPromptRequest)
}

internal fun interface GeckoWebPromptListener {
    fun onWebPrompt(request: BrowserEngineWebPromptRequest)
}

internal fun interface BrowserEnginePreviewCapture {
    fun cancel()
}

internal data class GeckoFindResult(
    val activeMatchOrdinal: Int,
    val matchCount: Int,
    val isDoneCounting: Boolean,
    /** Whether the query is on the page; true with no count means the engine has not counted. */
    val found: Boolean = matchCount > 0,
)

internal data class GeckoMediaSessionState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isFullscreen: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val currentPositionMillis: Long = 0,
    val durationMillis: Long? = null,
    val playbackRate: Float = 1f,
    val sourceUrl: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioTrackCount: Int = 0,
    val videoTrackCount: Int = 0,
    val hasInlineVideo: Boolean = false,
    val isInlineVideoPlaying: Boolean = false,
    val isInlineVideoPresented: Boolean = false,
    val inlineVideoWidth: Int = 0,
    val inlineVideoHeight: Int = 0,
    val inlineVideoDocumentNonce: String? = null,
    val inlineVideoElementNonce: String? = null,
)

internal fun interface GeckoMediaSessionStateListener {
    fun onStateChanged(state: GeckoMediaSessionState)
}

internal data class GeckoInlineVideoIdentity(
    val documentNonce: String,
    val elementNonce: String,
)

internal data class GeckoInlineVideoOpenRequest(
    val identity: GeckoInlineVideoIdentity,
    val navigationGeneration: Int,
    val expected: Boolean = true,
)

internal fun interface GeckoInlineVideoOpenRequestListener {
    fun onOpenRequested(request: GeckoInlineVideoOpenRequest)
}

internal enum class GeckoInlineVideoGestureHapticPhase(val stableId: String) {
    RubberbandStart("rubberband-start"),
    RubberbandStop("rubberband-stop"),
    Confirm("confirm"),
}

internal data class GeckoInlineVideoGestureHaptic(
    val identity: GeckoInlineVideoIdentity,
    val navigationGeneration: Int,
    val phase: GeckoInlineVideoGestureHapticPhase,
)

internal fun interface GeckoInlineVideoGestureHapticListener {
    fun onHapticRequested(haptic: GeckoInlineVideoGestureHaptic)
}

internal fun interface GeckoFullscreenStateListener {
    fun onStateChanged(fullscreen: Boolean)
}

internal enum class GeckoMediaCommand {
    Play,
    Pause,
    Stop,
}

internal data class GeckoBrowserSessionState(
    val url: String? = null,
    val title: String? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val lastNavigationSucceeded: Boolean? = null,
    val crashed: Boolean = false,
    val failureDescription: String? = null,
    val failureKind: BrowserEngineFailureKind? = null,
    val httpStatusCode: Int? = null,
)
