package dev.sk2andy.materialbrowser.recall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.BrowserTab

/**
 * Whether Recall is on, turning it off (which erases it first) and searching it from the address
 * bar (#500). Remembering a loaded page stays with the controller's page events and asks
 * [isActive]. Turning Recall off keeps it on until the erase succeeded.
 */
internal class RecallAccess(
    /** Stores the setting. */
    private val saveEnabled: (Boolean) -> Unit,
    /** Searches the stored pages; answers on the main thread. */
    private val search: (
        profileIds: Set<String>,
        query: String,
        limit: Int,
        onComplete: (List<RecallMatch>) -> Unit,
    ) -> Unit,
    /** Erases every stored page off the main thread and reports on it. */
    private val clearInBackground: (onCleared: (Boolean) -> Unit) -> Unit,
    private val host: Host,
) {
    interface Host {
        val isDestroyed: Boolean

        /** Clearing browsing data is under way: Recall neither shows nor stores anything. */
        val isBrowsingDataClearPending: Boolean
        val isHistorySuggestionsEnabled: Boolean
        val selectedTab: BrowserTab

        fun tab(tabId: String): BrowserTab?
    }

    var isEnabled by mutableStateOf(false)
        private set
    private var disablePending = false

    /** Recall may show and store pages now. */
    val isActive: Boolean
        get() = isEnabled && !disablePending && !host.isBrowsingDataClearPending

    /** At start: the stored setting. */
    fun restore(enabled: Boolean) {
        isEnabled = enabled
    }

    fun requestEnabled(enabled: Boolean) {
        if (disablePending || isEnabled == enabled) return
        if (enabled) {
            isEnabled = true
            saveEnabled(true)
            return
        }
        disablePending = true
        clearInBackground { cleared ->
            if (!host.isDestroyed && cleared) {
                isEnabled = false
                saveEnabled(false)
            }
            disablePending = false
        }
    }

    /**
     * Pages matching [query] typed into the selected tab's address bar, from its workspace only; empty
     * for a private tab, when Recall is off, and when the tab or the input changed meanwhile.
     */
    fun searchFromAddress(query: String, onComplete: (List<RecallMatch>) -> Unit) {
        val tab = host.selectedTab
        val explicit = RecallRules.isExplicitCommand(query)
        val recallQuery = if (explicit) RecallRules.explicitQuery(query) else RecallRules.addressQuery(query)
        if (
            !isActive || tab.isIncognito || recallQuery == null ||
            !RecallRules.canSearchFromAddress(query, host.isHistorySuggestionsEnabled)
        ) {
            onComplete(emptyList())
            return
        }
        val expectedTabId = tab.id
        val expectedProfileId = tab.profileId
        search(
            setOf(expectedProfileId),
            recallQuery,
            if (explicit) RecallRules.MAX_COMMAND_RESULTS else RecallRules.MAX_ADDRESS_RESULTS,
        ) { matches ->
            val current = host.tab(expectedTabId)
            val accepted = isActive &&
                RecallRules.canSearchFromAddress(query, host.isHistorySuggestionsEnabled) &&
                !host.isDestroyed &&
                host.selectedTab.id == expectedTabId &&
                current?.isIncognito == false &&
                current.profileId == expectedProfileId
            onComplete(if (accepted) matches else emptyList())
        }
    }
}
