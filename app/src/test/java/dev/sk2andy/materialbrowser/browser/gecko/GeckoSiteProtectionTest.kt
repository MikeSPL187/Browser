package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineCapabilities
import dev.sk2andy.materialbrowser.browser.gecko.GeckoContentBlockingCounts.BlockedEvent
import dev.sk2andy.materialbrowser.browser.protection.SiteProtectionCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.ContentBlockingController.Event

class GeckoSiteProtectionTest {
    private val registrable = mapOf(
        "maps.example.com" to "example.com",
        "www.bbc.co.uk" to "bbc.co.uk",
    )

    @Test
    fun `each excepted site switches every fingerprinting target off for its pages and frames`() {
        val value = GeckoFingerprintingExceptions.prefValue(listOf("maps.example.com")) { registrable[it] }

        assertEquals(
            """[{"firstPartyDomain":"example.com","overrides":"-AllTargets"},""" +
                """{"firstPartyDomain":"example.com","thirdPartyDomain":"*","overrides":"-AllTargets"}]""",
            value,
        )
    }

    @Test
    fun `domains are registrable, unique and sorted`() {
        val value = GeckoFingerprintingExceptions.prefValue(
            listOf("www.bbc.co.uk", "maps.example.com", "MAPS.EXAMPLE.COM", "solo.test"),
        ) { registrable[it] }

        val domains = Regex(""""firstPartyDomain":"([^"]+)"""").findAll(value).map { it.groupValues[1] }.distinct().toList()
        assertEquals(listOf("bbc.co.uk", "example.com", "solo.test"), domains)
    }

    @Test
    fun `no exception, or only unsafe hosts, restores gecko's default`() {
        assertEquals("", GeckoFingerprintingExceptions.prefValue(emptyList()) { null })
        assertEquals("", GeckoFingerprintingExceptions.prefValue(listOf("\"},{\"x", "localhost")) { null })
    }

    @Test
    fun `blocked sites count once per kind and logged-only events do not count`() {
        val counts = GeckoContentBlockingCounts.counts(
            listOf(
                BlockedEvent("https://ads.example", Event.BLOCKED_TRACKING_CONTENT, blocked = true),
                BlockedEvent("https://ads.example", Event.BLOCKED_TRACKING_CONTENT, blocked = true),
                BlockedEvent("https://pixel.example", Event.REPLACED_TRACKING_CONTENT, blocked = true),
                BlockedEvent("https://seen.example", Event.LOADED_LEVEL_1_TRACKING_CONTENT, blocked = false),
                BlockedEvent("https://fp.example", Event.BLOCKED_FINGERPRINTING_CONTENT, blocked = true),
                BlockedEvent("https://fp.example", Event.BLOCKED_SUSPICIOUS_FINGERPRINTING, blocked = true),
                BlockedEvent("https://miner.example", Event.BLOCKED_CRYPTOMINING_CONTENT, blocked = true),
                BlockedEvent("https://social.example", Event.BLOCKED_SOCIALTRACKING_CONTENT, blocked = true),
                BlockedEvent("https://social.example", Event.COOKIES_BLOCKED_SOCIALTRACKER, blocked = true),
                BlockedEvent("https://ads.example", Event.COOKIES_PARTITIONED_TRACKER, blocked = true),
                BlockedEvent(null, Event.BLOCKED_TRACKING_CONTENT, blocked = true),
            ),
        )

        assertEquals(
            SiteProtectionCounts(
                trackerSites = 2,
                fingerprinterSites = 1,
                cryptominerSites = 1,
                socialTrackerSites = 1,
                trackingCookieSites = 2,
            ),
            counts,
        )
    }

    @Test
    fun `no log is no counts`() {
        assertNull(GeckoContentBlockingCounts.fromLog(null))
    }

    @Test
    fun `only gecko turns fingerprinting protection off per site`() {
        assertTrue(AndroidBrowserEngineCapabilities.GeckoView.perSiteFingerprintingProtection)
        assertFalse(AndroidBrowserEngineCapabilities.SystemWebView.perSiteFingerprintingProtection)
    }
}
