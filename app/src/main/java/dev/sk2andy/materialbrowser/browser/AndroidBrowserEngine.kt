package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.permissions.SitePermission

enum class AndroidBrowserEngineKind(val stableId: String) {
    GeckoView("gecko"),
    SystemWebView("system_webview"),
    ;

    companion object {
        val Default = GeckoView

        fun fromStableId(value: String?): AndroidBrowserEngineKind =
            entries.firstOrNull { it.stableId == value } ?: Default
    }
}

internal data class AndroidBrowserEngineCapabilities(
    val firefoxExtensions: Boolean,
    val toppings: Boolean,
    val nativeAutoplayPolicy: Boolean,
    val insecureHttpPasswordManagerSelection: Boolean,
    val dnsOverHttps: Boolean,
    val httpsOnly: Boolean,
    /** Deleting one site's cookies, storage and caches; System WebView cannot do it per site. */
    val siteDataDeletion: Boolean,
    /** Site permissions the adapter routes to Vola's prompt and decisions; the radar hides others. */
    val sitePermissions: Set<SitePermission>,
    /** Turning the engine's fingerprinting protection off for one site (v5 site protection). */
    val perSiteFingerprintingProtection: Boolean,
    /** Page translation on the phone (v5 V23); System WebView has none, so the UI hides it. */
    val onDeviceTranslation: Boolean,
) {
    companion object {
        val GeckoView = AndroidBrowserEngineCapabilities(
            firefoxExtensions = true,
            toppings = true,
            nativeAutoplayPolicy = true,
            insecureHttpPasswordManagerSelection = true,
            dnsOverHttps = true,
            httpsOnly = true,
            siteDataDeletion = true,
            sitePermissions = setOf(
                SitePermission.Camera,
                SitePermission.Microphone,
                SitePermission.Location,
                SitePermission.Notifications,
                SitePermission.ProtectedMedia,
            ),
            perSiteFingerprintingProtection = true,
            onDeviceTranslation = true,
        )

        val SystemWebView = AndroidBrowserEngineCapabilities(
            firefoxExtensions = false,
            toppings = true,
            nativeAutoplayPolicy = true,
            insecureHttpPasswordManagerSelection = false,
            dnsOverHttps = false,
            httpsOnly = false,
            siteDataDeletion = false,
            sitePermissions = setOf(
                SitePermission.Camera,
                SitePermission.Microphone,
                SitePermission.Location,
            ),
            perSiteFingerprintingProtection = false,
            onDeviceTranslation = false,
        )
    }
}

internal object AndroidBrowserEngineRules {
    fun persistedKind(
        stableId: String?,
        systemWebViewOnly: Boolean,
    ): AndroidBrowserEngineKind = if (systemWebViewOnly) {
        AndroidBrowserEngineKind.SystemWebView
    } else {
        AndroidBrowserEngineKind.fromStableId(stableId)
    }

    fun canSelect(
        kind: AndroidBrowserEngineKind,
        systemWebViewOnly: Boolean,
    ): Boolean = !systemWebViewOnly || kind == AndroidBrowserEngineKind.SystemWebView

    fun capabilities(kind: AndroidBrowserEngineKind): AndroidBrowserEngineCapabilities =
        when (kind) {
            AndroidBrowserEngineKind.GeckoView -> AndroidBrowserEngineCapabilities.GeckoView
            AndroidBrowserEngineKind.SystemWebView ->
                AndroidBrowserEngineCapabilities.SystemWebView
        }
}
