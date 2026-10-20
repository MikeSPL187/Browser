package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
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
import dev.sk2andy.materialbrowser.browser.userscript.UserScript
import dev.sk2andy.materialbrowser.browser.userscript.UserScriptMenuCommand
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineCapabilities
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import dev.sk2andy.materialbrowser.browser.DnsOverHttpsSettings
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.WebRtcProtectionMode
import dev.sk2andy.materialbrowser.browser.engine.AndroidBrowserEngineFactory
import dev.sk2andy.materialbrowser.browser.engine.BrowserEngineContentKind
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import dev.sk2andy.materialbrowser.browser.protection.SiteProtectionCounts
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationPort
import dev.sk2andy.materialbrowser.browser.translation.TranslationModelsPort
import dev.sk2andy.materialbrowser.browser.engine.BrowserWebContentColorScheme
import dev.sk2andy.materialbrowser.browser.PageDistractionsPort
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineCommand
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineCommands
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineCommandType
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEvent
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import dev.sk2andy.materialbrowser.browser.engine.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.engine.BrowserEngineEventSink
import dev.sk2andy.materialbrowser.browser.engine.BrowserEnginePreviewCapture
import dev.sk2andy.materialbrowser.browser.engine.GeckoAndroidPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoAuthPromptListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoContentPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoContextDownloadRequest
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadResponseListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoFilePromptListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoFindResult
import dev.sk2andy.materialbrowser.browser.engine.GeckoFullscreenStateListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoIdentity
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoOpenRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaCommand
import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaPermissionRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaSessionStateListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoNavigationRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoNewSessionListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoWebPromptListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoBrowserSessionState
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadCancellation
import dev.sk2andy.materialbrowser.browser.engine.GeckoDownloadTransferListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoPictureInPicturePreparation
import dev.sk2andy.materialbrowser.browser.engine.GeckoPrivacyEventSink
import dev.sk2andy.materialbrowser.browser.engine.GeckoPrivacyPolicy
import dev.sk2andy.materialbrowser.browser.engine.GeckoBrowsingData
import dev.sk2andy.materialbrowser.browser.engine.GeckoCandyTrailHistoryEventSink
import dev.sk2andy.materialbrowser.browser.engine.GeckoCandyTrailHistoryTracker
import dev.sk2andy.materialbrowser.browser.engine.BrowserEnginePreparedSession
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionActionKey
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionSessionIdentity
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionChromeHost
import dev.sk2andy.materialbrowser.browser.engine.GeckoToppingInteractionDelegate
import dev.sk2andy.materialbrowser.browser.engine.GeckoToppingHostState

internal class GeckoPreparedSession(
    val session: GeckoSession,
) : BrowserEnginePreparedSession

/** Creates Gecko-backed ports without exposing GeckoRuntime or GeckoSession to browser chrome. */
internal class GeckoBrowserEngineSessionFactory(
    private val runtime: GeckoRuntimeHandle,
) : AndroidBrowserEngineFactory {
    override val kind = AndroidBrowserEngineKind.GeckoView

    override val capabilities = AndroidBrowserEngineCapabilities.GeckoView
    override val runtimeVersionName: String = org.mozilla.geckoview.BuildConfig.MOZ_APP_VERSION
    override val translationModels: TranslationModelsPort by lazy { GeckoTranslationModels() }
    private val extensionSessionGenerations = mutableMapOf<String, Long>()
    private val extensionSessionIdentities = mutableMapOf<String, GeckoExtensionSessionIdentity>()
    private val preparedSessions = mutableMapOf<String, GeckoSession>()
    private val adoptedSessions = mutableMapOf<String, GeckoBrowserSession>()
    private val sessions = mutableMapOf<GeckoBrowserSession, GeckoBrowserEngineSessionAdapter>()

    constructor(context: Context) : this(GeckoRuntimeOwner.getOrCreate(context.applicationContext))

    @UiThread
    override fun reconcileToppings(scripts: List<UserScript>) {
        runtime.toppings.reconcile(scripts)
    }

    override fun setToppingHostStateListener(listener: (GeckoToppingHostState) -> Unit) {
        runtime.toppings.setStateListener(listener)
    }

    override fun setToppingInteractionDelegate(delegate: GeckoToppingInteractionDelegate) {
        runtime.toppings.setInteractionDelegate(delegate)
    }

    override fun invokeToppingMenuCommand(command: UserScriptMenuCommand) {
        runtime.toppings.invokeMenuCommand(command)
    }

    override fun clearToppingValues(scriptId: String) {
        runtime.toppings.clearValues(scriptId)
    }

    override fun clearBrowsingData(
        data: GeckoBrowsingData,
        onComplete: (Boolean) -> Unit,
    ) = runtime.clearBrowsingData(data, onComplete)

    override fun clearAllData(onComplete: (Boolean) -> Unit) = runtime.clearAllData(onComplete)

    override fun clearSiteData(baseDomain: String, onComplete: (Boolean) -> Unit) =
        runtime.clearSiteData(baseDomain, onComplete)

    override fun requestProfileDataDeletion(profileId: String): Boolean =
        runtime.requestProfileDataDeletion(profileId)

    @UiThread
    override fun setBlockThirdPartyCookies(blocked: Boolean) {
        runtime.setBlockThirdPartyCookies(blocked)
    }

    @UiThread
    override fun setWebRtcProtectionMode(mode: WebRtcProtectionMode) {
        val sessionsToReload = sessions.values.toList()
        runtime.setWebRtcProtectionMode(mode) {
            sessionsToReload.forEach { session ->
                session.execute(BrowserEngineCommands.reload())
            }
        }
    }

    @UiThread
    override fun setDnsOverHttpsSettings(settings: DnsOverHttpsSettings) {
        runtime.setDnsOverHttpsSettings(settings)
    }

    @UiThread
    override fun setHttpsOnlyMode(mode: HttpsOnlyMode) {
        runtime.setHttpsOnlyMode(mode)
    }

    @UiThread
    override fun setGlobalPrivacyControl(enabled: Boolean) {
        runtime.setGlobalPrivacyControl(enabled)
    }

    @UiThread
    override fun setFingerprintingProtectionExceptions(hosts: Set<String>) {
        runtime.setFingerprintingProtectionExceptions(hosts)
    }

    override fun supportsFingerprintingException(host: String): Boolean =
        GeckoFingerprintingExceptions.supports(host)

    @UiThread
    override fun setWebContentFontSizeFactor(factor: Float) {
        runtime.setWebContentFontSizeFactor(factor)
    }

    @UiThread
    override fun setWebContentColorScheme(colorScheme: BrowserWebContentColorScheme) {
        runtime.setWebContentColorScheme(colorScheme)
    }

    @UiThread
    override fun onConfigurationChanged(configuration: Configuration) {
        runtime.onConfigurationChanged(configuration)
    }

    @UiThread
    override fun setExtensionChromeHost(host: GeckoExtensionChromeHost?) {
        runtime.extensions.setChromeHost(host)
    }

    @UiThread
    override fun clickExtensionAction(key: GeckoExtensionActionKey): Boolean =
        runtime.extensions.clickChromeAction(key)

    @UiThread
    override fun dismissExtensionPopup() {
        runtime.extensions.dismissChromePopup()
    }

    @UiThread
    override fun notifySelectedExtensionTabChanged() {
        runtime.extensions.onSelectedChromeSessionChanged()
    }

    override fun extensionSessionIdentity(tabId: String): GeckoExtensionSessionIdentity? =
        extensionSessionIdentities[tabId]

    /** Prepares an unopened Gecko-owned session for the next [create] of this Candy tab. */
    @UiThread
    override fun prepareSession(
        tabId: String,
        session: BrowserEnginePreparedSession,
    ): Boolean {
        val geckoSession = (session as? GeckoPreparedSession)?.session ?: return false
        if (
            tabId.isBlank() ||
            geckoSession.isOpen ||
            extensionSessionIdentities.containsKey(tabId) ||
            preparedSessions.containsKey(tabId)
        ) {
            return false
        }
        preparedSessions[tabId] = geckoSession
        return true
    }

    /**
     * Hands a link preview's live session to the next [create] of tab [tabId], which adopts it
     * as it is: no reload, the tab's own listeners, protection, extensions and toppings. Only a
     * preview of the tab's own context (profile, isolation, private) can be adopted.
     */
    @UiThread
    override fun prepareAdoption(
        tabId: String,
        preview: AndroidBrowserEngineSessionPort,
        profileId: String,
        isolationEnabled: Boolean,
        isPrivate: Boolean,
    ): Boolean {
        val adapter = preview as? GeckoBrowserEngineSessionAdapter ?: return false
        if (
            tabId.isBlank() ||
            extensionSessionIdentities.containsKey(tabId) ||
            preparedSessions.containsKey(tabId) ||
            adoptedSessions.containsKey(tabId) ||
            !adapter.sharesContext(profileId, isolationEnabled, isPrivate)
        ) {
            return false
        }
        val session = adapter.detachForAdoption() ?: return false
        extensionSessionIdentities.remove(adapter.tabId)
        extensionSessionGenerations.remove(adapter.tabId)
        adoptedSessions[tabId] = session
        return true
    }

    override fun createExtensionPopupView(
        context: Context,
        session: BrowserEnginePreparedSession,
    ): View? {
        val geckoSession = (session as? GeckoPreparedSession)?.session ?: return null
        return GeckoView(context).also { view -> view.setSession(geckoSession) }
    }

    override fun releaseExtensionPopupView(view: View) {
        (view as? GeckoView)?.releaseSession()
    }

    @UiThread
    override fun create(
        tabId: String,
        profileId: String,
        isolationEnabled: Boolean,
        isPrivate: Boolean,
        contentKind: BrowserEngineContentKind,
        privacyPolicy: GeckoPrivacyPolicy,
        privacyEventSink: GeckoPrivacyEventSink,
        trailHistoryEventSink: GeckoCandyTrailHistoryEventSink,
        eventSink: BrowserEngineEventSink,
    ): AndroidBrowserEngineSessionPort {
        require(tabId.isNotBlank()) { "A Gecko engine session needs a tab ID" }
        val adoptedSession = adoptedSessions.remove(tabId)?.also { adopted ->
            adopted.setPrivacyEventSink(privacyEventSink)
            adopted.updatePrivacyPolicy(privacyPolicy)
        }
        val preparedSession = preparedSessions.remove(tabId)
        val session = adoptedSession ?: preparedSession?.let { prepared ->
            runtime.adoptExtensionSession(
                session = prepared,
                profileId = profileId,
                isolationEnabled = isolationEnabled,
                isPrivate = isPrivate,
                privacyPolicy = privacyPolicy,
                privacyEventSink = privacyEventSink,
            ) ?: error("Prepared extension session does not match Vola tab context")
        } ?: runtime.createSession(
            profileId = profileId,
            isolationEnabled = isolationEnabled,
            isPrivate = isPrivate,
            privacyPolicy = privacyPolicy,
            privacyEventSink = privacyEventSink,
        )
        val generation = extensionSessionGenerations.getOrDefault(tabId, 0L) + 1L
        extensionSessionGenerations[tabId] = generation
        extensionSessionIdentities[tabId] = GeckoExtensionSessionIdentity(
            tabId = tabId,
            profileId = profileId,
            isPrivate = isPrivate,
            generation = generation,
            isolationEnabled = isolationEnabled,
        )
        session.bindExtensionTab(tabId, generation)
        session.bindToppingSession(tabId, contentKind)
        return GeckoBrowserEngineSessionAdapter(
            tabId = tabId,
            session = session,
            eventSink = eventSink,
            contentKind = contentKind,
            adopted = adoptedSession != null,
            trailHistoryEventSink = trailHistoryEventSink,
            onActiveChanged = { active ->
                runtime.toppings.setActiveTab(tabId.takeIf { active })
            },
            onClosed = { sessions.remove(session) },
        ).also { adapter -> sessions[session] = adapter }
    }
}

/**
 * Thin Gecko adapter for the shared session contract.
 *
 * It owns neither browser tabs nor chrome state. Those stay in BrowserSessionController; this
 * class only translates commands, renderer binding and Gecko lifecycle callbacks.
 */
internal class GeckoBrowserEngineSessionAdapter(
    override val tabId: String,
    private val session: GeckoBrowserSession,
    private val eventSink: BrowserEngineEventSink,
    private val contentKind: BrowserEngineContentKind = BrowserEngineContentKind.RegularTab,
    /** The session is a link preview's live page that this tab took over ([announceAdoptedPage]). */
    private val adopted: Boolean = false,
    trailHistoryEventSink: GeckoCandyTrailHistoryEventSink =
        GeckoCandyTrailHistoryEventSink { _, _ -> },
    private val onActiveChanged: (Boolean) -> Unit = {},
    private val onClosed: () -> Unit = {},
) : AndroidBrowserEngineSessionPort {
    private var previousState = GeckoBrowserSessionState()
    private var closed = false
    private var inPictureInPicture = false
    private var stopRequested = false
    private val trailHistoryTracker = GeckoCandyTrailHistoryTracker(tabId) { event ->
        trailHistoryEventSink.onHistoryEvent(this, event)
    }

    init {
        require(tabId.isNotBlank()) { "A Gecko engine session needs a tab ID" }
        session.setStateListener(::onStateChanged)
        session.setHistoryStateListener(trailHistoryTracker::onHistoryStateChanged)
    }

    @UiThread
    override fun execute(command: BrowserEngineCommand) {
        if (closed) return
        when (command.type) {
            BrowserEngineCommandType.Load -> load(requireNotNull(command.address))
            BrowserEngineCommandType.ReplaceHistory -> replaceHistory(
                requireNotNull(command.address),
            )
            BrowserEngineCommandType.RetryFailedPage -> retryFailedPage(
                requireNotNull(command.address),
            )
            BrowserEngineCommandType.Back -> session.goBack()
            BrowserEngineCommandType.Forward -> session.goForward()
            BrowserEngineCommandType.Reload -> session.reload()
            BrowserEngineCommandType.Stop -> {
                stopRequested = true
                session.stop()
            }
            BrowserEngineCommandType.Close -> close()
        }
    }

    @UiThread
    override fun createView(context: Context): View {
        check(!closed) { "Cannot bind a closed browser engine session" }
        // The link preview's page flies, fades and clips with its card; tabs keep the faster surface.
        return if (contentKind == BrowserEngineContentKind.LinkPeek) {
            session.createAnimatableView(context)
        } else {
            session.createView(context)
        }
    }

    @UiThread
    override fun setBackdropBlurRegion(region: BrowserBackdropBlurRegion?) {
        if (!closed) session.setBackdropBlurRegion(region)
    }

    @UiThread
    override fun awaitContentPresented(listener: () -> Unit) {
        if (!closed) session.awaitContentPresented(listener)
    }

    @UiThread
    override fun releaseView(view: View) {
        session.releaseView(view)
    }

    @UiThread
    override fun setActive(active: Boolean) {
        if (!closed) {
            session.setActive(active)
            onActiveChanged(active)
        }
    }

    @UiThread
    override fun setMediaStateListener(listener: GeckoMediaSessionStateListener?) {
        session.setMediaStateListener(if (closed) null else listener)
    }

    @UiThread
    override fun setInlineVideoOpenRequestListener(
        listener: GeckoInlineVideoOpenRequestListener?,
    ) {
        session.setInlineVideoOpenRequestListener(if (closed) null else listener)
    }

    @UiThread
    override fun setFullscreenStateListener(listener: GeckoFullscreenStateListener?) {
        session.setFullscreenStateListener(if (closed) null else listener)
    }

    @UiThread
    override fun setScrollListener(listener: BrowserEngineScrollListener?) {
        session.setScrollListener(if (closed) null else listener)
    }

    @UiThread
    override fun scrollMetrics(): BrowserEngineScrollMetrics? =
        session.scrollMetrics().takeUnless { closed }

    @UiThread
    override fun scrollToVerticalOffset(offsetPx: Int) {
        if (!closed) session.scrollToVerticalOffset(offsetPx)
    }

    @UiThread
    override fun scrollByVerticalOffset(deltaPx: Int) {
        if (!closed) session.scrollByVerticalOffset(deltaPx)
    }

    @UiThread
    override fun setContentTargetListener(listener: BrowserContentTargetListener?) {
        session.setContentTargetListener(if (closed) null else listener)
    }

    override fun requestEngineFocus(view: View): Boolean =
        (view as? CandyGeckoView)?.requestEngineFocus() ?: view.requestFocus()

    override fun dispatchEngineGenericMotionEvent(view: View, event: MotionEvent): Boolean =
        (view as? CandyGeckoView)?.dispatchEngineGenericMotionEvent(event)
            ?: view.dispatchGenericMotionEvent(event)

    override fun dispatchEngineKeyEvent(view: View, event: KeyEvent): Boolean =
        (view as? CandyGeckoView)?.dispatchEngineKeyEvent(event) ?: view.dispatchKeyEvent(event)

    @VisibleForTesting
    override fun dispatchContentTargetForTesting(target: WebContentTarget) {
        if (!closed) session.dispatchContentTargetForTesting(target)
    }

    @UiThread
    override fun setNavigationRequestListener(listener: GeckoNavigationRequestListener?) {
        session.setNavigationRequestListener(if (closed) null else listener)
    }

    @UiThread
    override fun setNewSessionListener(listener: GeckoNewSessionListener?) {
        session.setNewSessionListener(if (closed) null else listener)
    }

    @UiThread
    override fun setDownloadResponseListener(listener: GeckoDownloadResponseListener?) {
        session.setDownloadResponseListener(if (closed) null else listener)
    }

    override fun startContextDownload(
        request: GeckoContextDownloadRequest,
        listener: GeckoDownloadTransferListener,
    ): GeckoDownloadCancellation? = if (closed) null else session.startContextDownload(request, listener)

    @UiThread
    override fun setFilePromptListener(listener: GeckoFilePromptListener?) {
        session.setFilePromptListener(if (closed) null else listener)
    }

    @UiThread
    override fun setAndroidPermissionRequestListener(
        listener: GeckoAndroidPermissionRequestListener?,
    ) {
        session.setAndroidPermissionRequestListener(if (closed) null else listener)
    }

    @UiThread
    override fun setContentPermissionRequestListener(
        listener: GeckoContentPermissionRequestListener?,
    ) {
        session.setContentPermissionRequestListener(if (closed) null else listener)
    }

    @UiThread
    override fun setNotificationPermission(
        origin: String,
        decision: SitePermissionDecision,
        onComplete: (() -> Unit)?,
    ) {
        if (closed) onComplete?.invoke()
        else session.setNotificationPermission(origin, decision, onComplete)
    }

    @UiThread
    override fun setNotificationPermissionDecisionProvider(
        provider: ((String) -> SitePermissionDecision)?,
    ) {
        session.setNotificationPermissionDecisionProvider(if (closed) null else provider)
    }

    @UiThread
    override fun setMediaPermissionRequestListener(listener: GeckoMediaPermissionRequestListener?) {
        session.setMediaPermissionRequestListener(if (closed) null else listener)
    }

    @UiThread
    override fun setAuthPromptListener(listener: GeckoAuthPromptListener?) {
        session.setAuthPromptListener(if (closed) null else listener)
    }

    @UiThread
    override fun setWebPromptListener(listener: GeckoWebPromptListener?) {
        session.setWebPromptListener(if (closed) null else listener)
    }

    @UiThread
    override fun setVideoAutoplayBlocked(blocked: Boolean) {
        if (!closed) session.setVideoAutoplayBlocked(blocked)
    }

    override fun siteCertificate(): SiteCertificate? = if (closed) null else session.siteCertificate()

    @UiThread
    override fun setHttpPasswordManagerSelectionEnabled(enabled: Boolean) {
        if (!closed) session.setHttpPasswordManagerSelectionEnabled(enabled)
    }

    @UiThread
    override fun setAudioMuted(muted: Boolean) {
        if (!closed) session.setAudioMuted(muted)
    }

    @UiThread
    override fun executeMediaCommand(command: GeckoMediaCommand) {
        if (!closed) session.executeMediaCommand(command)
    }

    @UiThread
    override fun seekMedia(positionMillis: Long) {
        if (!closed) session.seekMedia(positionMillis)
    }

    @UiThread
    override fun notifyPictureInPictureModeChanged(inPictureInPicture: Boolean) {
        if (closed || this.inPictureInPicture == inPictureInPicture) return
        this.inPictureInPicture = inPictureInPicture
        session.notifyPictureInPictureModeChanged(inPictureInPicture)
    }

    @UiThread
    override fun setPictureInPicturePlaybackExpected(expected: Boolean) {
        if (!closed) session.setPictureInPicturePlaybackExpected(expected)
    }

    @UiThread
    override fun preparePictureInPicturePlayback(
        identity: GeckoInlineVideoIdentity,
        onResult: (GeckoPictureInPicturePreparation?) -> Unit,
    ) {
        if (closed) onResult(null) else session.preparePictureInPicturePlayback(
            identity = identity,
            onResult = onResult,
        )
    }

    @UiThread
    override fun restorePictureInPicturePresentation(onResult: (Boolean) -> Unit) {
        if (closed) onResult(false) else session.restorePictureInPicturePresentation(onResult)
    }

    @UiThread
    override fun setInlineVideoPresentation(
        identity: GeckoInlineVideoIdentity?,
        expected: Boolean,
        onResult: (Boolean) -> Unit,
    ) {
        if (closed) onResult(false) else session.setInlineVideoPresentation(
            identity = identity,
            expected = expected,
            onResult = onResult,
        )
    }

    @UiThread
    override fun exitFullscreen() {
        if (!closed) session.exitFullscreen()
    }

    @UiThread
    override fun goToHistoryIndex(index: Int) {
        if (!closed) session.goToHistoryIndex(index)
    }

    override fun historyUrlAtOffset(offset: Int): String? = if (closed) {
        null
    } else {
        session.historyUrlAtOffset(offset)
    }

    @UiThread
    override fun capturePreview(
        targetWidthPx: Int,
        visibleViewHeightPx: Int,
        maximumTargetHeightPx: Int,
        onComplete: (Bitmap?) -> Unit,
    ): BrowserEnginePreviewCapture? {
        if (closed) return null
        return session.capturePreview(
            targetWidthPx = targetWidthPx,
            visibleViewHeightPx = visibleViewHeightPx,
            maximumTargetHeightPx = maximumTargetHeightPx,
            onComplete = onComplete,
        )
    }

    @UiThread
    override fun findInPage(
        query: String,
        forward: Boolean,
        onComplete: (GeckoFindResult?) -> Unit,
    ) {
        if (closed) {
            onComplete(null)
            return
        }
        session.findInPage(query = query, forward = forward, onComplete = onComplete)
    }

    @UiThread
    override fun clearFindInPage() {
        if (!closed) session.clearFindInPage()
    }

    override val supportsFindInPageOptions: Boolean
        get() = true

    @UiThread
    override fun setFindInPageOptions(options: FindInPageOptions) {
        if (!closed) session.setFindInPageOptions(options)
    }

    @UiThread
    override fun extractPageForReader(onComplete: (String?) -> Unit) {
        if (closed) {
            onComplete(null)
            return
        }
        session.extractPageForReader(onComplete)
    }

    @UiThread
    override fun protectionCounts(onResult: (SiteProtectionCounts?) -> Unit) {
        if (closed) onResult(null) else session.protectionCounts(onResult)
    }

    override fun pageTranslation(): PageTranslationPort =
        if (closed) PageTranslationPort.Unavailable else session.pageTranslation()

    override fun pageDistractions(): PageDistractionsPort =
        if (closed) PageDistractionsPort.Unavailable else session.pageDistractions()

    override fun probeTextInputOcclusion(
        viewportRect: BrowserViewportRect,
        mode: TextInputOcclusionProbeMode,
        onComplete: (TextInputOcclusionProbeResult) -> Unit,
    ) {
        if (closed) {
            onComplete(TextInputOcclusionProbeResult.NoFocusedTextInput)
            return
        }
        session.probeTextInputOcclusion(viewportRect, mode, onComplete)
    }

    @UiThread
    override fun printPage(): Boolean = !closed && session.printPage()

    override fun updatePrivacyPolicy(
        policy: GeckoPrivacyPolicy,
        reloadOnCookiePermissionChange: Boolean,
        onReady: () -> Unit,
    ) {
        if (closed) return
        session.updatePrivacyPolicy(policy, reloadOnCookiePermissionChange, onReady)
    }

    @UiThread
    override fun setDesktopMode(enabled: Boolean) {
        if (!closed) session.setDesktopMode(enabled)
    }

    override fun sessionStateSnapshot(): String? = if (closed) null else session.sessionStateSnapshot()

    override fun restoreSessionState(encodedState: String): Boolean =
        !closed && session.restoreSessionState(encodedState)

    override fun loadExtensionUrl(url: String): Boolean =
        !closed && session.loadExtensionUrl(url)

    private fun load(address: String) {
        if (session.loadUrl(address)) return
        reportInvalidAddress()
    }

    private fun replaceHistory(address: String) {
        if (session.replaceHistoryUrl(address)) return
        reportInvalidAddress()
    }

    private fun retryFailedPage(address: String) {
        if (session.retryFailedPage(address)) return
        reportInvalidAddress()
    }

    private fun reportInvalidAddress() {
        eventSink.onEngineEvent(
            previousState.toEngineEvent(
                tabId = tabId,
                type = BrowserEngineEventType.NavigationFailed,
                failureDescription = INVALID_ADDRESS_FAILURE,
            ),
        )
    }

    /** Whether a tab of this profile, isolation and privacy may adopt this session's page. */
    fun sharesContext(profileId: String, isolationEnabled: Boolean, isPrivate: Boolean): Boolean =
        session.profileId == profileId &&
            session.isolationEnabled == isolationEnabled &&
            session.isPrivate == isPrivate

    /**
     * Lets go of a link preview's session without closing it, so a tab can adopt the live page.
     * This adapter is closed afterwards and reports nothing more; null if it can't hand over.
     */
    @UiThread
    fun detachForAdoption(): GeckoBrowserSession? {
        if (closed || contentKind != BrowserEngineContentKind.LinkPeek) return null
        closed = true
        clearSessionListeners()
        onClosed()
        return session
    }

    @UiThread
    override fun announceAdoptedPage() {
        if (closed || !adopted) return
        val current = previousState
        if (current.url == null) return
        eventSink.onEngineEvent(
            current.copy(isLoading = true).toEngineEvent(tabId, BrowserEngineEventType.NavigationStarted),
        )
        // The history the preview built (its redirects) becomes the tab's, saved for restore.
        trailHistoryTracker.forget()
        session.setHistoryStateListener(trailHistoryTracker::onHistoryStateChanged)
        if (current.isLoading) return
        val type = when (current.lastNavigationSucceeded) {
            true -> BrowserEngineEventType.NavigationCommitted
            false -> BrowserEngineEventType.NavigationFailed
            null -> BrowserEngineEventType.StateChanged
        }
        eventSink.onEngineEvent(
            current.toEngineEvent(
                tabId = tabId,
                type = type,
                failureDescription = if (type == BrowserEngineEventType.NavigationFailed) {
                    current.failureDescription ?: LOAD_FAILURE
                } else {
                    null
                },
            ),
        )
    }

    private fun clearSessionListeners() {
        session.setStateListener(null)
        session.setHistoryStateListener(null)
        session.setMediaStateListener(null)
        session.setInlineVideoOpenRequestListener(null)
        session.setFullscreenStateListener(null)
        session.setScrollListener(null)
        session.setContentTargetListener(null)
        session.setNavigationRequestListener(null)
        session.setNewSessionListener(null)
        session.setDownloadResponseListener(null)
        session.setFilePromptListener(null)
        session.setAndroidPermissionRequestListener(null)
        session.setContentPermissionRequestListener(null)
        session.setMediaPermissionRequestListener(null)
        session.setAuthPromptListener(null)
        session.setWebPromptListener(null)
    }

    private fun close() {
        if (closed) return
        closed = true
        clearSessionListeners()
        session.close()
        onClosed()
        eventSink.onEngineEvent(
            previousState.toEngineEvent(
                tabId = tabId,
                type = BrowserEngineEventType.Closed,
            ),
        )
    }

    private fun onStateChanged(state: GeckoBrowserSessionState) {
        if (closed) return
        if (state.crashed && !previousState.crashed) {
            previousState = state
            closed = true
            clearSessionListeners()
            session.close()
            onClosed()
            eventSink.onEngineEvent(
                state.toEngineEvent(
                    tabId = tabId,
                    type = BrowserEngineEventType.Crashed,
                    failureDescription = CRASH_FAILURE,
                ),
            )
            return
        }
        val type = when {
            state.isLoading && !previousState.isLoading ->
                BrowserEngineEventType.NavigationStarted
            !state.isLoading && previousState.isLoading && state.lastNavigationSucceeded == true ->
                BrowserEngineEventType.NavigationCommitted
            !state.isLoading && previousState.isLoading &&
                state.lastNavigationSucceeded == false && stopRequested ->
                BrowserEngineEventType.StateChanged
            !state.isLoading && previousState.isLoading &&
                state.lastNavigationSucceeded == false -> BrowserEngineEventType.NavigationFailed
            !state.isLoading && state.lastNavigationSucceeded == false &&
                previousState.lastNavigationSucceeded != false ->
                BrowserEngineEventType.NavigationFailed
            state.hasSharedStateChangeFrom(previousState) -> BrowserEngineEventType.StateChanged
            else -> null
        }
        previousState = state
        if (!state.isLoading) stopRequested = false
        if (type != null) {
            eventSink.onEngineEvent(
                state.toEngineEvent(
                    tabId = tabId,
                    type = type,
                    failureDescription = if (type == BrowserEngineEventType.NavigationFailed) {
                        state.failureDescription ?: LOAD_FAILURE
                    } else {
                        null
                    },
                ),
            )
        }
    }

    private companion object {
        const val INVALID_ADDRESS_FAILURE = "Address rejected by browser URI policy"
        const val LOAD_FAILURE = "Gecko navigation failed"
        const val CRASH_FAILURE = "Gecko content process terminated"
    }
}

private fun GeckoBrowserSessionState.hasSharedStateChangeFrom(
    previous: GeckoBrowserSessionState,
): Boolean =
    url != previous.url ||
        title != previous.title ||
        canGoBack != previous.canGoBack ||
        canGoForward != previous.canGoForward ||
        httpStatusCode != previous.httpStatusCode

private fun GeckoBrowserSessionState.toEngineEvent(
    tabId: String,
    type: BrowserEngineEventType,
    failureDescription: String? = null,
): BrowserEngineEvent = BrowserEngineEvent(
    tabId = tabId,
    type = type,
    address = url,
    title = title,
    canGoBack = canGoBack,
    canGoForward = canGoForward,
    failureDescription = failureDescription,
    isLoading = isLoading,
    httpStatusCode = httpStatusCode,
    failureKind = failureKind,
)
