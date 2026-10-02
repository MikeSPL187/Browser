package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.InactiveTabArchiveRules
import dev.sk2andy.materialbrowser.data.InactiveTabLifetime
import dev.sk2andy.materialbrowser.data.SnoozedTab
import dev.sk2andy.materialbrowser.data.SnoozedTabStore

/**
 * Auto-archive of inactive tabs: what «Close tabs automatically» would close for age waits in
 * Snoozed instead. On by default, yet the lifetime itself defaults to never, so nothing is archived
 * until the owner picks one.
 */
class InactiveTabArchive internal constructor(
    private val store: BrowserSessionStore,
    private val snoozedTabStore: SnoozedTabStore,
    private val snoozedTabs: SnapshotStateList<SnoozedTab>,
    private val isArchivable: (BrowserTab) -> Boolean,
) {
    var enabled by mutableStateOf(store.loadArchiveInactiveTabs())
        private set

    fun updateEnabled(value: Boolean) {
        enabled = value
        store.saveArchiveInactiveTabs(value)
    }

    /**
     * Archives what it can of [staleIds] and returns the ids to close: all of them once the archive
     * is saved; if saving fails, the tabs meant for it stay open rather than being lost.
     */
    fun closeOrArchive(
        tabs: List<BrowserTab>,
        staleIds: Set<String>,
        lifetime: InactiveTabLifetime,
        nowMillis: Long,
    ): Set<String> {
        if (staleIds.isEmpty()) return staleIds
        if (!InactiveTabArchiveRules.applies(lifetime, enabled)) return staleIds
        val stale = tabs.filter { tab ->
            tab.id in staleIds && InactiveTabArchiveRules.canArchive(tab) && isArchivable(tab)
        }
        if (stale.isEmpty()) return staleIds
        val updated = InactiveTabArchiveRules.archived(stale, snoozedTabs, nowMillis)
        if (!snoozedTabStore.save(updated)) return staleIds - stale.map(BrowserTab::id).toSet()
        snoozedTabs.clear()
        snoozedTabs += updated
        return staleIds
    }
}
