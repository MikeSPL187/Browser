package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import dev.sk2andy.materialbrowser.browser.FindInPageOptions
import dev.sk2andy.materialbrowser.browser.BrowserEngineScrollListener
import dev.sk2andy.materialbrowser.browser.BrowserEngineScrollMetrics
import dev.sk2andy.materialbrowser.browser.BrowserBackdropBlurRegion
import dev.sk2andy.materialbrowser.browser.BrowserViewportRect
import dev.sk2andy.materialbrowser.browser.SiteCertificate
import dev.sk2andy.materialbrowser.browser.TextInputOcclusionProbeMode
import dev.sk2andy.materialbrowser.browser.TextInputOcclusionProbeResult
import dev.sk2andy.materialbrowser.browser.actions.BrowserContentTargetListener
import dev.sk2andy.materialbrowser.browser.actions.WebContentTarget
import dev.sk2andy.materialbrowser.browser.engine.BrowserEngineContentKind
import dev.sk2andy.materialbrowser.browser.PageDistractionsPort
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import dev.sk2andy.materialbrowser.browser.protection.SiteProtectionCounts
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationPort
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebRequestError
import dev.sk2andy.materialbrowser.browser.engine.BrowserEnginePreviewCapture
import dev.sk2andy.materialbrowser.browser.engine.GeckoAndroidPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoAuthPromptListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoBrowserHistoryStateListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoBrowserSessionStateListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoContentPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoContextDownloadRequest
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadResponseListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoFilePromptListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoFindResult
import dev.sk2andy.materialbrowser.browser.engine.GeckoFullscreenStateListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoGestureHapticListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoIdentity
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoOpenRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaCommand
import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaSessionStateListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoNavigationRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoNewSessionListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoWebPromptListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadCancellation
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadTransferListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoPictureInPicturePreparation
import dev.sk2andy.materialbrowser.browser.engine.GeckoPrivacyEventSink
import dev.sk2andy.materialbrowser.browser.engine.GeckoPrivacyPolicy

internal object GeckoNavigationFailureRules {
    fun kindForErrorCode(errorCode: Int): BrowserEngineFailureKind = when (errorCode) {
        WebRequestError.ERROR_OFFLINE -> BrowserEngineFailureKind.Offline
        WebRequestError.ERROR_UNKNOWN_HOST -> BrowserEngineFailureKind.UnknownHost
        WebRequestError.ERROR_SECURITY_SSL,
        WebRequestError.ERROR_SECURITY_BAD_CERT,
        -> BrowserEngineFailureKind.InsecureConnection
        WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI,
        WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI,
        WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI,
        WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI,
        -> BrowserEngineFailureKind.DangerousSite
        else -> BrowserEngineFailureKind.Other
    }
}


/**
 * UI-facing session port. The implementation owns GeckoSession and only exposes an Android View
 * for Compose hosting, so callers cannot reach or replace the process GeckoRuntime.
 */
internal interface GeckoBrowserSession {
    val profileId: String
    val isPrivate: Boolean

    /** Whether the profile's storage is isolated; with [profileId] and [isPrivate] it names the context. */
    val isolationEnabled: Boolean get() = false

    /** Sends this session's privacy events to [sink] from now on: a tab adopting a link preview. */
    fun setPrivacyEventSink(sink: GeckoPrivacyEventSink) = Unit

    fun setStateListener(listener: GeckoBrowserSessionStateListener?)

    fun setHistoryStateListener(listener: GeckoBrowserHistoryStateListener?)

    fun setNavigationRequestListener(listener: GeckoNavigationRequestListener?)

    fun setNewSessionListener(listener: GeckoNewSessionListener?) = Unit

    fun setDownloadResponseListener(listener: GeckoDownloadResponseListener?) = Unit

    /** Fetches through Gecko so authenticated/profile/private request state stays engine-owned. */
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

    /** Keeps Gecko's stored notification decision aligned with Permission Radar. */
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

    fun setContentTargetListener(listener: BrowserContentTargetListener?)

    fun dispatchContentTargetForTesting(target: WebContentTarget) = Unit

    fun setMediaStateListener(listener: GeckoMediaSessionStateListener?)

    fun setInlineVideoOpenRequestListener(listener: GeckoInlineVideoOpenRequestListener?) = Unit

    fun setInlineVideoGestureHapticListener(
        listener: GeckoInlineVideoGestureHapticListener?,
    ) = Unit

    /** Reports the page fullscreen lifecycle independently from media metadata updates. */
    fun setFullscreenStateListener(listener: GeckoFullscreenStateListener?) = Unit

    fun setScrollListener(listener: BrowserEngineScrollListener?)

    /** Bounded document metrics used by Candy's engine-neutral scrollbar overlay. */
    fun scrollMetrics(): BrowserEngineScrollMetrics? = null

    /** Scrolls the Gecko document without synthesizing touch events. */
    fun scrollToVerticalOffset(offsetPx: Int) = Unit

    /** Scrolls the Gecko document by a relative hardware-input delta. */
    fun scrollByVerticalOffset(deltaPx: Int) = Unit

    fun setVideoAutoplayBlocked(blocked: Boolean)

    fun siteCertificate(): SiteCertificate? = null

    /** Allows explicit Gecko login selection for cleartext HTTP origins. */
    fun setHttpPasswordManagerSelectionEnabled(enabled: Boolean) = Unit

    /** Mutes current and future Gecko media sessions without changing tab activity. */
    fun setAudioMuted(muted: Boolean)

    fun executeMediaCommand(command: GeckoMediaCommand)

    fun seekMedia(positionMillis: Long)

    /** Keeps Gecko's compositor surface in sync with Android picture-in-picture mode. */
    fun notifyPictureInPictureModeChanged(inPictureInPicture: Boolean)

    /** Keeps page media aligned with the user's PiP play or pause intent. */
    fun setPictureInPicturePlaybackExpected(expected: Boolean)

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

    /** Requests that Gecko leave DOM fullscreen through its public session API. */
    fun exitFullscreen() = Unit

    /** Reports whether this is Candy's selected tab so WebExtensions receive active-tab events. */
    fun setActive(active: Boolean)

    /** Binds Gecko's session delegates to Candy's stable tab identity. */
    fun bindExtensionTab(tabId: String, generation: Long) = Unit

    /** Binds Topping authorization to this exact renderer session and content purpose. */
    fun bindToppingSession(tabId: String, contentKind: BrowserEngineContentKind) = Unit

    /** Applies a compositor-owned backdrop region without replacing Gecko's SurfaceView. */
    fun setBackdropBlurRegion(region: BrowserBackdropBlurRegion?) = Unit

    /** Creates and binds the one View currently rendering this session. */
    fun createView(context: Context): View

    /**
     * Like [createView], but the page is drawn through a TextureView, so it fades, clips and moves
     * with the views around it. It costs more than the default SurfaceView: only for a page that
     * flies inside a card (the link preview), never for a tab.
     */
    fun createAnimatableView(context: Context): View = createView(context)

    /**
     * Runs after Gecko has both started its compositor and painted valid page content.
     * A newer request replaces an older pending request for this session.
     */
    fun awaitContentPresented(listener: () -> Unit)

    /** Releases a View returned by [createView] without closing the navigable session. */
    fun releaseView(view: View)

    /** Captures the currently rendered viewport and returns a bounded tab-preview bitmap. */
    fun capturePreview(
        targetWidthPx: Int,
        visibleViewHeightPx: Int,
        maximumTargetHeightPx: Int,
        onComplete: (Bitmap?) -> Unit,
    ): BrowserEnginePreviewCapture?

    fun findInPage(
        query: String,
        forward: Boolean,
        onComplete: (GeckoFindResult?) -> Unit,
    )

    fun clearFindInPage()

    /** Options for the next [findInPage] calls. */
    fun setFindInPageOptions(options: FindInPageOptions) = Unit

    /** Extracts bounded article JSON through Candy's internal Gecko content-script bridge. */
    fun extractPageForReader(onComplete: (String?) -> Unit)

    /** What Gecko's content blocking stopped on the current page; null when unknown. */
    fun protectionCounts(onResult: (SiteProtectionCounts?) -> Unit) = onResult(null)

    /** On-device translation of this session's page (v5 V23). */
    fun pageTranslation(): PageTranslationPort = PageTranslationPort.Unavailable

    /** Choosing distracting items to hide on this session's page (v5 V20). */
    fun pageDistractions(): PageDistractionsPort = PageDistractionsPort.Unavailable

    /** Checks one committed document for a text editor hidden by browser chrome. */
    fun probeTextInputOcclusion(
        viewportRect: BrowserViewportRect,
        mode: TextInputOcclusionProbeMode,
        onComplete: (TextInputOcclusionProbeResult) -> Unit,
    ) = onComplete(TextInputOcclusionProbeResult.NoFocusedTextInput)

    /** Opens Gecko's Android print flow for the current document. */
    fun printPage(): Boolean

    /** Applies Gecko's native desktop/mobile UA and viewport policy for the next reload. */
    fun setDesktopMode(enabled: Boolean)

    /** Returns Gecko's own opaque session payload for a regular tab, if Gecko has produced one. */
    fun sessionStateSnapshot(): String? = null

    /** Restores an opaque payload only after the owning regular profile/session has been validated. */
    fun restoreSessionState(encodedState: String): Boolean = false

    /** Loads a validated HTTP(S) URL. Returns false when validation rejects the input. */
    fun loadUrl(url: String): Boolean

    /** Replaces the current history entry with a validated HTTP(S) URL. */
    fun replaceHistoryUrl(url: String): Boolean = loadUrl(url)

    /** Retries a failed URL without duplicating a matching committed history entry. */
    fun retryFailedPage(url: String): Boolean = loadUrl(url)

    /** Loads an already host-validated moz-extension options URL through startup gates. */
    fun loadExtensionUrl(url: String): Boolean = false

    fun updatePrivacyPolicy(
        policy: GeckoPrivacyPolicy,
        reloadOnCookiePermissionChange: Boolean = false,
        onReady: () -> Unit = {},
    ) = onReady()

    fun goBack()

    fun goForward()

    fun goToHistoryIndex(index: Int)

    /** Returns a known history target relative to the current entry without navigating. */
    fun historyUrlAtOffset(offset: Int): String?

    fun reload()

    fun stop()

    /** Permanently releases this session. Private session state must never be persisted. */
    fun close()
}
