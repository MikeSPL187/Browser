package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5SiteProtectionRulesTest {
    private val gecko = V5SiteProtectionRules.Report(
        host = "news.example",
        engine = "GeckoView",
        appVersion = "0.4.0",
        protectionOn = true,
        strict = false,
        blocked = 23,
        thirdPartyCookiesAllowed = false,
        cookieBannersHidden = true,
        deAmp = true,
        fingerprintingProtection = true,
        httpsOnly = "always",
    )

    @Test
    fun `the report lists the site's settings for an issue`() {
        assertEquals(
            """
            ### Site breakage report

            - Site: news.example
            - Vola: 0.4.0 (GeckoView)
            - Protection: on, level Standard
            - Blocked on this page: 23
            - Third-party cookies: blocked
            - Cookie banners: hidden
            - AMP: original page
            - Fingerprinting protection: on
            - HTTPS-only: always

            What goes wrong:
            """.trimIndent() + "\n",
            V5SiteProtectionRules.report(gecko),
        )
    }

    @Test
    fun `settings the engine does not offer stay out of the report`() {
        val report = V5SiteProtectionRules.report(
            gecko.copy(
                engine = "System WebView",
                strict = true,
                protectionOn = false,
                thirdPartyCookiesAllowed = null,
                cookieBannersHidden = null,
                deAmp = null,
                fingerprintingProtection = null,
                httpsOnly = null,
            ),
        )
        assertTrue(report.contains("- Protection: off, level Strict"))
        assertFalse(report.contains("cookies"))
        assertFalse(report.contains("AMP"))
        assertFalse(report.contains("Fingerprinting"))
        assertFalse(report.contains("HTTPS"))
    }
}
