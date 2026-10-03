package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.sk2andy.materialbrowser.data.TabStackRules
import java.util.UUID

/**
 * Tab stacks (the overview's groups): their state and every edit to them. [BrowserController]
 * loads and saves them with the session and keeps them in step when a tab closes or is snoozed.
 */
class TabStacksController internal constructor(
    private val allTabs: () -> List<BrowserTab>,
    private val activeTabs: () -> List<BrowserTab>,
    private val persist: () -> Unit,
) {
    val stacks: SnapshotStateList<TabStack> = mutableStateListOf()

    /** The stacks of the current workspace, with members that are gone left out. */
    val activeStacks: List<TabStack>
        get() = TabStackRules.sanitized(stacks, activeTabs())

    /** The workspace's tabs as the overview shows them: a collapsed stack shows one card. */
    val overviewTabs: List<BrowserTab>
        get() = TabStackRules.visibleTabs(tabs = activeTabs(), stacks = activeStacks)

    fun stackFor(tabId: String): TabStack? =
        activeStacks.firstOrNull { stack -> tabId in stack.tabIds }

    fun overviewTabId(tabId: String): String = TabStackRules.visibleTabId(
        tabId = tabId,
        tabs = activeTabs(),
        stacks = activeStacks,
    )

    fun create(
        tabIds: List<String>,
        name: String,
        color: TabStackColor,
        previewTabId: String? = null,
    ): String? {
        if (!allActive(tabIds)) return null
        val stackId = UUID.randomUUID().toString()
        val updated = TabStackRules.create(
            stacks = stacks,
            tabs = allTabs(),
            tabIds = tabIds,
            stackId = stackId,
            name = name,
            color = color,
            previewTabId = previewTabId,
        )
        return stackId.takeIf { commit(updated) }
    }

    fun addTab(tabId: String, stackId: String): Boolean = commit(
        TabStackRules.addTab(stacks = stacks, tabs = allTabs(), tabId = tabId, stackId = stackId),
    )

    fun update(
        stackId: String,
        tabIds: List<String>,
        name: String,
        color: TabStackColor,
        previewTabId: String? = null,
    ): Boolean = allActive(tabIds) && commit(
        TabStackRules.update(
            stacks = stacks,
            tabs = allTabs(),
            stackId = stackId,
            tabIds = tabIds,
            name = name,
            color = color,
            previewTabId = previewTabId,
        ),
    )

    fun removeTab(tabId: String): Boolean = commit(TabStackRules.removeTab(stacks, tabId))

    fun toggleCollapsed(stackId: String, triggerTabId: String? = null): Boolean = commit(
        TabStackRules.toggleCollapsed(
            stacks = stacks,
            stackId = stackId,
            triggerTabId = triggerTabId,
        ),
    )

    fun setPreview(stackId: String, tabId: String): Boolean =
        activeStacks.any { stack -> stack.id == stackId && tabId in stack.tabIds } &&
            commit(TabStackRules.setPreviewTab(stacks, stackId, tabId))

    /** Keeps [updated] and saves the session; nothing happens when it is null or unchanged. */
    private fun commit(updated: List<TabStack>?): Boolean {
        if (updated == null || updated == stacks) return false
        stacks.clear()
        stacks += updated
        persist()
        return true
    }

    private fun allActive(tabIds: List<String>): Boolean {
        val activeTabIds = activeTabs().mapTo(hashSetOf(), BrowserTab::id)
        return tabIds.all { it in activeTabIds }
    }
}
