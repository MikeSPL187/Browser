package dev.sk2andy.materialbrowser.browser.protection

import dev.sk2andy.materialbrowser.blocking.PrivacyRequestSanitizer
import dev.sk2andy.materialbrowser.blocking.SiteExceptionRules
import dev.sk2andy.materialbrowser.browser.BrowserController

/**
 * [SiteProtectionPort] over the browser's per-profile site settings (v5 site protection, V17). Off is
 * the site pause kept in the site exceptions; Strict and the per-site switches live in the site's
 * overrides. Private tabs keep their changes in memory only. Holds the pause logic that used to live
 * in [BrowserController].
 */
internal class BrowserSiteProtection(private val browser: BrowserController) : SiteProtectionPort {
    override fun state(tabId: String): SiteProtectionPanelState? {
        val tab = browser.tabs.firstOrNull { it.id == tabId } ?: return null
        val site = browser.siteProtectionState(tabId)
        val policy = browser.sitePolicyFor(tab, browser.pageUrls[tabId], site.isPaused)
        return SiteProtectionPanelState(
            host = site.host,
            level = policy.level,
            policy = policy,
            canPersist = site.canPersist,
            perSiteFingerprintingProtection = site.host != null &&
                browser.browserEngineCapabilities.perSiteFingerprintingProtection &&
                browser.supportsFingerprintingException(site.host),
            counts = SiteProtectionRules.counts(null, browser.privacySnapshot(tabId)),
        )
    }

    override fun counts(tabId: String, onResult: (SiteProtectionCounts) -> Unit) {
        browser.protectionCounts(tabId) { engine ->
            onResult(SiteProtectionRules.counts(engine, browser.privacySnapshot(tabId)))
        }
    }

    override fun setLevel(tabId: String, level: SiteProtectionLevel): Boolean {
        // The strict mark is written first, without a reload: pausing or resuming reloads the page.
        val marked = browser.updateSitePrivacyOverrides(tabId, reloadAffectedPages = false) { current, _ ->
            SiteProtectionRules.withLevel(current, level)
        }
        if (SiteProtectionRules.pausesSite(level)) return pause(tabId, persistently = true) || marked
        val resumed = resume(tabId)
        if (marked && !resumed) browser.reloadTabWithProtection(tabId)
        return marked || resumed
    }

    override fun setCookieBannersHidden(tabId: String, hidden: Boolean): Boolean =
        browser.setCookieBannerRemovalDisabled(tabId, disabled = !hidden)

    override fun setDeAmp(tabId: String, enabled: Boolean): Boolean =
        browser.updateSitePrivacyOverrides(tabId, reloadAffectedPages = false) { current, _ ->
            SiteProtectionRules.withDeAmp(current, enabled)
        }

    override fun setFingerprintingProtection(tabId: String, enabled: Boolean): Boolean =
        browser.updateSitePrivacyOverrides(tabId) { current, _ ->
            SiteProtectionRules.withFingerprintingProtection(current, enabled)
        }

    /**
     * Turns all protection off on the tab's site (level Off): for its profile when [persistently]
     * and the tab is not private, else for this tab only.
     */
    fun pause(tabId: String, persistently: Boolean): Boolean {
        val tab = browser.tabs.firstOrNull { it.id == tabId } ?: return false
        val host = PrivacyRequestSanitizer.webHost(browser.pageUrls[tabId] ?: tab.url) ?: return false
        if (persistently && SiteExceptionRules.mayPersist(tab.isIncognito)) {
            browser.permanentSiteExceptions = browser.permanentSiteExceptions + (
                tab.profileId to SiteExceptionRules.withException(
                    browser.permanentSiteExceptions[tab.profileId].orEmpty(),
                    host,
                )
            )
            browser.temporarySiteExceptions.computeIfPresent(tabId) { _, hosts ->
                hosts.filterNot { exception ->
                    SiteExceptionRules.isPaused(host, listOf(exception))
                }.toSet().takeIf(Set<String>::isNotEmpty)
            }
            browser.store.savePermanentSiteExceptions(browser.permanentSiteExceptions)
            browser.refreshProtectionForProfile(tab.profileId)
        } else browser.temporarySiteExceptions[tabId] = setOf(host)
        browser.siteExceptionRevision++
        browser.reloadTabWithProtection(tabId)
        return true
    }

    /** Lifts the pause of the tab's site, wherever it was set. */
    fun resume(tabId: String): Boolean {
        val tab = browser.tabs.firstOrNull { it.id == tabId } ?: return false
        val host = PrivacyRequestSanitizer.webHost(browser.pageUrls[tabId] ?: tab.url) ?: return false
        var changed = false
        var persistentChanged = false
        browser.temporarySiteExceptions.computeIfPresent(tabId) { _, hosts ->
            val retained = hosts.filterNot { exception ->
                SiteExceptionRules.isPaused(host, listOf(exception))
            }.toSet()
            changed = changed || retained.size != hosts.size
            retained.takeIf(Set<String>::isNotEmpty)
        }
        if (!tab.isIncognito) {
            val profileHosts = browser.permanentSiteExceptions[tab.profileId].orEmpty()
            val retained = profileHosts.filterNot { exception ->
                SiteExceptionRules.isPaused(host, listOf(exception))
            }.toSet()
            if (retained.size != profileHosts.size) {
                changed = true
                persistentChanged = true
                browser.permanentSiteExceptions = if (retained.isEmpty()) {
                    browser.permanentSiteExceptions - tab.profileId
                } else {
                    browser.permanentSiteExceptions + (tab.profileId to retained)
                }
                browser.store.savePermanentSiteExceptions(browser.permanentSiteExceptions)
            }
        }
        if (!changed) return false
        if (persistentChanged) browser.refreshProtectionForProfile(tab.profileId)
        browser.siteExceptionRevision++
        browser.reloadTabWithProtection(tabId)
        return true
    }
}
