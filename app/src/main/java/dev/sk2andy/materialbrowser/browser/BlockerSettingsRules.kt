package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.blocking.BlockerSettings

internal object BlockerSettingsRules {
    /**
     * Whether open pages need their privacy policy re-published. System WebView filters ads from
     * the cached session policy, so the ad switch alone must reach it; in Gecko the default
     * extension owns ad filtering and the session policy carries no ad rules.
     */
    fun refreshesSessionPolicy(
        current: BlockerSettings,
        next: BlockerSettings,
        sessionPolicyFiltersAds: Boolean,
    ): Boolean = current.blockThirdPartyCookies != next.blockThirdPartyCookies ||
        current.hideCookieConsent != next.hideCookieConsent ||
        (sessionPolicyFiltersAds && current.blockAdsAndTrackers != next.blockAdsAndTrackers)
}
