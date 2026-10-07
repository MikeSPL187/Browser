package dev.sk2andy.materialbrowser.browser.systemwebview.commands

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

internal object WebViewProfileCookies {
    fun managerFor(webView: WebView): CookieManager? =
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            runCatching { WebViewCompat.getProfile(webView).cookieManager }.getOrNull()
        } else {
            CookieManager.getInstance()
        }

    /**
     * Cookie jars of every stored profile, whether or not one of its tabs was opened in this
     * process: an isolated workspace keeps its own jar on disk.
     */
    fun storedProfileManagers(): List<CookieManager> =
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            storedProfiles().mapNotNull { profile -> runCatching { profile.cookieManager }.getOrNull() }
        } else {
            emptyList()
        }

    /** Deletes site storage of every stored profile; the default WebStorage covers only one. */
    fun deleteStoredProfileWebStorage() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return
        storedProfiles().forEach { profile -> runCatching { profile.webStorage.deleteAllData() } }
    }

    private fun storedProfiles(): List<Profile> = runCatching {
        val store = ProfileStore.getInstance()
        store.allProfileNames.mapNotNull(store::getProfile)
    }.getOrDefault(emptyList())
}
