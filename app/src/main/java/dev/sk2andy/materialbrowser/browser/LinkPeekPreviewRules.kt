package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import dev.sk2andy.materialbrowser.browser.integration.LinkPeekPreviewNavigationPolicy
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEvent
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind

/** Where a Glance preview stands; the card shows a message in place of a page it can't show. */
internal sealed interface LinkPeekPreviewStatus {
    data object Loading : LinkPeekPreviewStatus

    data object Loaded : LinkPeekPreviewStatus

    /** The page didn't load; [kind] picks the explanation. Retry builds a fresh preview. */
    data class Failed(val kind: BrowserEngineFailureKind?) : LinkPeekPreviewStatus

    /** The preview's renderer stopped; the engine closed its session, so only Retry helps. */
    data object Crashed : LinkPeekPreviewStatus

    /** The local dangerous-site guard stopped the address; Glance offers no way through. */
    data class Blocked(val site: BlockedSite) : LinkPeekPreviewStatus
}

internal sealed interface LinkPeekNavigationDecision {
    data object Allow : LinkPeekNavigationDecision

    /** Not a web address: denied quietly, the page stays where it was. */
    data object Deny : LinkPeekNavigationDecision

    /** A dangerous or lookalike site: denied, and the card says why. */
    data class Block(val site: BlockedSite) : LinkPeekNavigationDecision
}

internal object LinkPeekPreviewRules {
    /**
     * Glance asks the same dangerous-site guard as a tab, before the first load and again for
     * every main-frame navigation and redirect after it.
     */
    fun navigationDecision(
        url: String?,
        check: (String) -> BlockedSite?,
    ): LinkPeekNavigationDecision {
        if (LinkPeekPreviewNavigationPolicy.shouldBlock(url)) return LinkPeekNavigationDecision.Deny
        val safeUrl = BrowserUriPolicy.normalizeHttpUrl(url) ?: return LinkPeekNavigationDecision.Deny
        val site = check(safeUrl) ?: return LinkPeekNavigationDecision.Allow
        return LinkPeekNavigationDecision.Block(site)
    }

    /**
     * A blocked preview stays blocked: nothing the stopped page does may bring it back. A crashed
     * one has no session left. A failure is not a loaded page: the card says what went wrong, and
     * Safe Browsing's refusal is the dangerous-site warning, as in a tab.
     */
    fun statusAfter(
        current: LinkPeekPreviewStatus,
        event: BrowserEngineEvent,
        pageUrl: String,
    ): LinkPeekPreviewStatus = when {
        current is LinkPeekPreviewStatus.Blocked || current == LinkPeekPreviewStatus.Crashed -> current
        event.type == BrowserEngineEventType.Crashed -> LinkPeekPreviewStatus.Crashed
        event.type == BrowserEngineEventType.NavigationStarted -> LinkPeekPreviewStatus.Loading
        event.type == BrowserEngineEventType.NavigationCommitted -> LinkPeekPreviewStatus.Loaded
        event.type != BrowserEngineEventType.NavigationFailed -> current
        event.failureKind == BrowserEngineFailureKind.DangerousSite -> {
            // Safe Browsing halts the load before the page moves; the engine names the address.
            val url = BrowserUriPolicy.normalizeHttpUrl(event.failureDescription) ?: pageUrl
            LinkPeekPreviewStatus.Blocked(
                BlockedSite(
                    url = url,
                    host = BrowserUriPolicy.displayHttpHost(url).ifEmpty { url },
                    reportedBySafeBrowsing = true,
                ),
            )
        }
        else -> LinkPeekPreviewStatus.Failed(event.failureKind)
    }
}
