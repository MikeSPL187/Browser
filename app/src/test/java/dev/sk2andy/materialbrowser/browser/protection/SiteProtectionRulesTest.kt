package dev.sk2andy.materialbrowser.browser.protection

import dev.sk2andy.materialbrowser.blocking.BlockerSettings
import dev.sk2andy.materialbrowser.blocking.PrivacyXRaySnapshot
import dev.sk2andy.materialbrowser.blocking.SitePrivacyOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteProtectionRulesTest {
    private val defaults = BlockerSettings()

    @Test
    fun `a paused site is off, a strict mark is strict, anything else standard`() {
        val strict = SitePrivacyOverrides(strictProtection = true)

        assertEquals(SiteProtectionLevel.Off, SiteProtectionRules.level(paused = true, overrides = strict))
        assertEquals(SiteProtectionLevel.Strict, SiteProtectionRules.level(paused = false, overrides = strict))
        assertEquals(SiteProtectionLevel.Standard, SiteProtectionRules.level(paused = false, overrides = null))
    }

    @Test
    fun `off turns every protection of the site off`() {
        val policy = SiteProtectionRules.policy(SiteProtectionLevel.Off, defaults, overrides = null)

        assertFalse(policy.blockAdsAndTrackers)
        assertFalse(policy.hideCookieBanners)
        assertFalse(policy.deAmp)
        assertFalse(policy.fingerprintingProtection)
        assertFalse(policy.engineTrackingProtection)
        assertTrue(SiteProtectionRules.acceptsThirdPartyCookies(policy))
    }

    @Test
    fun `standard follows the global settings and the site's compatibility switches`() {
        val global = BlockerSettings(blockAdsAndTrackers = false, hideCookieConsent = true, blockThirdPartyCookies = true)
        val login = SitePrivacyOverrides(thirdPartyLoginAllowed = true)

        val policy = SiteProtectionRules.policy(SiteProtectionLevel.Standard, global, login)

        assertFalse(policy.blockAdsAndTrackers)
        assertFalse(policy.ignoreCompatibilityAllowLists)
        assertTrue(policy.hideCookieBanners)
        assertTrue(policy.blockThirdPartyCookies)
        assertTrue(SiteProtectionRules.acceptsThirdPartyCookies(policy))
        assertFalse(policy.trimCrossSiteReferrer)
        assertTrue(policy.engineTrackingProtection)
    }

    @Test
    fun `strict blocks fully whatever the global switches, without sign-in cookie exceptions`() {
        val global = BlockerSettings(blockAdsAndTrackers = false, hideCookieConsent = false, blockThirdPartyCookies = false)
        val overrides = SitePrivacyOverrides(strictProtection = true, thirdPartyLoginAllowed = true, captchaCompatibilityAllowed = true)

        val policy = SiteProtectionRules.policy(SiteProtectionLevel.Strict, global, overrides)

        assertTrue(policy.blockAdsAndTrackers)
        assertTrue(policy.ignoreCompatibilityAllowLists)
        assertTrue(policy.hideCookieBanners)
        assertFalse(SiteProtectionRules.acceptsThirdPartyCookies(policy))
        assertTrue(policy.trimCrossSiteReferrer)
    }

    @Test
    fun `the site's own switches hold on standard and strict`() {
        val overrides = SitePrivacyOverrides(
            cookieBannerRemovalDisabled = true,
            deAmpDisabled = true,
            fingerprintingProtectionDisabled = true,
        )

        for (level in listOf(SiteProtectionLevel.Standard, SiteProtectionLevel.Strict)) {
            val policy = SiteProtectionRules.policy(level, defaults, overrides)
            assertFalse("$level cookie banners", policy.hideCookieBanners)
            assertFalse("$level de-AMP", policy.deAmp)
            assertFalse("$level fingerprinting", policy.fingerprintingProtection)
        }
    }

    @Test
    fun `a bundled cookie banner exception applies until the user overrides it`() {
        val bundled = SiteProtectionRules.policy(
            SiteProtectionLevel.Standard,
            defaults,
            overrides = null,
            cookieBannerRemovalDisabledByDefault = true,
        )
        val overridden = SiteProtectionRules.policy(
            SiteProtectionLevel.Standard,
            defaults,
            SitePrivacyOverrides(cookieBannerRemovalDisabled = false),
            cookieBannerRemovalDisabledByDefault = true,
        )

        assertFalse(bundled.hideCookieBanners)
        assertTrue(overridden.hideCookieBanners)
    }

    @Test
    fun `levels and switches write only what differs from the default`() {
        val strict = SiteProtectionRules.withLevel(SitePrivacyOverrides(), SiteProtectionLevel.Strict)
        val back = SiteProtectionRules.withLevel(strict, SiteProtectionLevel.Standard)
        val noDeAmp = SiteProtectionRules.withDeAmp(SitePrivacyOverrides(), enabled = false)
        val noFingerprinting = SiteProtectionRules.withFingerprintingProtection(SitePrivacyOverrides(), enabled = false)

        assertEquals(true, strict.strictProtection)
        assertNull(back.strictProtection)
        assertTrue(back.isDefault)
        assertFalse(noDeAmp.isDefault)
        assertTrue(SiteProtectionRules.withDeAmp(noDeAmp, enabled = true).isDefault)
        assertFalse(noFingerprinting.isDefault)
        assertTrue(SiteProtectionRules.withFingerprintingProtection(noFingerprinting, enabled = true).isDefault)
        assertTrue(SiteProtectionRules.pausesSite(SiteProtectionLevel.Off))
        assertFalse(SiteProtectionRules.pausesSite(SiteProtectionLevel.Strict))
    }

    @Test
    fun `fingerprinting exceptions gather every profile and private tab`() {
        val personal = mapOf(
            "maps.example" to SitePrivacyOverrides(fingerprintingProtectionDisabled = true),
            "news.example" to SitePrivacyOverrides(strictProtection = true),
        )
        val privateTab = mapOf("bank.example" to SitePrivacyOverrides(fingerprintingProtectionDisabled = true))

        assertEquals(
            setOf("bank.example", "maps.example"),
            SiteProtectionRules.fingerprintingExceptionHosts(listOf(personal, privateTab)),
        )
    }

    @Test
    fun `counts add the blocker's requests to the engine log`() {
        val engine = SiteProtectionCounts(trackerSites = 3, fingerprinterSites = 1)
        val blocker = PrivacyXRaySnapshot(totalBlocked = 12)

        assertEquals(SiteProtectionCounts(trackerSites = 3, fingerprinterSites = 1, blockedRequests = 12), SiteProtectionRules.counts(engine, blocker))
        assertEquals(16, SiteProtectionRules.counts(engine, blocker).total)
        assertEquals(SiteProtectionCounts(blockedRequests = 12), SiteProtectionRules.counts(null, blocker))
    }

    @Test
    fun `levels keep stable ids for storage`() {
        SiteProtectionLevel.entries.forEach { level ->
            assertEquals(level, SiteProtectionLevel.fromStableId(level.stableId))
        }
        assertNull(SiteProtectionLevel.fromStableId("aggressive"))
    }
}
