package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import dev.sk2andy.materialbrowser.browser.integration.LinkPeekPreviewNavigationPolicy
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType

/** Where a Glance preview stands; the card shows a message in place of a page it can't show. */
internal sealed interface LinkPeekPreviewStatus {
    data object Loading : LinkPeekPreviewStatus

    data object Loaded : LinkPeekPreviewStatus

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

    /** A blocked preview stays blocked: nothing the stopped page does may bring it back. */
    fun statusAfter(
        current: LinkPeekPreviewStatus,
        type: BrowserEngineEventType,
    ): LinkPeekPreviewStatus = when {
        current is LinkPeekPreviewStatus.Blocked -> current
        type == BrowserEngineEventType.NavigationStarted -> LinkPeekPreviewStatus.Loading
        type == BrowserEngineEventType.NavigationCommitted ||
            type == BrowserEngineEventType.NavigationFailed -> LinkPeekPreviewStatus.Loaded
        else -> current
    }
}
