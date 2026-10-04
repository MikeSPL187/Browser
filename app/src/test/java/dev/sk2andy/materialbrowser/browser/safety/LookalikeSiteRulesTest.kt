package dev.sk2andy.materialbrowser.browser.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LookalikeSiteRulesTest {
    private val known = LookalikeSiteRules.WELL_KNOWN_HOSTS + "bank.example.ru"

    private fun imitated(url: String) = LookalikeSiteRules.imitatedHost(url, known)

    @Test
    fun `digits and letters that read alike point to the real site`() {
        assertEquals("paypal.com", imitated("https://paypa1.com/login"))
        assertEquals("gmail.com", imitated("http://www.gmai1.com/"))
        assertEquals("microsoft.com", imitated("https://rnicrosoft.com/"))
    }

    @Test
    fun `a hyphen in place of a dot is caught, as on the board`() {
        assertEquals("bank.example.ru", imitated("https://bank-exarnple.ru/"))
    }

    @Test
    fun `a Cyrillic letter inside a Latin name is caught, punycode or not`() {
        assertEquals("apple.com", imitated("https://аpple.com/"))
        assertEquals("apple.com", imitated("https://xn--pple-43d.com/"))
    }

    @Test
    fun `a lookalike subdomain of another site is caught by its registrable part`() {
        assertEquals("paypal.com", imitated("https://secure.paypa1.com/signin"))
    }

    @Test
    fun `the real site and its own subdomains are never flagged`() {
        assertNull(imitated("https://paypal.com/"))
        assertNull(imitated("https://www.paypal.com/myaccount"))
        assertNull(imitated("https://online.sberbank.ru/"))
        assertNull(imitated("https://bank.example.ru/"))
    }

    @Test
    fun `ordinary sites, addresses and other schemes pass`() {
        assertNull(imitated("https://en.wikipedia.org/wiki/Zen"))
        assertNull(imitated("https://example.org/"))
        assertNull(imitated("http://192.168.1.1/"))
        assertNull(imitated("about:blank"))
        assertNull(imitated("https://сбербанк.рф/"))
    }

    @Test
    fun `another top-level domain is another site, not a lookalike`() {
        assertNull(imitated("https://paypal.de/"))
    }

    @Test
    fun `the skeleton folds what the eye folds`() {
        assertEquals(
            LookalikeSiteRules.skeleton("bank.example.ru"),
            LookalikeSiteRules.skeleton("bank-exarnple.ru"),
        )
    }
}
