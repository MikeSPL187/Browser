package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
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
    // «Close all tabs» asks first: it closes every unpinned tab of the workspace at once.
    var closeAllCount by rememberSaveable { mutableIntStateOf(0) }
    if (closeAllCount > 0) {
        val dismissCloseAll = { closeAllCount = 0 }
        AlertDialog(
            onDismissRequest = dismissCloseAll,
            title = { Text(pluralStringResource(R.plurals.close_all_tabs_title, closeAllCount, closeAllCount)) },
            text = { Text(stringResource(R.string.close_all_tabs_pinned_supporting_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        closeAllCount = 0
                        if (controller.closeAllTabs() > 0) rootView.performConfirmHaptic()
                    },
                ) { Text(stringResource(R.string.action_close_all_tabs)) }
            },
            dismissButton = {
                TextButton(onClick = dismissCloseAll) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
    val tab = tabId?.let { id -> controller.activeTabs.firstOrNull { it.id == id } }
    // The tab went away under the sheet (undo, a closed workspace): close the sheet, or the overview
    // would stay blurred and deaf to touches.
    LaunchedEffect(tabId, tab == null) {
        if (tabId != null && tab == null) onDismiss()
    }
    val workspaces = controller.profiles.take(if (controller.profilesEnabled) Int.MAX_VALUE else 1)
    // Only workspaces the tab can really move to: not locked ones, nor a synced one for a private tab.
    val moveTargets = tab?.let { controller.compatibleMoveTargetProfiles(it.id) }.orEmpty()
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
    val locked = tab != null && hidesPrivateTab(tab)
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
            otherWorkspaceCount = moveTargets.size,
            canOpenSideBySide = it.url != BLANK_URL &&
                it.id != controller.selectedTabId &&
                it.isIncognito == controller.selectedTab.isIncognito,
            isLocked = locked,
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
        otherWorkspaces = moveTargets,
        canGroup = TabStacksFeature.ENABLED && !locked && (canCreateStack || otherStacks.isNotEmpty()),
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
                actions = if (!locked && tab?.id == controller.selectedTabId) {
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
                TabMoreAction.SideBySide ->
                    dismissThen { target -> controller.openSplitView(target.id) }
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
                    closeAllCount = TabDeletionRules.deletableTabIds(controller.activeTabs).size
                }
            }
        },
        onMoveTo = { profileId -> dismissThen { target -> onMoveToWorkspace(target, profileId) } },
        onSnooze = { dismissThen { target -> onSnooze(target.id) } },
        onClose = { dismissThen(onClose) },
        onDismiss = onDismiss,
    )
}
