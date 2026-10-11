package dev.sk2andy.materialbrowser.browser.protection

/**
 * How hard Vola protects one site (v5, site protection panel V17; levels after Brave's, owner's
 * decision 2026-10-11). [Off] is the existing site pause: no blocking, no engine tracking
 * protection. [Standard] follows the global protection settings. [Strict] adds what can break
 * sites: third-party cookies stay blocked even for sign-in and captcha providers, the blocker
 * ignores its compatibility allow lists, cross-site referrers are trimmed and cookie banners are
 * removed whatever the global switch says.
 */
internal enum class SiteProtectionLevel(val stableId: String) {
    Off("off"),
    Standard("standard"),
    Strict("strict"),
    ;

    companion object {
        fun fromStableId(value: String?): SiteProtectionLevel? = entries.firstOrNull { it.stableId == value }
    }
}

/**
 * What protects a page of the site, resolved from the level, the user's switches for the site and
 * the global settings ([SiteProtectionRules.policy]). The blocker and the engine apply this.
 */
internal data class SiteProtectionPolicy(
    val level: SiteProtectionLevel,
    val blockAdsAndTrackers: Boolean,
    /** Strict only: the blocker drops its compatibility allow lists for this site. */
    val ignoreCompatibilityAllowLists: Boolean,
    val blockThirdPartyCookies: Boolean,
    /** Sign-in and captcha providers may set third-party cookies; never on [SiteProtectionLevel.Strict]. */
    val allowLoginAndCaptchaCookies: Boolean,
    val hideCookieBanners: Boolean,
    val deAmp: Boolean,
    val fingerprintingProtection: Boolean,
    val trimCrossSiteReferrer: Boolean,
    /** Engine tracking protection (Gecko ETP); off only with the site paused. */
    val engineTrackingProtection: Boolean,
)

/**
 * What was stopped on the current page, for the panel: the engine's own log (Gecko's content
 * blocking log, counted per blocked site) and Vola's blocker (requests).
 */
internal data class SiteProtectionCounts(
    val trackerSites: Int = 0,
    val fingerprinterSites: Int = 0,
    val cryptominerSites: Int = 0,
    val socialTrackerSites: Int = 0,
    val trackingCookieSites: Int = 0,
    val blockedRequests: Int = 0,
) {
    /** Distinct things the panel can sum up as «blocked», requests plus engine-blocked sites. */
    val total: Int
        get() = blockedRequests + trackerSites + fingerprinterSites + cryptominerSites + socialTrackerSites +
            trackingCookieSites
}

/** Everything the panel shows for the tab's site. Null host means a page without a web site. */
internal data class SiteProtectionPanelState(
    val host: String?,
    val level: SiteProtectionLevel,
    val policy: SiteProtectionPolicy,
    /** False in private tabs: changes last until the tab closes. */
    val canPersist: Boolean,
    /** Whether the engine can turn fingerprinting protection off for one site (Gecko only). */
    val perSiteFingerprintingProtection: Boolean,
    val counts: SiteProtectionCounts,
)

/**
 * The site protection panel's port (V17). The browser implements it over its per-profile site
 * settings; private tabs keep their changes in memory only. Every setter returns whether
 * something changed; a change reloads the site's open pages so it applies at once.
 */
internal interface SiteProtectionPort {
    fun state(tabId: String): SiteProtectionPanelState?

    fun setLevel(tabId: String, level: SiteProtectionLevel): Boolean

    fun setCookieBannersHidden(tabId: String, hidden: Boolean): Boolean

    fun setDeAmp(tabId: String, enabled: Boolean): Boolean

    fun setFingerprintingProtection(tabId: String, enabled: Boolean): Boolean
}
