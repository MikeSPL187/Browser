package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.GeckoRuntimeSettings

class GeckoHttpsOnlyErrorPageTest {
    private val strings = GeckoHttpsOnlyErrorPage.Strings(
        languageTag = "en",
        title = "Secure site not available",
        body = "Vola could not reach ${GeckoHttpsOnlyErrorPage.HOST_MARKER} over HTTPS.",
        advice = "Others can read what you send.",
        continueLabel = "Continue to HTTP site",
        backLabel = "Go back",
    )

    @Test
    fun `modes map to GeckoView HTTPS-only levels`() {
        assertEquals(
            GeckoRuntimeSettings.HTTPS_ONLY,
            GeckoHttpsOnlyRules.allowInsecureConnections(HttpsOnlyMode.Always),
        )
        assertEquals(
            GeckoRuntimeSettings.HTTPS_ONLY_PRIVATE,
            GeckoHttpsOnlyRules.allowInsecureConnections(HttpsOnlyMode.PrivateTabs),
        )
        assertEquals(
            GeckoRuntimeSettings.ALLOW_ALL,
            GeckoHttpsOnlyRules.allowInsecureConnections(HttpsOnlyMode.Off),
        )
    }

    @Test
    fun `page emphasizes the host and offers both actions`() {
        val html = GeckoHttpsOnlyErrorPage.html(strings, host = "neverssl.com")

        assertTrue(html.contains("Vola could not reach <strong>neverssl.com</strong> over HTTPS."))
        assertTrue(html.contains("document.reloadWithHttpsOnlyException()"))
        assertTrue(html.contains(">Go back</button>"))
        assertTrue(html.contains(">Continue to HTTP site</button>"))
        assertTrue(html.startsWith("<!DOCTYPE html>"))
    }

    @Test
    fun `host and strings cannot inject markup`() {
        val html = GeckoHttpsOnlyErrorPage.html(
            strings.copy(title = "<b>\"title\"</b>"),
            host = "evil.example\"><script>alert(1)</script>",
        )

        assertFalse(html.contains("<script>alert(1)</script>"))
        assertFalse(html.contains("<b>"))
        assertTrue(html.contains("evil.example&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;"))
        assertTrue(html.contains("&lt;b&gt;&quot;title&quot;&lt;/b&gt;"))
    }

    @Test
    fun `escape covers every HTML special character`() {
        assertEquals("&amp;&lt;&gt;&quot;&#39;", GeckoHttpsOnlyErrorPage.escape("&<>\"'"))
    }

    @Test
    fun `display host comes from the failed URI`() {
        assertEquals("example.com", GeckoHttpsOnlyErrorPage.displayHost("http://example.com/a?b"))
        assertEquals(
            "xn--e1afmkfd.xn--p1ai",
            GeckoHttpsOnlyErrorPage.displayHost("http://xn--e1afmkfd.xn--p1ai/"),
        )
        assertEquals("not a uri", GeckoHttpsOnlyErrorPage.displayHost("not a uri"))
        assertEquals("", GeckoHttpsOnlyErrorPage.displayHost(null))
    }

    @Test
    fun `data URI carries the page as UTF-8`() {
        val html = GeckoHttpsOnlyErrorPage.html(strings.copy(title = "Защищённая версия"), "a.b")
        val uri = GeckoHttpsOnlyErrorPage.dataUri(html)
        val prefix = "data:text/html;charset=utf-8;base64,"

        assertTrue(uri.startsWith(prefix))
        assertEquals(
            html,
            String(Base64.getDecoder().decode(uri.removePrefix(prefix)), StandardCharsets.UTF_8),
        )
    }
}
