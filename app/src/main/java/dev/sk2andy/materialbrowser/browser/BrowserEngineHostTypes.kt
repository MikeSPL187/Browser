package dev.sk2andy.materialbrowser.browser

import android.net.Uri
import dev.sk2andy.materialbrowser.browser.gecko.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.integration.ExternalAppHandoff
import dev.sk2andy.materialbrowser.browser.integration.ExternalNavigationGrant

/*
 * Bookkeeping types of BrowserController, kept apart so the controller stays within its size
 * budget (docs/vola/tech-plan.md, section 1).
 */

internal data class FirefoxExtensionOptionsTabChrome(
    val title: String,
    val scheme: String,
    val host: String,
    val port: Int,
) {
    fun owns(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() == scheme &&
            uri.host?.lowercase() == host &&
            uri.port == port
    }

    companion object {
        fun create(title: String, url: String): FirefoxExtensionOptionsTabChrome? {
            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase() ?: return null
            val host = uri.host?.lowercase()?.takeIf(String::isNotBlank) ?: return null
            if (scheme != "moz-extension") return null
            return FirefoxExtensionOptionsTabChrome(
                title = title,
                scheme = scheme,
                host = host,
                port = uri.port,
            )
        }
    }
}

internal sealed interface ExternalAppHandoffSource {
    data class Tab(
        val tabId: String,
        val profileId: String,
        val isPrivate: Boolean,
        val session: AndroidBrowserEngineSessionPort,
    ) : ExternalAppHandoffSource

    data class Preview(
        val sessionId: Long,
        val generation: Int,
        val profileId: String,
        val session: AndroidBrowserEngineSessionPort,
    ) : ExternalAppHandoffSource
}

internal data class PendingExternalAppHandoff(
    val match: ExternalAppHandoff,
    val source: ExternalAppHandoffSource,
)

internal data class PendingExternalAppPrompt(
    val prompt: ExternalAppPrompt,
    val requestUrl: String,
    val safeHttpUrl: String?,
    val webTargetUrl: String?,
    val source: ExternalAppHandoffSource,
    val grant: ExternalNavigationGrant?,
    val isRedirect: Boolean,
    val sourceNavigationGeneration: Int,
    val sourcePageUrl: String?,
)

internal enum class ExternalAppNavigationHandling {
    Automatic,
    Prompted,
    Unavailable,
}
