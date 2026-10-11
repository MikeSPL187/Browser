package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.PageTranslationRules
import dev.sk2andy.materialbrowser.data.TabDeletionRules
import dev.sk2andy.materialbrowser.reader.ReaderStudioSessionRules
import dev.sk2andy.materialbrowser.ui.v5.V5PageMenu
import dev.sk2andy.materialbrowser.ui.v5.V5PageMenuActions
import dev.sk2andy.materialbrowser.ui.v5.V5PageMenuState
import dev.sk2andy.materialbrowser.ui.v5.v5BarDomain

/**
 * The v5 page menu (V15b) bound to the selected tab: the same actions as the current menu, through
 * the same callbacks. [onAllActions] opens the current menu for everything the v5 one leaves out.
 */
@Composable
internal fun BrowserPageMenuV5(
    controller: BrowserController,
    tab: BrowserTab,
    visible: Boolean,
    actions: V5MenuActions,
    onDismiss: () -> Unit,
    onAllActions: () -> Unit,
) {
    val blank = tab.url == BLANK_URL
    val faviconBitmap = controller.favicons[tab.id]
    val favicon = remember(faviconBitmap) { faviconBitmap?.asImageBitmap() }
    val state = V5PageMenuState(
        title = if (blank) stringResource(R.string.new_tab_title) else displayTabTitle(tab),
        subtitle = if (blank) null else v5BarDomain(tab.url) ?: AddressResolver.displayText(tab.url),
        favicon = favicon,
        hasPage = !blank,
        canGoForward = tab.canGoForward,
        canFavorite = !blank && !tab.isIncognito,
        isFavorite = controller.isSelectedTabFavorite,
        canRead = !blank && ReaderStudioSessionRules.isSupportedSource(tab.url),
        canTranslate = !blank && PageTranslationRules.canTranslate(
            provider = controller.pageTranslationProvider,
            sourceUrl = tab.url,
        ),
        isPinned = tab.isPinned,
        desktopView = controller.isSelectedDesktopView.takeIf { controller.canToggleSelectedDesktopView },
        siteMuted = controller.isSelectedDomainMuted.takeIf { controller.canToggleSelectedDomainMute },
        canCloseTab = TabDeletionRules.canDelete(tab),
    )
    V5PageMenu(
        visible = visible,
        state = state,
        actions = V5PageMenuActions(
            onForward = controller::goForward,
            onToggleFavorite = actions.onToggleFavorite,
            onShare = controller::shareSelectedPage,
            onPrint = controller::printSelectedPage,
            onRead = actions.onReaderStudio,
            onTranslate = controller::translateSelectedPage,
            onFindInPage = actions.onFindInPage,
            onTogglePinned = actions.onTogglePinned,
            onDesktopViewChange = controller::setSelectedDesktopView,
            onSiteMutedChange = controller::setSelectedDomainMuted,
            onNewTab = actions.onNewTab,
            onNewPrivateTab = {
                actions.onNewTab()
                controller.setBlankTabIncognito(true)
            },
            onCloseTab = actions.onCloseTab,
            onFavorites = actions.onFavorites,
            onHistory = actions.onHistory,
            onDownloads = actions.onDownloads,
            onSettings = actions.onSettings,
            onAllActions = onAllActions,
        ),
        onDismiss = onDismiss,
    )
}
