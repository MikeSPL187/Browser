package dev.sk2andy.materialbrowser.browser.engine

import android.content.Context
import android.content.res.Configuration
import android.view.View
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineCapabilities
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import dev.sk2andy.materialbrowser.browser.DnsOverHttpsSettings
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.WebRtcProtectionMode
import dev.sk2andy.materialbrowser.browser.gecko.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.gecko.BrowserEngineEventSink
import dev.sk2andy.materialbrowser.browser.gecko.BrowserEnginePreparedSession
import dev.sk2andy.materialbrowser.browser.gecko.GeckoBrowsingData
import dev.sk2andy.materialbrowser.browser.gecko.GeckoCandyTrailHistoryEventSink
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionActionKey
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionChromeHost
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionSessionIdentity
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPrivacyEventSink
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPrivacyPolicy
import dev.sk2andy.materialbrowser.browser.gecko.GeckoToppingHostState
import dev.sk2andy.materialbrowser.browser.gecko.GeckoToppingInteractionDelegate
import dev.sk2andy.materialbrowser.browser.translation.TranslationModelsPort
import dev.sk2andy.materialbrowser.browser.userscript.UserScript
import dev.sk2andy.materialbrowser.browser.userscript.UserScriptMenuCommand

/** Process-wide engine adapter. Implementations must initialize only their own runtime. */
internal interface AndroidBrowserEngineFactory {
    val kind: AndroidBrowserEngineKind

    val capabilities: AndroidBrowserEngineCapabilities

    val runtimeVersionName: String?

    /**
     * Language models for on-device page translation (v5 V23). Engines without it, System WebView
     * among them, keep [TranslationModelsPort.Unavailable].
     */
    val translationModels: TranslationModelsPort get() = TranslationModelsPort.Unavailable

    fun setExtensionChromeHost(host: GeckoExtensionChromeHost?) = Unit

    fun clickExtensionAction(key: GeckoExtensionActionKey): Boolean = false

    fun dismissExtensionPopup() = Unit

    fun notifySelectedExtensionTabChanged() = Unit

    fun extensionSessionIdentity(tabId: String): GeckoExtensionSessionIdentity? = null

    fun prepareSession(tabId: String, session: BrowserEnginePreparedSession): Boolean = false

    /**
     * Hands [preview], a link preview's live session, to the next [create] of tab [tabId] of this
     * context, so the tab shows the page without loading it again. Engines that can't keep the
     * preview's page (System WebView) return false and the tab loads the link itself.
     */
    fun prepareAdoption(
        tabId: String,
        preview: AndroidBrowserEngineSessionPort,
        profileId: String,
        isolationEnabled: Boolean,
        isPrivate: Boolean,
    ): Boolean = false

    fun createExtensionPopupView(
        context: Context,
        session: BrowserEnginePreparedSession,
    ): View? = null

    fun releaseExtensionPopupView(view: View) = Unit

    fun reconcileToppings(scripts: List<UserScript>)

    fun setToppingHostStateListener(listener: (GeckoToppingHostState) -> Unit)

    fun setToppingInteractionDelegate(delegate: GeckoToppingInteractionDelegate)

    fun invokeToppingMenuCommand(command: UserScriptMenuCommand)

    fun clearToppingValues(scriptId: String)

    fun clearBrowsingData(data: GeckoBrowsingData, onComplete: (Boolean) -> Unit = {})

    fun clearAllData(onComplete: (Boolean) -> Unit = {})

    /**
     * Deletes the cookies, storage and caches of one site ([baseDomain] and its subdomains) in every
     * workspace. Engines without it report `false`; the UI hides it through the capabilities.
     */
    fun clearSiteData(baseDomain: String, onComplete: (Boolean) -> Unit) = onComplete(false)

    fun requestProfileDataDeletion(profileId: String): Boolean

    fun setBlockThirdPartyCookies(blocked: Boolean)

    fun setWebRtcProtectionMode(mode: WebRtcProtectionMode)

    fun setDnsOverHttpsSettings(settings: DnsOverHttpsSettings) = Unit

    fun setHttpsOnlyMode(mode: HttpsOnlyMode) = Unit

    /** Engine-native GPC (workers and every request); WebView keeps the page script and header. */
    fun setGlobalPrivacyControl(enabled: Boolean) = Unit

    /**
     * Sites whose engine fingerprinting protection is off (v5 site protection). One list for the
     * whole engine; engines without per-site control ignore it and the UI hides the switch by
     * [AndroidBrowserEngineCapabilities.perSiteFingerprintingProtection].
     */
    fun setFingerprintingProtectionExceptions(hosts: Set<String>) = Unit

    /** Whether [host] can be excepted from fingerprinting protection; Gecko rejects some domains. */
    fun supportsFingerprintingException(host: String): Boolean = false

    fun setWebContentFontSizeFactor(factor: Float)

    fun setWebContentColorScheme(colorScheme: BrowserWebContentColorScheme) = Unit

    fun onConfigurationChanged(configuration: Configuration) = Unit

    fun setForceDarkWebsites(enabled: Boolean) = Unit

    fun clearPrivateData() = Unit

    fun shutdown() = Unit

    fun create(
        tabId: String,
        profileId: String,
        isolationEnabled: Boolean = false,
        isPrivate: Boolean,
        contentKind: BrowserEngineContentKind,
        privacyPolicy: GeckoPrivacyPolicy = GeckoPrivacyPolicy.Disabled,
        privacyEventSink: GeckoPrivacyEventSink = GeckoPrivacyEventSink { },
        trailHistoryEventSink: GeckoCandyTrailHistoryEventSink =
            GeckoCandyTrailHistoryEventSink { _, _ -> },
        eventSink: BrowserEngineEventSink,
    ): AndroidBrowserEngineSessionPort
}

internal enum class BrowserEngineContentKind {
    RegularTab,
    LinkPeek,
    ExternalPreview,
}

internal enum class BrowserWebContentColorScheme {
    System,
    Light,
    Dark,
}
