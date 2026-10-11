package dev.sk2andy.materialbrowser.browser.protection

import dev.sk2andy.materialbrowser.blocking.SitePrivacyOverrideRules
import dev.sk2andy.materialbrowser.blocking.SitePrivacyOverrides
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-tab site overrides that live only in memory: every private tab's, and a regular tab's
 * "this time only" choice. The engine holds the fingerprinting exceptions they carry, so every
 * change, closing a tab or leaving private mode included, calls [onChange]; otherwise a private
 * site would stay unprotected in every tab after the private tab is gone.
 */
internal class TemporarySitePrivacyOverrides(private val onChange: () -> Unit) {
    private val byTab = ConcurrentHashMap<String, Map<String, SitePrivacyOverrides>>()

    operator fun get(tabId: String): Map<String, SitePrivacyOverrides>? = byTab[tabId]

    val values: Collection<Map<String, SitePrivacyOverrides>> get() = byTab.values

    operator fun set(tabId: String, byHost: Map<String, SitePrivacyOverrides>) {
        if (byHost.isEmpty()) byTab.remove(tabId) else byTab[tabId] = byHost
        onChange()
    }

    /** Drops [host]'s override in [tabId], when a persistent choice replaces it. */
    fun clearHost(tabId: String, host: String) {
        byTab.computeIfPresent(tabId) { _, overrides ->
            SitePrivacyOverrideRules.withOverride(
                current = overrides,
                host = host,
                overrides = SitePrivacyOverrides(),
            ).takeIf(Map<String, SitePrivacyOverrides>::isNotEmpty)
        }
        onChange()
    }

    fun remove(tabId: String) {
        if (byTab.remove(tabId) != null) onChange()
    }

    fun clear() {
        if (byTab.isEmpty()) return
        byTab.clear()
        onChange()
    }
}
