package dev.sk2andy.materialbrowser.browser.safety

import java.net.IDN
import org.junit.Assert.assertEquals
import org.junit.Test

class IdnDisplayRulesTest {
    private fun ascii(host: String) = IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).lowercase()

    @Test
    fun `plain names stay as they are`() {
        assertEquals("example.com", IdnDisplayRules.displayHost("example.com"))
        assertEquals("[::1]", IdnDisplayRules.displayHost("[::1]"))
        assertEquals("", IdnDisplayRules.displayHost(""))
    }

    @Test
    fun `a name in one script reads in its letters, whichever spelling the engine gives`() {
        assertEquals("пример.рф", IdnDisplayRules.displayHost(ascii("пример.рф")))
        assertEquals("пример.рф", IdnDisplayRules.displayHost("пример.рф"))
        assertEquals("münchen.de", IdnDisplayRules.displayHost(ascii("münchen.de")))
        assertEquals("例え.jp", IdnDisplayRules.displayHost(ascii("例え.jp")))
        assertEquals("яндекс.рф", IdnDisplayRules.displayHost("яндекс.рф"))
    }

    @Test
    fun `a Cyrillic name made of Latin look-alikes stays in punycode under a Latin domain`() {
        val apple = "аррӏе.com"
        assertEquals(ascii(apple), IdnDisplayRules.displayHost(apple))
        assertEquals(ascii(apple), IdnDisplayRules.displayHost(ascii(apple)))
        assertEquals(ascii("сосо.com"), IdnDisplayRules.displayHost("сосо.com"))
    }

    @Test
    fun `the same letters under a Cyrillic domain are an ordinary Russian name`() {
        assertEquals("сосо.рф", IdnDisplayRules.displayHost("сосо.рф"))
    }

    @Test
    fun `scripts mixed in one label stay in punycode`() {
        val mixed = "pаypal.com" // Cyrillic а
        assertEquals(ascii(mixed), IdnDisplayRules.displayHost(mixed))
        assertEquals(ascii("gοοgle.com"), IdnDisplayRules.displayHost("gοοgle.com")) // Greek ο
    }

    @Test
    fun `labels in different scripts are each judged alone`() {
        assertEquals("почта.example.com", IdnDisplayRules.displayHost("почта.example.com"))
    }

    @Test
    fun `symbols that look like separators stay in punycode`() {
        val slash = "example.com⁄login.evil.com" // fraction slash
        assertEquals(ascii(slash), IdnDisplayRules.displayHost(slash))
    }

    @Test
    fun `a broken punycode label is shown as given`() {
        assertEquals("xn--zz.com", IdnDisplayRules.displayHost("xn--zz.com"))
    }
}
