package dev.sk2andy.materialbrowser.browser.safety

import androidx.compose.runtime.mutableStateMapOf
import java.net.URI

/** A navigation stopped because its address imitates [imitatedHost]. */
data class BlockedSite(val url: String, val host: String, val imitatedHost: String)

/**
 * Stops navigations to sites that pretend to be another (board W-DangerousSite) before anything
 * loads, and remembers per tab what was stopped for the warning page. «Open anyway» lets that
 * host through for the rest of the session, in memory only.
 */
class DangerousSiteGuard(private val knownHosts: () -> Collection<String>) {
    val blocked = mutableStateMapOf<String, BlockedSite>()
    private val allowedHosts = mutableSetOf<String>()

    /** True when [url] must not load in [tabId]; the tab then shows the warning. */
    fun intercept(tabId: String, url: String): Boolean {
        val host = hostKey(url)
        if (host in allowedHosts) return false
        val imitated = LookalikeSiteRules.imitatedHost(
            url = url,
            knownHosts = LookalikeSiteRules.WELL_KNOWN_HOSTS + knownHosts(),
        ) ?: return false
        blocked[tabId] = BlockedSite(url = url, host = host, imitatedHost = imitated)
        return true
    }

    /** The host as the user reads it; URI gives none for a non-ASCII host, the authority does. */
    private fun hostKey(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return url
        val host = uri.host ?: uri.authority?.substringAfterLast('@')?.substringBefore(':')
        return host?.lowercase()?.removePrefix("www.") ?: url
    }

    /** Back to safety: the tab stays where it was. */
    fun dismiss(tabId: String) {
        blocked.remove(tabId)
    }

    /** The address to load after «Open anyway»; its host is not stopped again this session. */
    fun allow(tabId: String): String? {
        val site = blocked.remove(tabId) ?: return null
        allowedHosts += site.host
        return site.url
    }

    fun forgetTab(tabId: String) {
        blocked.remove(tabId)
    }
}
