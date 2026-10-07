package dev.sk2andy.materialbrowser.browser.safety

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import java.net.URI

/**
 * A navigation stopped before anything loaded: the address imitates [imitatedHost], or, when that
 * is null, the host is on the list of known phishing and malware sites, or Safe Browsing
 * [reported][reportedBySafeBrowsing] it. A reported site has no way through: the engine refuses it.
 * [shownPageUrl] is the page the tab showed when it was stopped; the engine still shows it.
 */
data class BlockedSite(
    val url: String,
    val host: String,
    val imitatedHost: String? = null,
    val reportedBySafeBrowsing: Boolean = false,
    val shownPageUrl: String? = null,
)

/** Where «Back to safety» takes the tab. */
sealed interface BackToSafety {
    /** The tab still shows [url] under the warning: its address returns to it. */
    data class ShowPage(val url: String) : BackToSafety

    /** The tab was opened straight onto the stopped site: there is nothing safe to show. */
    data object CloseTab : BackToSafety
}

/**
 * Stops navigations to sites that pretend to be another or are known to be dangerous (board
 * W-DangerousSite) before anything loads, and remembers per tab what was stopped for the warning
 * page. «Open anyway» lets that host through for the rest of the session, in memory only.
 */
class DangerousSiteGuard(
    private val listedHosts: (String) -> Boolean = { false },
    private val knownHosts: () -> Collection<String>,
) {
    constructor(context: Context, knownHosts: () -> Collection<String>) :
        this(ThreatHostList.get(context)::contains, knownHosts)

    val blocked = mutableStateMapOf<String, BlockedSite>()
    private val allowedHosts = mutableSetOf<String>()

    /**
     * True when the main-frame [url] must not load in [tabId]; the tab then shows the warning.
     * A navigation that may load leaves the warning behind: the tab is going somewhere else.
     * [shownPageUrl] is the page the engine shows in the tab now, null without one.
     */
    fun intercept(tabId: String, url: String, shownPageUrl: String? = null): Boolean {
        val site = check(url)?.let { site ->
            site.copy(shownPageUrl = shownPageUrl?.takeIf { page -> isSafePage(page, site.host) })
        }
        if (site == null) blocked.remove(tabId) else blocked[tabId] = site
        return site != null
    }

    /**
     * What would stop [url], without remembering it for any tab: Glance asks before it loads and
     * shows its own warning, with no way through.
     */
    fun check(url: String): BlockedSite? {
        val host = readableHost(url)
        val key = host?.removePrefix("www.") ?: url
        if (key in allowedHosts) return null
        val imitated = LookalikeSiteRules.imitatedHost(
            url = url,
            knownHosts = LookalikeSiteRules.WELL_KNOWN_HOSTS + knownHosts(),
        )
        if (imitated == null && (host == null || !listedHosts(host))) return null
        return BlockedSite(url = url, host = key, imitatedHost = imitated)
    }

    /** A web page off the stopped host; a page of that site itself is no safety. */
    private fun isSafePage(url: String, blockedHost: String): Boolean {
        val scheme = url.substringBefore(':').lowercase()
        if (scheme != "http" && scheme != "https") return false
        val host = readableHost(url)?.removePrefix("www.") ?: return false
        return host != blockedHost
    }

    /** The host as the user reads it; URI gives none for a non-ASCII host, the authority does. */
    private fun readableHost(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val host = uri.host ?: uri.authority?.substringAfterLast('@')?.substringBefore(':')
        return host?.lowercase()
    }

    /** The warning goes unanswered: the tab is going somewhere else. */
    fun dismiss(tabId: String) {
        blocked.remove(tabId)
    }

    /** The warning goes; null when [tabId] showed none. */
    fun backToSafety(tabId: String): BackToSafety? {
        val site = blocked.remove(tabId) ?: return null
        return site.shownPageUrl?.let(BackToSafety::ShowPage) ?: BackToSafety.CloseTab
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
