package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.BrowserTab

internal object TabRetentionRules {
    fun tabIdsToCloseOnBackground(
        tabs: List<BrowserTab>,
        lifetime: InactiveTabLifetime,
    ): Set<String> {
        if (lifetime != InactiveTabLifetime.Immediately) return emptySet()
        return tabs.asSequence()
            .mapTo(linkedSetOf(), BrowserTab::id)
    }

    fun tabIdsToCloseOnTaskRemoval(
        tabs: List<BrowserTab>,
        lifetime: InactiveTabLifetime,
    ): Set<String> {
        if (
            lifetime != InactiveTabLifetime.Immediately &&
            lifetime != InactiveTabLifetime.WhenAppCloses
        ) {
            return emptySet()
        }
        return tabs.asSequence()
            .mapTo(linkedSetOf(), BrowserTab::id)
    }

    /**
     * The tabs not used for longer than [lifetime]. The selected tab and Split View's
     * [companionTabId], both on screen, never count as inactive.
     */
    fun expiredTabIds(
        tabs: List<BrowserTab>,
        selectedTabId: String?,
        lifetime: InactiveTabLifetime,
        nowMillis: Long,
        companionTabId: String? = null,
    ): Set<String> {
        val maxAgeMillis = lifetime.maxAgeMillis ?: return emptySet()
        if (tabs.size <= 1) return emptySet()
        val protectedTabId = selectedTabId
            ?.takeIf { selected -> tabs.any { it.id == selected } }
            ?: tabs.maxByOrNull(BrowserTab::lastAccessedAt)?.id
        val cutoff = nowMillis - maxAgeMillis
        return tabs.asSequence()
            .filter { it.id != protectedTabId && it.id != companionTabId }
            .filter(TabDeletionRules::canDelete)
            .filter { it.lastAccessedAt > 0L && it.lastAccessedAt < cutoff }
            .map(BrowserTab::id)
            .toSet()
    }
}
