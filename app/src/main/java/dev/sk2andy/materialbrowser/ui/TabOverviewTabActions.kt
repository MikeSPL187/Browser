package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.CanonicalWebUrl
import dev.sk2andy.materialbrowser.data.EssentialsRules
import dev.sk2andy.materialbrowser.data.TabDeletionRules
import dev.sk2andy.materialbrowser.data.TabStackRules

/**
 * The tab actions sheet of the tab overview. It works out what [tabId] allows and runs the actions
 * that need nothing from the overview; pinning, moving, the trail and closing come back to the
 * overview, which animates them. Every action closes the sheet first, except the site mute switch.
 */
@Composable
internal fun TabOverviewTabActions(
    controller: BrowserController,
    tabId: String?,
    onTogglePinned: (BrowserTab) -> Unit,
    onMoveToWorkspace: (BrowserTab, String) -> Unit,
    onOpenCandyTrail: (BrowserTab) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onAddSiteCapsule: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onEditStack: (String) -> Unit,
    onClose: (BrowserTab) -> Unit,
    onDismiss: () -> Unit,
) {
    val rootView = LocalView.current
    val tab = tabId?.let { id -> controller.activeTabs.firstOrNull { it.id == id } }
    val workspaces = controller.profiles.take(if (controller.profilesEnabled) Int.MAX_VALUE else 1)
    val essentialId = tab?.let { CanonicalWebUrl.key(it.url) }
    val workspaceEssentials = tab?.let { controller.essentials.entriesFor(it.profileId) }.orEmpty()
    val favicon = tab?.let { controller.favicons[it.id] }
    val stack = tab?.let { controller.tabStackController.stackFor(it.id) }
    val stackCandidates = tab?.let { target ->
        controller.activeTabs.filter { candidate ->
            candidate.profileId == target.profileId &&
                candidate.isIncognito == target.isIncognito &&
                candidate.isPinned == target.isPinned
        }
    }.orEmpty()
    val stackCandidateIds = stackCandidates.mapTo(hashSetOf(), BrowserTab::id)
    val otherStacks = controller.tabStackController.activeStacks.filter { candidate ->
        candidate.id != stack?.id && candidate.tabIds.any(stackCandidateIds::contains)
    }
    val canCreateStack = stack != null ||
        (stackCandidates.size >= TabStackRules.MIN_MEMBER_COUNT &&
            controller.tabStacks.size < TabStackRules.MAX_STACKS)
    val facts = tab?.let {
        TabActionsFacts(
            isWebPage = it.url != BLANK_URL,
            isHttpPage = essentialId != null,
            isIncognito = it.isIncognito,
            isPinned = it.isPinned,
            isEssential = workspaceEssentials.any { entry -> entry.id == essentialId },
            canAddEssential = workspaceEssentials.size < EssentialsRules.MAX_ENTRIES,
            isBookmarked = controller.isFavorite(it.url),
            canToggleSiteMute = controller.canToggleDomainMute(it.id),
            isSiteMuted = controller.isDomainMuted(it.id),
            canDelete = TabDeletionRules.canDelete(it),
            canCloseAll = controller.activeTabs.any(TabDeletionRules::canDelete),
            otherWorkspaceCount = workspaces.count { workspace -> workspace.id != it.profileId },
        )
    }
    // Closes the sheet, then runs [action] on its tab.
    fun dismissThen(action: (BrowserTab) -> Unit) {
        val target = tab ?: return
        onDismiss()
        action(target)
    }
    val currentWorkspace = workspaces.firstOrNull { it.id == tab?.profileId }
    TabActionsSheet(
        tab = tab,
        facts = facts,
        favicon = favicon,
        currentWorkspaceName = currentWorkspace
            ?.let { it.syncedDisplayName ?: it.workspaceDisplayName() }
            .orEmpty(),
        otherWorkspaces = workspaces.filter { it.id != tab?.profileId },
        canGroup = canCreateStack || otherStacks.isNotEmpty(),
        preview = { target ->
            TabPreviewContent(
                tab = target,
                preview = controller.previews[target.id],
                favicon = controller.favicons[target.id],
                essentials = controller.essentials,
            )
        },
        groupContent = {
            TabStackMenuSection(
                currentStack = stack,
                availableStacks = otherStacks,
                canCreate = canCreateStack,
                onCreate = { dismissThen { target -> onEditStack(target.id) } },
                onAddToStack = { stackId ->
                    dismissThen { target ->
                        if (controller.tabStackController.addTab(target.id, stackId)) {
                            rootView.performConfirmHaptic()
                        }
                    }
                },
                onRemoveFromStack = {
                    dismissThen { target ->
                        if (controller.tabStackController.removeTab(target.id)) {
                            rootView.performConfirmHaptic()
                        }
                    }
                },
            )
        },
        extensionContent = {
            // Extensions act on the selected tab, so only its sheet lists them.
            FirefoxExtensionMenuSection(
                actions = if (tab?.id == controller.selectedTabId) {
                    controller.firefoxExtensionActions
                } else {
                    emptyList()
                },
                onAction = { actionKey ->
                    onDismiss()
                    controller.clickFirefoxExtensionAction(actionKey)
                },
            )
        },
        onQuickAction = { action ->
            when (action) {
                TabQuickAction.AddToEssentials -> dismissThen { target ->
                    controller.essentials.add(target.profileId, target.url, target.title, favicon)
                    rootView.performConfirmHaptic()
                }
                TabQuickAction.RemoveFromEssentials -> dismissThen { target ->
                    essentialId?.let { id -> controller.essentials.remove(target.profileId, id) }
                }
                TabQuickAction.Duplicate -> dismissThen { target ->
                    controller.createBackgroundTab(
                        initialUrl = target.url,
                        openerTabId = target.id,
                        isIncognito = target.isIncognito,
                    )?.let { rootView.performConfirmHaptic() }
                }
                TabQuickAction.Pin, TabQuickAction.Unpin -> dismissThen(onTogglePinned)
                TabQuickAction.Share -> dismissThen { target -> controller.sharePage(target.id) }
            }
        },
        onMoreAction = { action ->
            when (action) {
                TabMoreAction.AddBookmark, TabMoreAction.RemoveBookmark ->
                    dismissThen { target -> onToggleBookmark(target.id) }
                TabMoreAction.MuteSite, TabMoreAction.UnmuteSite -> tab?.let { target ->
                    val muted = action == TabMoreAction.MuteSite
                    if (controller.setDomainMuted(target.id, muted)) rootView.performConfirmHaptic()
                }
                TabMoreAction.OpenInApp ->
                    dismissThen { target -> controller.openPageExternally(target.id) }
                TabMoreAction.Print -> dismissThen { target -> controller.printPage(target.id) }
                TabMoreAction.Summarize ->
                    dismissThen { target -> controller.summarizePageWithAssistant(target.id) }
                TabMoreAction.CandyTrail -> dismissThen(onOpenCandyTrail)
                TabMoreAction.SiteCapsule -> dismissThen { target -> onAddSiteCapsule(target.id) }
                TabMoreAction.CloseAll -> dismissThen {
                    if (controller.closeAllTabs() > 0) rootView.performConfirmHaptic()
                }
            }
        },
        onMoveTo = { profileId -> dismissThen { target -> onMoveToWorkspace(target, profileId) } },
        onSnooze = { dismissThen { target -> onSnooze(target.id) } },
        onClose = { dismissThen(onClose) },
        onDismiss = onDismiss,
    )
}
