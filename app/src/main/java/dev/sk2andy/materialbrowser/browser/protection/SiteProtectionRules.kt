package dev.sk2andy.materialbrowser.browser.protection

import dev.sk2andy.materialbrowser.blocking.BlockerSettings
import dev.sk2andy.materialbrowser.blocking.PrivacyXRaySnapshot
import dev.sk2andy.materialbrowser.blocking.SitePrivacyOverrideRules
import dev.sk2andy.materialbrowser.blocking.SitePrivacyOverrides

/** Engine-free decisions of per-site protection, shared by the panel, the blocker and the engines. */
internal object SiteProtectionRules {
    /** The level the site is at: a paused site is Off, a strict override Strict, the rest Standard. */
    fun level(paused: Boolean, overrides: SitePrivacyOverrides?): SiteProtectionLevel = when {
        paused -> SiteProtectionLevel.Off
        overrides?.strictProtection == true -> SiteProtectionLevel.Strict
        else -> SiteProtectionLevel.Standard
    }

    /**
     * What protects the site's pages. Off turns everything off. Standard follows [global] and the
     * site's switches. Strict turns the blocker and cookie protection fully on whatever [global]
     * says, but keeps the switches the user set for this site (cookie banners, de-AMP,
     * fingerprinting), since those are how a broken site is fixed without leaving Strict.
     */
    fun policy(
        level: SiteProtectionLevel,
        global: BlockerSettings,
        overrides: SitePrivacyOverrides?,
        cookieBannerRemovalDisabledByDefault: Boolean = false,
    ): SiteProtectionPolicy {
        val cookieBannerRemovalDisabled = SitePrivacyOverrideRules.cookieBannerRemovalDisabled(
            overrides = overrides,
            bundledDefault = cookieBannerRemovalDisabledByDefault,
        )
        val deAmp = overrides?.deAmpDisabled != true
        val fingerprinting = overrides?.fingerprintingProtectionDisabled != true
        return when (level) {
            SiteProtectionLevel.Off -> SiteProtectionPolicy(
                level = level,
                blockAdsAndTrackers = false,
                ignoreCompatibilityAllowLists = false,
                blockThirdPartyCookies = false,
                allowLoginAndCaptchaCookies = true,
                hideCookieBanners = false,
                deAmp = false,
                fingerprintingProtection = false,
                trimCrossSiteReferrer = false,
                engineTrackingProtection = false,
            )
            SiteProtectionLevel.Standard -> SiteProtectionPolicy(
                level = level,
                blockAdsAndTrackers = global.blockAdsAndTrackers,
                ignoreCompatibilityAllowLists = false,
                blockThirdPartyCookies = global.blockThirdPartyCookies,
                allowLoginAndCaptchaCookies = SitePrivacyOverrideRules.thirdPartyLoginAllowed(overrides) ||
                    SitePrivacyOverrideRules.captchaCompatibilityAllowed(overrides),
                hideCookieBanners = global.hideCookieConsent && !cookieBannerRemovalDisabled,
                deAmp = deAmp,
                fingerprintingProtection = fingerprinting,
                trimCrossSiteReferrer = false,
                engineTrackingProtection = true,
            )
            SiteProtectionLevel.Strict -> SiteProtectionPolicy(
                level = level,
                blockAdsAndTrackers = true,
                ignoreCompatibilityAllowLists = true,
                blockThirdPartyCookies = true,
                allowLoginAndCaptchaCookies = false,
                hideCookieBanners = !cookieBannerRemovalDisabled,
                deAmp = deAmp,
                fingerprintingProtection = fingerprinting,
                trimCrossSiteReferrer = true,
                engineTrackingProtection = true,
            )
        }
    }

    /**
     * The site's overrides at [level]. Only Strict is kept here; Off is the site pause, which the
     * caller stores in the site exceptions, so it clears the strict mark too.
     */
    fun withLevel(overrides: SitePrivacyOverrides, level: SiteProtectionLevel): SitePrivacyOverrides =
        overrides.copy(strictProtection = true.takeIf { level == SiteProtectionLevel.Strict })

    /** Whether the site goes to (or stays in) the site exceptions, which pause all protection. */
    fun pausesSite(level: SiteProtectionLevel): Boolean = level == SiteProtectionLevel.Off

    fun withDeAmp(overrides: SitePrivacyOverrides, enabled: Boolean): SitePrivacyOverrides =
        overrides.copy(deAmpDisabled = true.takeUnless { enabled })

    fun withFingerprintingProtection(overrides: SitePrivacyOverrides, enabled: Boolean): SitePrivacyOverrides =
        overrides.copy(fingerprintingProtectionDisabled = true.takeUnless { enabled })

    /**
     * Whether the consent handling runs on the site: off when paused, always on Strict, else as the
     * global switch says. A site's own cookie-banner switch then decides whether banners are removed.
     */
    fun handlesCookieConsent(policy: SiteProtectionPolicy, global: BlockerSettings): Boolean =
        policy.level != SiteProtectionLevel.Off &&
            (policy.level == SiteProtectionLevel.Strict || global.hideCookieConsent)

    /** Whether the site's pages accept third-party cookies under [policy]. */
    fun acceptsThirdPartyCookies(policy: SiteProtectionPolicy): Boolean =
        !policy.blockThirdPartyCookies || policy.allowLoginAndCaptchaCookies

    /**
     * Sites whose fingerprinting protection is off, across [overrideMaps] (every profile and every
     * private tab). The engine keeps one list for the whole runtime, so a site set in one profile is
     * excepted in all; the panel says so.
     */
    fun fingerprintingExceptionHosts(overrideMaps: Iterable<Map<String, SitePrivacyOverrides>>): Set<String> =
        overrideMaps.asSequence()
            .flatMap { byHost -> byHost.asSequence() }
            .filter { (_, overrides) -> overrides.fingerprintingProtectionDisabled == true }
            .map { (host, _) -> host }
            .toSortedSet()

    /** The panel's counts: the engine's log, when the engine keeps one, plus the blocker's requests. */
    fun counts(engine: SiteProtectionCounts?, blocker: PrivacyXRaySnapshot): SiteProtectionCounts =
        (engine ?: SiteProtectionCounts()).copy(blockedRequests = blocker.totalBlocked)
}
