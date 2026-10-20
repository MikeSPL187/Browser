package dev.sk2andy.materialbrowser.browser.engine

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.annotation.VisibleForTesting
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineCapabilities
import dev.sk2andy.materialbrowser.browser.BrowserBackdropBlurRegion
import dev.sk2andy.materialbrowser.browser.BrowserEngineScrollListener
import dev.sk2andy.materialbrowser.browser.BrowserEngineScrollMetrics
import dev.sk2andy.materialbrowser.browser.BrowserViewportRect
import dev.sk2andy.materialbrowser.browser.FindInPageOptions
import dev.sk2andy.materialbrowser.browser.PageDistractionsPort
import dev.sk2andy.materialbrowser.browser.SiteCertificate
import dev.sk2andy.materialbrowser.browser.TextInputOcclusionProbeMode
import dev.sk2andy.materialbrowser.browser.TextInputOcclusionProbeResult
import dev.sk2andy.materialbrowser.browser.actions.BrowserContentTargetListener
import dev.sk2andy.materialbrowser.browser.actions.WebContentTarget
import dev.sk2andy.materialbrowser.browser.gecko.BrowserEnginePreviewCapture
import dev.sk2andy.materialbrowser.browser.gecko.GeckoAndroidPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoAuthPromptListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoContentPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoContextDownloadRequest
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadCancellation
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadResponseListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadTransferListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoFilePromptListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoFindResult
import dev.sk2andy.materialbrowser.browser.gecko.GeckoFullscreenStateListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoInlineVideoGestureHapticListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoInlineVideoIdentity
import dev.sk2andy.materialbrowser.browser.gecko.GeckoInlineVideoOpenRequestListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoMediaCommand
import dev.sk2andy.materialbrowser.browser.gecko.GeckoMediaPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoMediaSessionStateListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoNavigationRequestListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoNewSessionListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPictureInPicturePreparation
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPrivacyPolicy
import dev.sk2andy.materialbrowser.browser.gecko.GeckoWebPromptListener
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import dev.sk2andy.materialbrowser.browser.protection.SiteProtectionCounts
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationPort
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEvent
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineSessionPort

/** Find in page, the part of [BrowserEngineViewPort] that FindInPageController needs. */
internal interface BrowserEngineFindPort {
    fun findInPage(
        query: String,
        forward: Boolean,
        onComplete: (GeckoFindResult?) -> Unit,
    )

    fun clearFindInPage()

    /** Whether the engine honours [setFindInPageOptions]; the find bar hides them otherwise. */
    val supportsFindInPageOptions: Boolean
        get() = false

    /** Options for the next [findInPage] calls. */
    fun setFindInPageOptions(options: FindInPageOptions) = Unit
}

/** Android view-host edge kept separate from the engine-neutral shared session port. */
internal interface BrowserEngineViewPort : BrowserEngineFindPort {
    fun setBackdropCaptureEnabled(enabled: Boolean) = Unit

    fun setBackdropBlurRegion(region: BrowserBackdropBlurRegion?) = Unit

    fun createView(context: Context): View

    fun awaitContentPresented(listener: () -> Unit)

    fun releaseView(view: View)

    fun capturePreview(
        targetWidthPx: Int,
        visibleViewHeightPx: Int,
        maximumTargetHeightPx: Int,
        onComplete: (Bitmap?) -> Unit,
    ): BrowserEnginePreviewCapture?

    fun printPage(): Boolean

    fun setDesktopMode(enabled: Boolean)

    fun scrollMetrics(): BrowserEngineScrollMetrics? = null

    fun scrollToVerticalOffset(offsetPx: Int) = Unit

    fun scrollByVerticalOffset(deltaPx: Int) = Unit

    /** Opaque Gecko state for same-device resume; never a cross-engine archive payload. */
    fun sessionStateSnapshot(): String? = null

    fun restoreSessionState(encodedState: String): Boolean = false

    fun platformViewStateSnapshot(): Bundle? = null

    fun restorePlatformViewState(state: Bundle, expectedUrl: String): Boolean = false

    /** In-memory-only renderer state used while replacing a platform view at runtime. */
    fun transientPlatformViewStateSnapshot(): Bundle? = null

    fun restoreTransientPlatformViewState(state: Bundle, expectedUrl: String): Boolean = false

    fun loadExtensionUrl(url: String): Boolean = false

    fun requestEngineFocus(view: View): Boolean = view.requestFocus()

    fun dispatchEngineGenericMotionEvent(view: View, event: MotionEvent): Boolean =
        view.dispatchGenericMotionEvent(event)

    fun dispatchEngineKeyEvent(view: View, event: KeyEvent): Boolean =
        view.dispatchKeyEvent(event)
}

/** One Android browser tab: shared commands plus the platform renderer host. */
internal interface AndroidBrowserEngineSessionPort :
    BrowserEngineSessionPort,
    BrowserEngineViewPort {
    fun setActive(active: Boolean)

    /**
     * For a tab that adopted a link preview's live page: reports that page as a navigation that
     * started (and finished, if it had), so the tab, its history and its protection catch up
     * without loading it again. Sessions that adopted nothing ignore it.
     */
    fun announceAdoptedPage() = Unit

    /** The certificate the current page came with; none over plain HTTP or before it is known. */
    fun siteCertificate(): SiteCertificate? = null

    /** Existing page icon from an engine callback; GeckoView does not expose one. */
    fun setFaviconListener(listener: ((String?, Bitmap) -> Unit)?) = Unit

    fun setMediaStateListener(listener: GeckoMediaSessionStateListener?)

    fun setInlineVideoOpenRequestListener(listener: GeckoInlineVideoOpenRequestListener?) = Unit

    fun setInlineVideoGestureHapticListener(
        listener: GeckoInlineVideoGestureHapticListener?,
    ) = Unit

    fun setFullscreenStateListener(listener: GeckoFullscreenStateListener?) = Unit

    fun setScrollListener(listener: BrowserEngineScrollListener?)

    fun setNativeVerticalScrollBarEnabled(enabled: Boolean) = Unit

    fun setContentTargetListener(listener: BrowserContentTargetListener?)

    /** Delivers an engine-originated content target through the normal callback chain. */
    @VisibleForTesting
    fun dispatchContentTargetForTesting(target: WebContentTarget) = Unit

    fun setNavigationRequestListener(listener: GeckoNavigationRequestListener?)

    fun setNewSessionListener(listener: GeckoNewSessionListener?) = Unit

    fun setDownloadResponseListener(listener: GeckoDownloadResponseListener?) = Unit

    fun startContextDownload(
        request: GeckoContextDownloadRequest,
        listener: GeckoDownloadTransferListener,
    ): GeckoDownloadCancellation? = null

    fun setFilePromptListener(listener: GeckoFilePromptListener?) = Unit

    fun setAndroidPermissionRequestListener(
        listener: GeckoAndroidPermissionRequestListener?,
    ) = Unit

    fun setContentPermissionRequestListener(
        listener: GeckoContentPermissionRequestListener?,
    ) = Unit

    fun setNotificationPermission(
        origin: String,
        decision: SitePermissionDecision,
        onComplete: (() -> Unit)? = null,
    ) {
        onComplete?.invoke()
    }

    fun setNotificationPermissionDecisionProvider(
        provider: ((String) -> SitePermissionDecision)?,
    ) = Unit

    fun setMediaPermissionRequestListener(listener: GeckoMediaPermissionRequestListener?) = Unit

    fun setAuthPromptListener(listener: GeckoAuthPromptListener?) = Unit

    fun setWebPromptListener(listener: GeckoWebPromptListener?) = Unit

    fun setVideoAutoplayBlocked(blocked: Boolean)

    fun setHttpPasswordManagerSelectionEnabled(enabled: Boolean) = Unit

    fun setAudioMuted(muted: Boolean)

    fun executeMediaCommand(command: GeckoMediaCommand)

    fun seekMedia(positionMillis: Long)

    fun notifyPictureInPictureModeChanged(inPictureInPicture: Boolean) = Unit

    fun setPictureInPicturePlaybackExpected(expected: Boolean) = Unit

    fun preparePictureInPicturePlayback(
        identity: GeckoInlineVideoIdentity,
        onResult: (GeckoPictureInPicturePreparation?) -> Unit,
    ) = onResult(null)

    fun restorePictureInPicturePresentation(onResult: (Boolean) -> Unit) = onResult(false)

    fun setInlineVideoPresentation(
        identity: GeckoInlineVideoIdentity?,
        expected: Boolean,
        onResult: (Boolean) -> Unit,
    ) = onResult(false)

    fun exitFullscreen() = Unit

    fun goToHistoryIndex(index: Int)

    fun historyUrlAtOffset(offset: Int): String?

    /**
     * Runs the bounded article extraction and returns the raw JSON object text (as read by
     * `ReaderExtractionParser.parseJson`), or null. Every engine decodes its own transport
     * encoding first, so callers never see a JSON-encoded string.
     */
    fun extractPageForReader(onComplete: (String?) -> Unit)

    /**
     * What the engine's own tracking protection stopped on the current page (v5 site protection);
     * engines without such a log, System WebView among them, answer null.
     */
    fun protectionCounts(onResult: (SiteProtectionCounts?) -> Unit) = onResult(null)

    /**
     * On-device translation of this tab's page (v5 V23). Engines without it, System WebView among
     * them, keep [PageTranslationPort.Unavailable]; the UI hides the feature by
     * [AndroidBrowserEngineCapabilities.onDeviceTranslation].
     */
    fun pageTranslation(): PageTranslationPort = PageTranslationPort.Unavailable

    /**
     * Choosing distracting items to hide on this tab's page (v5 V20). System WebView keeps
     * [PageDistractionsPort.Unavailable]; the UI hides the feature by
     * [AndroidBrowserEngineCapabilities.hideDistractions].
     */
    fun pageDistractions(): PageDistractionsPort = PageDistractionsPort.Unavailable

    fun probeTextInputOcclusion(
        viewportRect: BrowserViewportRect,
        mode: TextInputOcclusionProbeMode,
        onComplete: (TextInputOcclusionProbeResult) -> Unit,
    ) = onComplete(TextInputOcclusionProbeResult.NoFocusedTextInput)

    fun updatePrivacyPolicy(
        policy: GeckoPrivacyPolicy,
        reloadOnCookiePermissionChange: Boolean = false,
        onReady: () -> Unit = {},
    )
}

internal fun interface BrowserEngineEventSink {
    fun onEngineEvent(event: BrowserEngineEvent)
}
