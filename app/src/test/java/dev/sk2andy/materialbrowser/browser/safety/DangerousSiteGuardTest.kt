package dev.sk2andy.materialbrowser.browser.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DangerousSiteGuardTest {
    private val guard = DangerousSiteGuard { listOf("https://bank.example.ru/cabinet") }

    @Test
    fun `a lookalike of the user's own site is stopped and remembered for the tab`() {
        assertTrue(guard.intercept("tab", "https://bank-exarnple.ru/login"))
        assertEquals(
            BlockedSite("https://bank-exarnple.ru/login", "bank-exarnple.ru", "bank.example.ru"),
            guard.blocked["tab"],
        )
    }

    @Test
    fun `ordinary sites pass and leave nothing behind`() {
        assertFalse(guard.intercept("tab", "https://en.wikipedia.org/"))
        assertNull(guard.blocked["tab"])
    }

    @Test
    fun `open anyway lets the host through for the session, even a non-ASCII one`() {
        assertTrue(guard.intercept("tab", "https://аpple.com/"))
        assertEquals("https://аpple.com/", guard.allow("tab"))
        assertNull(guard.blocked["tab"])
        assertFalse(guard.intercept("tab", "https://аpple.com/id"))
    }

    @Test
    fun `a safe address after the warning takes the warning away`() {
        assertTrue(guard.intercept("tab", "https://paypa1.com/"))

        assertFalse(guard.intercept("tab", "https://en.wikipedia.org/"))

        assertNull(guard.blocked["tab"])
        assertNull(guard.allow("tab"))
    }

    @Test
    fun `a safe address in another tab leaves this tab's warning`() {
        assertTrue(guard.intercept("tab", "https://paypa1.com/"))

        assertFalse(guard.intercept("other", "https://en.wikipedia.org/"))

        assertEquals("paypa1.com", guard.blocked["tab"]?.host)
    }

    @Test
    fun `back to safety forgets the warning but not the danger`() {
        assertTrue(guard.intercept("tab", "https://paypa1.com/"))
        guard.dismiss("tab")
        assertNull(guard.blocked["tab"])
        assertTrue(guard.intercept("tab", "https://paypa1.com/"))
    }

    @Test
    fun `a host on the threat list is stopped with no site to point to`() {
        val listed = DangerousSiteGuard(listedHosts = { host -> host == "www.login-bank.top" }) {
            emptyList()
        }

        assertTrue(listed.intercept("tab", "https://www.login-bank.top/sign-in"))
        assertEquals(
            BlockedSite("https://www.login-bank.top/sign-in", "login-bank.top", imitatedHost = null),
            listed.blocked["tab"],
        )
        assertEquals("https://www.login-bank.top/sign-in", listed.allow("tab"))
        assertFalse(listed.intercept("tab", "https://www.login-bank.top/"))
    }

    @Test
    fun `a lookalike on the threat list still names the real site`() {
        val listed = DangerousSiteGuard(listedHosts = { true }) {
            listOf("https://bank.example.ru/")
        }

        assertTrue(listed.intercept("tab", "https://bank-exarnple.ru/"))
        assertEquals("bank.example.ru", listed.blocked["tab"]?.imitatedHost)
    }
}
