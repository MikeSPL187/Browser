package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.PageTranslationRules
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguageSetting
import dev.sk2andy.materialbrowser.data.TabDeletionRules
import dev.sk2andy.materialbrowser.reader.ListenSession
import dev.sk2andy.materialbrowser.reader.ReaderStudioSessionRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5TranslateOfferSetting
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5Preference
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.v5.V5DownloadRingState
import dev.sk2andy.materialbrowser.ui.v5.V5PageMenu
import dev.sk2andy.materialbrowser.ui.v5.V5PageMenuActions
import dev.sk2andy.materialbrowser.ui.v5.V5PageMenuExtra
import dev.sk2andy.materialbrowser.ui.v5.V5PageMenuState
import dev.sk2andy.materialbrowser.ui.v5.v5BarDomain

/**
 * The v5 page menu (V15b) bound to the selected tab: the same actions as the current menu, through
 * the same callbacks, and the commands extensions and user scripts add to the page (V28, B4).
 */
@Composable
internal fun BrowserPageMenuV5(
    controller: BrowserController,
    tab: BrowserTab,
    visible: Boolean,
    actions: V5MenuActions,
    onDismiss: () -> Unit,
    downloads: V5DownloadRingState? = null,
) {
    val blank = tab.url == BLANK_URL
    val view = LocalView.current
    val context = LocalContext.current
    val listenSession = remember(context) { ListenSession.get(context) }
    val accentArgb = V5Theme.colors.accent.toArgb()
    // Only GeckoView can choose and hide page blocks; WebView keeps the menu without the item.
    val canHideDistractions = !blank && controller.browserEngineCapabilities.hideDistractions
    // On GeckoView the page translates on the phone, through the sheet (V23); elsewhere through the provider.
    val onDeviceTranslate = BrowserTranslateRules.usesOnDevice(controller.browserEngineCapabilities.onDeviceTranslation, tab.url)
    var translateSheet by remember { mutableStateOf(false) }
    val translateOffer by rememberV5Preference(V5TranslateOfferSetting)
    // «Never translate» (Settings → Translation) takes the item away for pages in those languages.
    var languageSettings by remember { mutableStateOf<Map<String, TranslationLanguageSetting>?>(null) }
    // Read before the menu first opens too, so «Translate» does not show and then vanish (М-2).
    LaunchedEffect(visible, onDeviceTranslate) {
        if (onDeviceTranslate && (visible || languageSettings == null)) {
            controller.onDeviceTranslation.models.languageSettings { languageSettings = it }
        }
    }
    val pageLanguage = controller.onDeviceTranslation.stateOf(tab.id).page.pageLanguage
    val faviconBitmap = controller.favicons[tab.id]
    val favicon = remember(faviconBitmap) { faviconBitmap?.asImageBitmap() }
    val state = V5PageMenuState(
        title = if (blank) stringResource(R.string.new_tab_title) else displayTabTitle(tab),
        subtitle = if (blank) null else v5BarDomain(tab.url) ?: AddressResolver.displayText(tab.url),
        favicon = favicon,
        hasPage = !blank,
        canGoForward = tab.canGoForward,
        canFavorite = !blank && !tab.isIncognito,
        favoriteOffInPrivate = !blank && tab.isIncognito,
        isFavorite = controller.isSelectedTabFavorite,
        canRead = !blank && ReaderStudioSessionRules.isSupportedSource(tab.url),
        // Settings → Translation can take the item out of the menu.
        canTranslate = if (onDeviceTranslate) {
            translateOffer && BrowserTranslateRules.offers(pageLanguage, languageSettings)
        } else {
            !blank && PageTranslationRules.canTranslate(provider = controller.pageTranslationProvider, sourceUrl = tab.url)
        },
        isPinned = tab.isPinned,
        desktopView = controller.isSelectedDesktopView.takeIf { controller.canToggleSelectedDesktopView },
        siteMuted = controller.isSelectedDomainMuted.takeIf { controller.canToggleSelectedDomainMute },
        canCloseTab = TabDeletionRules.canDelete(tab),
        canNewPrivateTab = controller.canOpenLinkInPrivate,
        canHideDistractions = canHideDistractions,
        hiddenCount = if (canHideDistractions) controller.hiddenDistractions.hiddenCount(tab) else 0,
        downloads = downloads,
        extras = pageMenuExtras(controller, offHere = stringResource(R.string.v5_menu_extension_off_here)),
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
            onTranslate = { if (onDeviceTranslate) translateSheet = true else controller.translateSelectedPage() },
            onFindInPage = actions.onFindInPage,
            onTogglePinned = actions.onTogglePinned,
            onDesktopViewChange = controller::setSelectedDesktopView,
            onSiteMutedChange = controller::setSelectedDomainMuted,
            onNewTab = actions.onNewTab,
            onNewPrivateTab = {
                // Checked before the tab is made: a refusal after it left an ordinary tab, unsaid.
                if (controller.canOpenLinkInPrivate) actions.onNewPrivateTab()
            },
            onCloseTab = actions.onCloseTab,
            onFavorites = actions.onFavorites,
            onHistory = actions.onHistory,
            onDownloads = actions.onDownloads,
            onSettings = actions.onSettings,
            onDuplicateTab = actions.onDuplicateTab,
            onOpenInApp = controller::openSelectedPageExternally,
            // The page outlines in the profile's accent; a page not ready to choose refuses.
            onHideDistractions = {
                if (!controller.hiddenDistractions.startPicking(tab, accentArgb)) view.performV5Haptic(V5Haptic.Reject)
            },
            onShowHidden = { controller.hiddenDistractions.showAll(tab) },
            onListen = { listenToSelectedPage(controller, listenSession, title = state.title) },
        ),
        onDismiss = onDismiss,
    )
    BrowserTranslateSheetV5(
        controller = controller,
        tab = tab,
        visible = translateSheet,
        onDismiss = { translateSheet = false },
    )
}

/**
 * The page's extension actions, then its user-script commands, as rows of the v5 menu. User scripts
 * count only where the engine runs them; an extension action the page disables stays, faded.
 */
private fun pageMenuExtras(controller: BrowserController, offHere: String): List<V5PageMenuExtra> {
    val extensions = controller.firefoxExtensionActions.toList().map { action ->
        V5PageMenuExtra(
            key = "ext_${action.key.hashCode()}",
            title = action.title,
            // A faded action says why: the extension turned it off for this page (М-5).
            subtitle = if (action.enabled) action.badgeText else offHere,
            enabled = action.enabled,
            onClick = { controller.clickFirefoxExtensionAction(action.key) },
        )
    }
    val scripts = if (controller.isUserScriptSupported) controller.selectedUserScriptMenuCommands else emptyList()
    return extensions + scripts.map { command ->
        V5PageMenuExtra(
            key = "script_${command.scriptId}_${command.commandId}",
            title = command.caption,
            subtitle = command.scriptName,
            enabled = true,
            onClick = { controller.invokeUserScriptMenuCommand(command) },
        )
    }
}
