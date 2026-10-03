package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.BrowserTab

/**
 * Auto-archive, as in Arc and Firefox: tabs closed for age go to Snoozed, with no wake time.
 * The lifetimes that close every tab when Vola leaves stay closing: they are a privacy choice.
 */
internal object InactiveTabArchiveRules {
    fun applies(lifetime: InactiveTabLifetime, enabled: Boolean): Boolean =
        enabled && lifetime.maxAgeMillis != null

    /** Private tabs are never written down, so they are closed rather than archived. */
    fun canArchive(tab: BrowserTab): Boolean = !tab.isIncognito

    /** [snoozed] with [stale] added as archived; a tab archived again replaces its old entry. */
    fun archived(
        stale: List<BrowserTab>,
        snoozed: List<SnoozedTab>,
        nowMillis: Long,
    ): List<SnoozedTab> {
        val staleIds = stale.mapTo(hashSetOf(), BrowserTab::id)
        val added = stale.filter(::canArchive).map { tab ->
            SnoozedTab(
                tab = tab.copy(progress = 0, isLoading = false, error = null),
                wakeAtMillis = SnoozedTab.ARCHIVED_WAKE_AT_MILLIS,
                createdAtMillis = nowMillis,
            )
        }
        return (snoozed.filterNot { it.tab.id in staleIds } + added)
            .sortedWith(compareBy<SnoozedTab>({ it.wakeAtMillis }, { it.tab.id }))
    }
}
