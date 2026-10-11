package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.PageTranslationRules
import dev.sk2andy.materialbrowser.browser.SiteConnectionRules
import dev.sk2andy.materialbrowser.browser.permissions.PermissionOrigin
import dev.sk2andy.materialbrowser.data.TabDeletionRules
import dev.sk2andy.materialbrowser.reader.ReaderStudioSessionRules
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTints
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5BarGestures
import dev.sk2andy.materialbrowser.ui.v5.V5BarState
import dev.sk2andy.materialbrowser.ui.v5.V5BottomBar
import dev.sk2andy.materialbrowser.ui.v5.v5BarDomain

/*
 * Binds the v5 bottom bar (ui/v5/V5BottomBar.kt) to the browser while the ui.v5 flag is on. The menu
 * is still the current browser menu: its v5 version is V15.
 */

/** Menu actions that need more than a controller call. */
internal class V5MenuActions(
    val onNewTab: () -> Unit,
    val onDuplicateTab: () -> Unit,
    val onCloseTab: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onTogglePinned: () -> Unit,
    val onFindInPage: () -> Unit,
    val onReaderStudio: () -> Unit,
    val onOpenCandyTrail: () -> Unit,
    val onSnooze: () -> Unit,
    val onSnoozedTabs: () -> Unit,
    val onAddSiteCapsule: () -> Unit,
    val onFavorites: () -> Unit,
    val onDownloads: () -> Unit,
    val onHistory: () -> Unit,
    val onSettings: () -> Unit,
    val onPrivacyXRay: () -> Unit,
    val onSplitViewChange: (Boolean) -> Unit,
)

@Composable
internal fun BrowserV5Bar(
    controller: BrowserController,
    selectedTab: BrowserTab,
    backdropSource: CandyChromeBackdropSource?,
    collapsed: Boolean,
    onExpand: () -> Unit,
    gestures: V5BarGestures,
    permissionActivityVisible: Boolean,
    canDock: Boolean,
    snoozedTabCount: Int,
    onAddress: () -> Unit,
    onTabs: () -> Unit,
    onBarPositioned: (Rect, Int) -> Unit,
    actions: V5MenuActions,
    modifier: Modifier = Modifier,
) {
    val tab = selectedTab
    val darkShell = MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE
    val state = V5BarState(
        tabId = tab.id,
        domain = if (tab.url == BLANK_URL) null else v5BarDomain(tab.url) ?: AddressResolver.displayText(tab.url),
        isLoading = tab.isLoading,
        progress = tab.progress / PROGRESS_MAX,
        tabCount = controller.activeTabs.size,
        siteAvailable = PermissionOrigin.normalize(tab.url) != null,
        permissionActivity = permissionActivityVisible,
        connectionKind = SiteConnectionRules.kind(
            pageUrl = tab.url,
            isLoading = tab.isLoading,
            hasError = tab.error != null || tab.failureKind != null,
        ),
        blockedCount = tab.blockedCount,
    )
    var menuExpanded by remember { mutableStateOf(false) }
    // The page's theme-color (system-v5.md, section 1.3). Private tabs, a new tab and error pages
    // keep the neutral bar (step 7).
    val siteColor = controller.selectedSiteThemeColor
        ?.takeUnless { tab.isIncognito || tab.url == BLANK_URL || tab.error != null || tab.failureKind != null }
    VolaV5Theme(dark = darkShell, privateBrowsing = tab.isIncognito) {
        val chrome = V5Theme.colors.chrome
        val label = V5Theme.colors.label
        val tint = remember(siteColor, chrome, label) { V5SiteTints.of(siteColor, chrome, label) }
        Box(modifier) {
            V5BottomBar(
                state = state,
                gestures = gestures,
                tint = tint,
                collapsed = collapsed,
                onExpand = onExpand,
                onAddress = onAddress,
                onShield = actions.onPrivacyXRay,
                onReloadOrStop = { if (tab.isLoading) controller.stopLoading() else controller.reload() },
                onTabs = onTabs,
                onBarPositioned = onBarPositioned,
                menuButton = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(VolaIcons.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                    }
                    BrowserMainMenu(
                        expanded = menuExpanded,
                        backdropSource = backdropSource,
                        onDismissRequest = { menuExpanded = false },
                        pageSubtitle = if (tab.url == BLANK_URL) {
                            stringResource(R.string.new_tab_title)
                        } else {
                            AddressResolver.displayText(tab.url)
                        },
                        canGoBack = tab.canGoBack,
                        canGoForward = tab.canGoForward,
                        isLoading = tab.isLoading,
                        canToggleFavorite = tab.url != BLANK_URL && !tab.isIncognito,
                        isFavorite = controller.isSelectedTabFavorite,
                        isPinned = tab.isPinned,
                        canUsePageActions = tab.url != BLANK_URL,
                        canOpenReader = ReaderStudioSessionRules.isSupportedSource(tab.url),
                        canTranslatePage = PageTranslationRules.canTranslate(
                            provider = controller.pageTranslationProvider,
                            sourceUrl = tab.url,
                        ),
                        canToggleDomainMute = controller.canToggleSelectedDomainMute,
                        isDomainMuted = controller.isSelectedDomainMuted,
                        canToggleDesktopView = controller.canToggleSelectedDesktopView,
                        isDesktopView = controller.isSelectedDesktopView,
                        canAddSiteCapsule = tab.url != BLANK_URL &&
                            !tab.isIncognito &&
                            (tab.url.startsWith("https://") || tab.url.startsWith("http://")),
                        canSnooze = controller.canSnoozeSelectedTab,
                        snoozedTabCount = snoozedTabCount,
                        canCloseTab = TabDeletionRules.canDelete(tab),
                        userScriptMenuCommands = if (controller.isUserScriptSupported) {
                            controller.selectedUserScriptMenuCommands
                        } else {
                            emptyList()
                        },
                        onTabs = onTabs,
                        onNewTab = actions.onNewTab,
                        onDuplicateTab = actions.onDuplicateTab,
                        onCloseTab = actions.onCloseTab,
                        onBack = controller::goBack,
                        onForward = controller::goForward,
                        onReloadOrStop = { if (tab.isLoading) controller.stopLoading() else controller.reload() },
                        onToggleFavorite = actions.onToggleFavorite,
                        onTogglePinned = actions.onTogglePinned,
                        onShare = controller::shareSelectedPage,
                        onOpenExternal = controller::openSelectedPageExternally,
                        onPrint = controller::printSelectedPage,
                        onOpenReader = actions.onReaderStudio,
                        onTranslate = controller::translateSelectedPage,
                        splitView = controller.splitView.state != null,
                        onSplitViewChange = actions.onSplitViewChange,
                        onFindInPage = actions.onFindInPage,
                        onDomainMutedChange = controller::setSelectedDomainMuted,
                        onDesktopViewChange = controller::setSelectedDesktopView,
                        compactMode = controller.addressBar.compactMode,
                        onCompactModeChange = controller.addressBar::updateCompactMode,
                        onOpenCandyTrail = actions.onOpenCandyTrail,
                        onAddSiteCapsule = actions.onAddSiteCapsule,
                        onSummarize = controller::summarizeSelectedPageWithAssistant,
                        onSnooze = actions.onSnooze,
                        onSnoozedTabs = actions.onSnoozedTabs,
                        onUserScriptMenuCommand = controller::invokeUserScriptMenuCommand,
                        canDockAddressBar = canDock,
                        onDockAddressBar = { controller.addressBar.updateDocked(true) },
                        onParkAddressBarRight = controller.addressBar::parkOnRight,
                        onFavorites = actions.onFavorites,
                        onDownloads = actions.onDownloads,
                        onHistory = actions.onHistory,
                        firefoxExtensionActions = controller.firefoxExtensionActions,
                        menuLayout = controller.browserMenuLayout,
                        onFirefoxExtensionAction = controller::clickFirefoxExtensionAction,
                        onSettings = actions.onSettings,
                    )
                },
            )
        }
    }
}

/** Below this background luminance the current theme is dark. */
private const val DARK_LUMINANCE = 0.5f

/** Tab progress is reported in percent. */
private const val PROGRESS_MAX = 100f
