package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.browser.safety.DangerousSiteGuard
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType
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
            assertEquals(blocked, LinkPeekPreviewRules.statusAfter(blocked, type))
        }
    }

    @Test
    fun `loading follows the page from start to commit`() {
        assertEquals(
            LinkPeekPreviewStatus.Loading,
            LinkPeekPreviewRules.statusAfter(LinkPeekPreviewStatus.Loaded, BrowserEngineEventType.NavigationStarted),
        )
        assertEquals(
            LinkPeekPreviewStatus.Loaded,
            LinkPeekPreviewRules.statusAfter(LinkPeekPreviewStatus.Loading, BrowserEngineEventType.NavigationCommitted),
        )
        assertEquals(
            LinkPeekPreviewStatus.Loading,
            LinkPeekPreviewRules.statusAfter(LinkPeekPreviewStatus.Loading, BrowserEngineEventType.StateChanged),
        )
    }
}
