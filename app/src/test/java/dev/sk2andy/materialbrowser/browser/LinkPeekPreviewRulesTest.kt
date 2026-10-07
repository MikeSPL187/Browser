package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.browser.safety.DangerousSiteGuard
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEvent
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkPeekPreviewRulesTest {
    private val guard = DangerousSiteGuard(listedHosts = { host -> host == "login-bank.top" }) {
        emptyList()
    }

    @Test
    fun `an ordinary web address loads`() {
        assertEquals(
            LinkPeekNavigationDecision.Allow,
            LinkPeekPreviewRules.navigationDecision("https://en.wikipedia.org/wiki/Glance", guard::check),
        )
    }

    @Test
    fun `a lookalike is blocked with the site the warning names`() {
        assertEquals(
            LinkPeekNavigationDecision.Block(
                BlockedSite("https://paypa1.com/login", "paypa1.com", imitatedHost = "paypal.com"),
            ),
            LinkPeekPreviewRules.navigationDecision("https://paypa1.com/login", guard::check),
        )
    }

    @Test
    fun `a redirect to a listed host is blocked and leaves no tab warning behind`() {
        val decision = LinkPeekPreviewRules.navigationDecision("https://login-bank.top/sign-in", guard::check)

        assertTrue(decision is LinkPeekNavigationDecision.Block)
        assertTrue(guard.blocked.isEmpty())
    }

    @Test
    fun `non-web navigations are denied without asking the guard`() {
        val noGuard: (String) -> BlockedSite? = { error("asked the guard for $it") }

        listOf("intent://open/#Intent;end", "javascript:alert(1)", "file:///sdcard/a.html", null)
            .forEach { url ->
                assertEquals(
                    LinkPeekNavigationDecision.Deny,
                    LinkPeekPreviewRules.navigationDecision(url, noGuard),
                )
            }
    }

    @Test
    fun `a blocked preview stays blocked whatever the stopped page reports`() {
        val blocked = LinkPeekPreviewStatus.Blocked(BlockedSite("https://paypa1.com/", "paypa1.com"))

        BrowserEngineEventType.entries.forEach { type ->
            assertEquals(blocked, statusAfter(blocked, event(type)))
        }
    }

    @Test
    fun `loading follows the page from start to commit`() {
        assertEquals(
            LinkPeekPreviewStatus.Loading,
            statusAfter(LinkPeekPreviewStatus.Loaded, event(BrowserEngineEventType.NavigationStarted)),
        )
        assertEquals(
            LinkPeekPreviewStatus.Loaded,
            statusAfter(LinkPeekPreviewStatus.Loading, event(BrowserEngineEventType.NavigationCommitted)),
        )
        assertEquals(
            LinkPeekPreviewStatus.Loading,
            statusAfter(LinkPeekPreviewStatus.Loading, event(BrowserEngineEventType.StateChanged)),
        )
    }

    @Test
    fun `a failed load is a failure with its kind, not a loaded page`() {
        assertEquals(
            LinkPeekPreviewStatus.Failed(BrowserEngineFailureKind.UnknownHost),
            statusAfter(
                LinkPeekPreviewStatus.Loading,
                event(BrowserEngineEventType.NavigationFailed, BrowserEngineFailureKind.UnknownHost),
            ),
        )
        assertEquals(
            LinkPeekPreviewStatus.Failed(null),
            statusAfter(LinkPeekPreviewStatus.Loading, event(BrowserEngineEventType.NavigationFailed)),
        )
    }

    @Test
    fun `a new navigation after a failure loads again`() {
        assertEquals(
            LinkPeekPreviewStatus.Loading,
            statusAfter(
                LinkPeekPreviewStatus.Failed(BrowserEngineFailureKind.Offline),
                event(BrowserEngineEventType.NavigationStarted),
            ),
        )
    }

    @Test
    fun `a crash ends the preview until it is built again`() {
        val crashed = statusAfter(LinkPeekPreviewStatus.Loading, event(BrowserEngineEventType.Crashed))

        assertEquals(LinkPeekPreviewStatus.Crashed, crashed)
        BrowserEngineEventType.entries.forEach { type ->
            assertEquals(LinkPeekPreviewStatus.Crashed, statusAfter(crashed, event(type)))
        }
    }

    @Test
    fun `safe browsing's refusal is the dangerous-site warning for the stopped address`() {
        val status = statusAfter(
            LinkPeekPreviewStatus.Loading,
            event(
                BrowserEngineEventType.NavigationFailed,
                BrowserEngineFailureKind.DangerousSite,
                failureDescription = "https://www.malware.example/landing",
            ),
        )

        assertEquals(
            LinkPeekPreviewStatus.Blocked(
                BlockedSite(
                    url = "https://www.malware.example/landing",
                    host = "malware.example",
                    reportedBySafeBrowsing = true,
                ),
            ),
            status,
        )
    }

    private fun statusAfter(current: LinkPeekPreviewStatus, event: BrowserEngineEvent) =
        LinkPeekPreviewRules.statusAfter(current, event, pageUrl = "https://start.example/")

    private fun event(
        type: BrowserEngineEventType,
        failureKind: BrowserEngineFailureKind? = null,
        failureDescription: String? = null,
    ) = BrowserEngineEvent(
        tabId = "link-peek-1",
        type = type,
        address = "https://start.example/",
        title = null,
        canGoBack = false,
        canGoForward = false,
        failureDescription = failureDescription,
        failureKind = failureKind,
    )
}
