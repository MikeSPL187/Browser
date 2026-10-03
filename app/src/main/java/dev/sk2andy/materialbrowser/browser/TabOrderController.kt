package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.data.TabPinningRules
import dev.sk2andy.materialbrowser.data.TabReorderingRules

/**
 * The order of the tabs in a workspace: manual moves, pinning, and where a tab an extension
 * created lands. Sorting tabs by recent use turns manual moves off. [BrowserController] keeps the
 * tab list itself; this only rearranges the active workspace's part of it.
 */
class TabOrderController internal constructor(
    private val tabs: MutableList<BrowserTab>,
    private val activeTabs: () -> List<BrowserTab>,
    private val activeProfileId: () -> String,
    private val automaticSorting: () -> Boolean,
    private val isEphemeral: (String) -> Boolean,
    private val onPinnedChanged: (tabId: String, pinned: Boolean) -> Unit,
    private val onOrderChanged: (profileId: String) -> Unit,
    private val persist: () -> Unit,
) {
    fun setPinned(tabId: String, isPinned: Boolean): Boolean {
        if (isEphemeral(tabId)) return false
        val current = activeTabs()
        val updated = TabPinningRules.withPinnedState(
            tabs = current,
            tabId = tabId,
            isPinned = isPinned,
        )
        if (updated == current) return false
        replaceProfileTabs(activeProfileId(), updated)
        onPinnedChanged(tabId, isPinned)
        persist()
        return true
    }

    fun move(tabId: String, destinationIndex: Int): Boolean {
        if (automaticSorting() || isEphemeral(tabId)) return false
        val current = activeTabs()
        val updated = TabReorderingRules.move(
            tabs = current,
            tabId = tabId,
            requestedIndex = destinationIndex,
        )
        if (updated == current) return false
        replaceProfileTabs(activeProfileId(), updated)
        onOrderChanged(activeProfileId())
        persist()
        return true
    }

    /** Puts a tab an extension just created at the index it asked for; false if it cannot. */
    fun positionCreatedTab(tabId: String, requestedIndex: Int): Boolean {
        if (automaticSorting() || isEphemeral(tabId)) return false
        val tab = tabs.firstOrNull { candidate -> candidate.id == tabId } ?: return false
        if (tab.profileId != activeProfileId()) return false
        val current = activeTabs()
        val destinationIndex = TabReorderingRules.clampedDestinationIndex(
            tabs = current,
            tabId = tabId,
            requestedIndex = requestedIndex,
        ) ?: return false
        if (current.indexOfFirst { candidate -> candidate.id == tabId } == destinationIndex) {
            return true
        }
        return move(tabId, requestedIndex)
    }

    /** Swaps one workspace's tabs for [orderedTabs], where that workspace's tabs were. */
    fun replaceProfileTabs(profileId: String, orderedTabs: List<BrowserTab>) {
        val insertionIndex = tabs.indexOfFirst { it.profileId == profileId }
            .takeIf { it >= 0 }
            ?: tabs.size
        tabs.removeAll { it.profileId == profileId }
        tabs.addAll(insertionIndex.coerceAtMost(tabs.size), orderedTabs)
    }
}
