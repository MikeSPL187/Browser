package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.MAX_PROFILES
import dev.sk2andy.materialbrowser.browser.TabStack
import dev.sk2andy.materialbrowser.browser.TabStackColor

/** Tab stacks, the tab groups of v5 (V12, as in Safari). */
object TabStackRules {
    /** Groups one profile may keep. */
    const val MAX_STACKS = 12

    /** Groups of all profiles together: what a stored session may hold. */
    const val MAX_STORED_STACKS = MAX_STACKS * MAX_PROFILES

    /** A group may hold a single tab, as in Safari; one left without tabs disappears. */
    const val MIN_MEMBER_COUNT = 1
    const val MAX_NAME_LENGTH = 40
    private const val MAX_ID_LENGTH = 128
    private val whitespace = Regex("\\s+")

    fun normalizedName(value: String): String = value
        .trim()
        .replace(whitespace, " ")
        .take(MAX_NAME_LENGTH)

    fun sanitized(
        stacks: List<TabStack>,
        tabs: Collection<BrowserTab>,
    ): List<TabStack> {
        val tabsById = tabs.associateBy(BrowserTab::id)
        val claimedTabIds = mutableSetOf<String>()
        val acceptedStackIds = mutableSetOf<String>()
        val stacksPerProfile = mutableMapOf<String, Int>()
        return buildList {
            stacks.take(MAX_STORED_STACKS).forEach { stack ->
                val name = normalizedName(stack.name)
                if (
                    stack.id.isBlank() ||
                    stack.id.length > MAX_ID_LENGTH ||
                    stack.profileId.isBlank() ||
                    name.isBlank() ||
                    stacksPerProfile.getOrElse(stack.profileId) { 0 } >= MAX_STACKS ||
                    !acceptedStackIds.add(stack.id)
                ) return@forEach

                val members = stack.tabIds.distinct().filter { tabId ->
                    val tab = tabsById[tabId]
                    tab != null &&
                        tab.profileId == stack.profileId &&
                        tabId !in claimedTabIds
                }
                if (members.size < MIN_MEMBER_COUNT) return@forEach
                val privacy = tabsById.getValue(members.first()).isIncognito
                val pinned = tabsById.getValue(members.first()).isPinned
                val samePrivacyMembers = members.filter { tabId ->
                    tabsById.getValue(tabId).isIncognito == privacy &&
                        tabsById.getValue(tabId).isPinned == pinned
                }
                if (samePrivacyMembers.size < MIN_MEMBER_COUNT) return@forEach
                val fallbackAnchorTabId = tabs
                    .firstOrNull { tab -> tab.id in samePrivacyMembers }
                    ?.id
                    ?: samePrivacyMembers.first()
                claimedTabIds += samePrivacyMembers
                stacksPerProfile[stack.profileId] = stacksPerProfile.getOrElse(stack.profileId) { 0 } + 1
                add(
                    stack.copy(
                        name = name,
                        tabIds = samePrivacyMembers,
                        previewTabId = stack.previewTabId
                            ?.takeIf(samePrivacyMembers::contains)
                            ?: samePrivacyMembers.first(),
                        collapsedAnchorTabId = stack.collapsedAnchorTabId
                            ?.takeIf(samePrivacyMembers::contains)
                            ?: fallbackAnchorTabId,
                    ),
                )
            }
        }
    }

    fun create(
        stacks: List<TabStack>,
        tabs: Collection<BrowserTab>,
        tabIds: List<String>,
        stackId: String,
        name: String,
        color: TabStackColor,
        previewTabId: String? = null,
    ): List<TabStack>? {
        val distinctTabIds = tabIds.distinct()
        if (distinctTabIds.size < MIN_MEMBER_COUNT) return null
        if (previewTabId != null && previewTabId !in distinctTabIds) return null
        val target = tabs.firstOrNull { it.id == distinctTabIds.first() } ?: return null
        val members = distinctTabIds.map { tabId ->
            tabs.firstOrNull { it.id == tabId } ?: return null
        }
        if (
            members.any { it.profileId != target.profileId } ||
            members.any { it.isIncognito != target.isIncognito } ||
            members.any { it.isPinned != target.isPinned }
        ) return null
        val normalizedName = normalizedName(name).takeIf(String::isNotBlank) ?: return null
        val withoutTargets = distinctTabIds.fold(stacks) { current, tabId ->
            removeTab(current, tabId)
        }
        if (withoutTargets.count { it.profileId == target.profileId } >= MAX_STACKS || stackId.isBlank()) {
            return null
        }
        return sanitized(
            stacks = withoutTargets + TabStack(
                id = stackId,
                profileId = target.profileId,
                name = normalizedName,
                color = color,
                tabIds = distinctTabIds,
                previewTabId = previewTabId,
            ),
            tabs = tabs,
        )
    }

    fun addTab(
        stacks: List<TabStack>,
        tabs: Collection<BrowserTab>,
        tabId: String,
        stackId: String,
    ): List<TabStack>? {
        val targetTab = tabs.firstOrNull { it.id == tabId } ?: return null
        val targetStack = stacks.firstOrNull { it.id == stackId } ?: return null
        if (tabId in targetStack.tabIds) return stacks
        val firstMember = targetStack.tabIds.firstNotNullOfOrNull { memberId ->
            tabs.firstOrNull { it.id == memberId }
        } ?: return null
        if (
            targetTab.profileId != targetStack.profileId ||
            targetTab.isIncognito != firstMember.isIncognito ||
            targetTab.isPinned != firstMember.isPinned
        ) return null
        val withoutTarget = removeTab(stacks, tabId)
        return sanitized(
            stacks = withoutTarget.map { stack ->
                if (stack.id == stackId) stack.copy(tabIds = stack.tabIds + tabId) else stack
            },
            tabs = tabs,
        )
    }

    fun update(
        stacks: List<TabStack>,
        tabs: Collection<BrowserTab>,
        stackId: String,
        tabIds: List<String>,
        name: String,
        color: TabStackColor,
        previewTabId: String? = null,
    ): List<TabStack>? {
        val targetStack = stacks.firstOrNull { it.id == stackId } ?: return null
        val distinctTabIds = tabIds.distinct()
        if (distinctTabIds.size < MIN_MEMBER_COUNT) return null
        if (previewTabId != null && previewTabId !in distinctTabIds) return null
        val members = distinctTabIds.map { tabId ->
            tabs.firstOrNull { it.id == tabId } ?: return null
        }
        val firstMember = members.first()
        if (
            firstMember.profileId != targetStack.profileId ||
            members.any { it.profileId != firstMember.profileId } ||
            members.any { it.isIncognito != firstMember.isIncognito } ||
            members.any { it.isPinned != firstMember.isPinned }
        ) return null
        val normalizedName = normalizedName(name).takeIf(String::isNotBlank) ?: return null
        val withoutTargetStack = stacks.filterNot { it.id == stackId }
        val withoutSelectedMembers = distinctTabIds.fold(withoutTargetStack) { current, tabId ->
            removeTab(current, tabId)
        }
        return sanitized(
            stacks = withoutSelectedMembers + targetStack.copy(
                name = normalizedName,
                color = color,
                tabIds = distinctTabIds,
                previewTabId = previewTabId ?: targetStack.previewTabId,
            ),
            tabs = tabs,
        )
    }

    /** Renames a group; null when there is no such group or the name is blank. */
    fun rename(stacks: List<TabStack>, stackId: String, name: String): List<TabStack>? {
        val normalizedName = normalizedName(name).takeIf(String::isNotBlank) ?: return null
        if (stacks.none { it.id == stackId }) return null
        return stacks.map { stack -> if (stack.id == stackId) stack.copy(name = normalizedName) else stack }
    }

    fun setColor(stacks: List<TabStack>, stackId: String, color: TabStackColor): List<TabStack>? {
        if (stacks.none { it.id == stackId }) return null
        return stacks.map { stack -> if (stack.id == stackId) stack.copy(color = color) else stack }
    }

    /** Drops a group and keeps its tabs, now ungrouped. */
    fun ungroup(stacks: List<TabStack>, stackId: String): List<TabStack>? =
        stacks.takeIf { current -> current.any { it.id == stackId } }?.filterNot { it.id == stackId }

    fun removeTab(stacks: List<TabStack>, tabId: String): List<TabStack> = stacks
        .map { stack ->
            val members = stack.tabIds.filterNot(tabId::equals)
            stack.copy(
                tabIds = members,
                previewTabId = stack.previewTabId?.takeIf(members::contains)
                    ?: members.firstOrNull(),
                collapsedAnchorTabId = stack.collapsedAnchorTabId
                    ?.takeIf(members::contains)
                    ?: members.firstOrNull(),
            )
        }
        .filter { stack -> stack.tabIds.size >= MIN_MEMBER_COUNT }

    fun removeTabs(stacks: List<TabStack>, tabIds: Set<String>): List<TabStack> = stacks
        .map { stack ->
            val members = stack.tabIds.filterNot(tabIds::contains)
            stack.copy(
                tabIds = members,
                previewTabId = stack.previewTabId?.takeIf(members::contains)
                    ?: members.firstOrNull(),
                collapsedAnchorTabId = stack.collapsedAnchorTabId
                    ?.takeIf(members::contains)
                    ?: members.firstOrNull(),
            )
        }
        .filter { stack -> stack.tabIds.size >= MIN_MEMBER_COUNT }

    fun persistent(
        stacks: List<TabStack>,
        tabs: Collection<BrowserTab>,
    ): List<TabStack> {
        val tabsById = tabs.associateBy(BrowserTab::id)
        val safeStacks = stacks.filter { stack ->
            val members = stack.tabIds.mapNotNull(tabsById::get)
            members.size == stack.tabIds.size && members.none(BrowserTab::isIncognito)
        }
        return sanitized(safeStacks, tabs.filterNot(BrowserTab::isIncognito))
    }

    fun toggleCollapsed(
        stacks: List<TabStack>,
        stackId: String,
        triggerTabId: String? = null,
    ): List<TabStack>? {
        val target = stacks.firstOrNull { it.id == stackId } ?: return null
        if (triggerTabId != null && triggerTabId !in target.tabIds) {
            return null
        }
        return stacks.map { stack ->
            if (stack.id != stackId) return@map stack
            stack.copy(
                isCollapsed = !stack.isCollapsed,
                collapsedAnchorTabId = if (stack.isCollapsed) {
                    stack.collapsedAnchorTabId
                } else {
                    triggerTabId
                        ?: stack.collapsedAnchorTabId?.takeIf(stack.tabIds::contains)
                        ?: stack.tabIds.firstOrNull()
                },
            )
        }
    }

    fun setPreviewTab(
        stacks: List<TabStack>,
        stackId: String,
        tabId: String,
    ): List<TabStack>? {
        val target = stacks.firstOrNull { it.id == stackId } ?: return null
        if (tabId !in target.tabIds) return null
        return stacks.map { stack ->
            if (stack.id == stackId) stack.copy(previewTabId = tabId) else stack
        }
    }

    fun restoreSnoozedMember(
        stacks: List<TabStack>,
        tabs: Collection<BrowserTab>,
        restoredTabId: String,
        originalStack: TabStack,
        stackAfterSnooze: TabStack?,
    ): List<TabStack> {
        val currentStack = stacks.firstOrNull { it.id == originalStack.id }
        val restoredStack = when {
            currentStack == stackAfterSnooze -> originalStack
            currentStack == null -> return sanitized(stacks, tabs)
            else -> currentStack.copy(
                tabIds = insertRelativeToOriginal(
                    currentIds = currentStack.tabIds,
                    originalIds = originalStack.tabIds,
                    restoredTabId = restoredTabId,
                ),
            )
        }
        return sanitized(
            stacks = stacks.filterNot { it.id == originalStack.id } + restoredStack,
            tabs = tabs,
        )
    }

    private fun insertRelativeToOriginal(
        currentIds: List<String>,
        originalIds: List<String>,
        restoredTabId: String,
    ): List<String> {
        if (restoredTabId in currentIds) return currentIds
        val originalIndex = originalIds.indexOf(restoredTabId)
        if (originalIndex < 0) return currentIds
        val nextId = originalIds
            .drop(originalIndex + 1)
            .firstOrNull(currentIds::contains)
        if (nextId != null) {
            val insertionIndex = currentIds.indexOf(nextId)
            return currentIds.toMutableList().apply { add(insertionIndex, restoredTabId) }
        }
        val previousId = originalIds
            .take(originalIndex)
            .lastOrNull(currentIds::contains)
        val insertionIndex = previousId?.let(currentIds::indexOf)?.plus(1) ?: currentIds.size
        return currentIds.toMutableList().apply { add(insertionIndex, restoredTabId) }
    }

    fun visibleTabs(
        tabs: List<BrowserTab>,
        stacks: List<TabStack>,
    ): List<BrowserTab> {
        val visibleIds = tabs.mapTo(linkedSetOf(), BrowserTab::id)
        stacks.filter(TabStack::isCollapsed).forEach { stack ->
            val activeMembers = tabs.filter { it.id in stack.tabIds }
            val representative = activeMembers
                .firstOrNull { tab -> tab.id == stack.collapsedAnchorTabId }
                ?: activeMembers.firstOrNull()
                ?: return@forEach
            activeMembers.forEach { member ->
                if (member.id != representative.id) visibleIds.remove(member.id)
            }
        }
        return tabs.filter { it.id in visibleIds }
    }

    fun visibleTabId(
        tabId: String,
        tabs: List<BrowserTab>,
        stacks: List<TabStack>,
    ): String {
        val collapsedStack = stacks.firstOrNull { stack ->
            stack.isCollapsed && tabId in stack.tabIds
        } ?: return tabId
        return tabs.firstOrNull { tab ->
            tab.id == collapsedStack.collapsedAnchorTabId && tab.id in collapsedStack.tabIds
        }?.id
            ?: tabs.firstOrNull { tab -> tab.id in collapsedStack.tabIds }?.id
            ?: tabId
    }
}
