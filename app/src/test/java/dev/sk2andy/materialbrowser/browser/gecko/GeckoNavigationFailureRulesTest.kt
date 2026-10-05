package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.geckoview.WebRequestError

class GeckoNavigationFailureRulesTest {
    @Test
    fun `offline error stays distinct from unknown host`() {
        assertEquals(
            BrowserEngineFailureKind.Offline,
            GeckoNavigationFailureRules.kindForErrorCode(WebRequestError.ERROR_OFFLINE),
        )
        assertEquals(
            BrowserEngineFailureKind.UnknownHost,
            GeckoNavigationFailureRules.kindForErrorCode(WebRequestError.ERROR_UNKNOWN_HOST),
        )
    }

    @Test
    fun `certificate and TLS failures mean an insecure connection`() {
        assertEquals(
            BrowserEngineFailureKind.InsecureConnection,
            GeckoNavigationFailureRules.kindForErrorCode(WebRequestError.ERROR_SECURITY_BAD_CERT),
        )
        assertEquals(
            BrowserEngineFailureKind.InsecureConnection,
            GeckoNavigationFailureRules.kindForErrorCode(WebRequestError.ERROR_SECURITY_SSL),
        )
    }

    @Test
    fun `safe browsing blocks mean a dangerous site`() {
        listOf(
            WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI,
            WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI,
            WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI,
            WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI,
        ).forEach { code ->
            assertEquals(
                BrowserEngineFailureKind.DangerousSite,
                GeckoNavigationFailureRules.kindForErrorCode(code),
            )
        }
    }

    @Test
    fun `other transport errors remain generic`() {
        assertEquals(
            BrowserEngineFailureKind.Other,
            GeckoNavigationFailureRules.kindForErrorCode(WebRequestError.ERROR_NET_TIMEOUT),
        )
    }
}
