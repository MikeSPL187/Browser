package dev.sk2andy.materialbrowser.browser.gecko

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeckoCertErrorPageTest {
    private val marker = GeckoHttpsOnlyErrorPage.HOST_MARKER
    private val strings = GeckoCertErrorPage.Strings(
        languageTag = "en",
        title = "This connection isn’t private",
        body = "The certificate of $marker has expired.",
        backLabel = "Go back",
        detailsLabel = "Details and open at your own risk",
        detailsText = "Open $marker only if you know why.",
        continueLabel = "Open the site anyway",
        continueNote = "Vola will remember this for this site until it closes.",
        failedLabel = "The site couldn’t be opened.",
    )
    private val rows = listOf("Problem" to "It has expired", "Valid until" to "20 October 2026")

    @Test
    fun `page says why, lists the certificate and keeps the way past it behind details`() {
        val html = GeckoCertErrorPage.html(strings, host = "mail.example", rows = rows)

        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("The certificate of <strong>mail.example</strong> has expired."))
        assertTrue(html.contains("<span>Problem</span><b class=\"problem\">It has expired</b>"))
        assertTrue(html.contains("<span>Valid until</span><b>20 October 2026</b>"))
        assertTrue(html.contains(">Go back</button>"))
        assertTrue(html.contains("<section id=\"risk\" hidden>"))
        assertTrue(html.indexOf("id=\"details\"") < html.indexOf("id=\"continue\""))
    }

    @Test
    fun `the exception is temporary and a refusal is said`() {
        val html = GeckoCertErrorPage.html(strings, host = "mail.example", rows = rows)

        assertTrue(html.contains("document.addCertException(true)"))
        assertFalse(html.contains("addCertException(false)"))
        assertTrue(html.contains("role=\"alert\" hidden>The site couldn’t be opened.</p>"))
    }

    @Test
    fun `host, strings and certificate fields cannot inject markup`() {
        val html = GeckoCertErrorPage.html(
            strings.copy(title = "<b>title</b>"),
            host = "evil.example\"><script>alert(1)</script>",
            rows = listOf("Issued by" to "<img src=x onerror=alert(2)>"),
        )

        assertFalse(html.contains("<script>alert(1)</script>"))
        assertFalse(html.contains("<img"))
        assertFalse(html.contains("<b>title</b>"))
        assertTrue(html.contains("&lt;img src=x onerror=alert(2)&gt;"))
    }

    @Test
    fun `page takes the v5 colors in both themes`() {
        val (light, dark) = GeckoCertErrorPage.defaultPalettes
        val html = GeckoCertErrorPage.html(strings, host = "a.b", rows = rows)

        assertTrue(html.contains(":root { --bg: ${light.background};"))
        assertTrue(html.contains("@media (prefers-color-scheme: dark) { :root { --bg: ${dark.background};"))
        assertTrue(light.warning.matches(Regex("#[0-9A-F]{8}")))
    }
}
