package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.TextFieldValue
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.ClipboardOffer
import dev.sk2andy.materialbrowser.browser.DistractionAction
import dev.sk2andy.materialbrowser.browser.DistractionPickingStep
import dev.sk2andy.materialbrowser.browser.SiteConnectionRules
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.browser.permissions.PermissionOrigin
import dev.sk2andy.materialbrowser.reader.ReaderStudioSessionRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTintSetting
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTints
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5Preference
import dev.sk2andy.materialbrowser.ui.v5.V5AddressCompletionRules
import dev.sk2andy.materialbrowser.ui.v5.V5AddressEditor
import dev.sk2andy.materialbrowser.ui.v5.V5BarGestures
import dev.sk2andy.materialbrowser.ui.v5.V5BarState
import dev.sk2andy.materialbrowser.ui.v5.V5BottomBar
import dev.sk2andy.materialbrowser.ui.v5.V5DistractionChoice
import dev.sk2andy.materialbrowser.ui.v5.V5DistractionsPanel
import dev.sk2andy.materialbrowser.ui.v5.V5CommandNotice
import dev.sk2andy.materialbrowser.ui.v5.V5CommandNoticeAccessory
import dev.sk2andy.materialbrowser.ui.v5.V5EditorState
import dev.sk2andy.materialbrowser.ui.v5.V5SuggestionLayoutRules
import dev.sk2andy.materialbrowser.ui.v5.v5BarDomain

/*
 * Binds the v5 bottom bar (ui/v5/V5BottomBar.kt) to the browser while the ui.v5 flag is on. «•••»
 * opens the v5 page menu (V15b), the only menu under v5 (V28, B4).
 */

/** Menu actions that need more than a controller call. */
internal class V5MenuActions(
    val onNewTab: () -> Unit,
    /** A new tab made private from the start: never an ordinary one turned private after (М-6). */
    val onNewPrivateTab: () -> Unit,
    val onDuplicateTab: () -> Unit,
    val onCloseTab: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onTogglePinned: () -> Unit,
    val onFindInPage: () -> Unit,
    val onReaderStudio: () -> Unit,
    val onFavorites: () -> Unit,
    val onDownloads: () -> Unit,
    val onHistory: () -> Unit,
    val onSettings: () -> Unit,
    val onPrivacyXRay: () -> Unit,
)

@Composable
internal fun BrowserV5Bar(
    controller: BrowserController,
    selectedTab: BrowserTab,
    collapsed: Boolean,
    onExpand: () -> Unit,
    gestures: V5BarGestures,
    permissionActivityVisible: Boolean,
    onAddress: () -> Unit,
    onTabs: () -> Unit,
    onBarPositioned: (Rect, Int) -> Unit,
    actions: V5MenuActions,
    modifier: Modifier = Modifier,
    /** What an address bar command just answered, shown above the bar (V28, B6b). */
    commandFeedback: AddressCommandFeedback? = null,
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
        readerAvailable = controller.selectedReaderAvailable && ReaderStudioSessionRules.isSupportedSource(tab.url),
        hidesItems = controller.hiddenDistractions.hiddenCount(tab) > 0,
        isPrivate = tab.isIncognito,
    )
    var menuExpanded by remember { mutableStateOf(false) }
    // V22: what is downloading, as a ring around «•••» and a row at the top of the menu.
    val downloadRing = rememberDownloadRing(controller)
    // The page's color (system-v5.md, section 1.3), unless turned off in Appearance. Private tabs,
    // a new tab and error pages keep the neutral bar (step 7).
    val siteTint by rememberV5Preference(V5SiteTintSetting)
    val siteColor = controller.selectedSiteThemeColor
        ?.takeIf { siteTint }
        ?.takeUnless { tab.isIncognito || tab.url == BLANK_URL || tab.error != null || tab.failureKind != null }
    VolaV5Theme(dark = darkShell, accent = controller.v5Accent, privateBrowsing = tab.isIncognito) {
        val chrome = V5Theme.colors.chrome
        val label = V5Theme.colors.label
        val accentArgb = V5Theme.colors.accent.toArgb()
        val view = LocalView.current
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
                onRead = actions.onReaderStudio,
                // The page outlines in the profile's accent, as from the menu.
                onHidden = {
                    if (!controller.hiddenDistractions.startPicking(tab, accentArgb)) view.performV5Haptic(V5Haptic.Reject)
                },
                onTabs = onTabs,
                onBarPositioned = onBarPositioned,
                accessory = {
                    Column {
                        V5CommandNoticeAccessory(
                            commandFeedback?.let { feedback ->
                                V5CommandNotice(
                                    text = feedback.localizedText(),
                                    rejected = feedback.tone == AddressCommandFeedbackTone.Reject,
                                )
                            },
                        )
                        BrowserTranslatedAccessory(controller = controller, tab = tab)
                        BrowserListenAccessory(selectedTabPrivate = tab.isIncognito)
                        BrowserDownloadNoticeAccessory(
                            downloads = downloadRing,
                            selectedTabPrivate = tab.isIncognito,
                            onDownloads = actions.onDownloads,
                        )
                    }
                },
                menuButton = {
                    // On the site's color the accent may not show; the bar's own ink does.
                    DownloadsMenuButton(
                        downloads = downloadRing,
                        ink = if (siteColor != null) tint.onTint else V5Theme.colors.accent,
                        onClick = { menuExpanded = true },
                    )
                },
            )
            // Over the whole browser, above the bar: the bar's box fills the window.
            BrowserPageMenuV5(
                controller = controller,
                tab = tab,
                visible = menuExpanded,
                actions = actions,
                onDismiss = { menuExpanded = false },
                downloads = downloadRing.progress,
            )
            // «Hide distracting items» (V20): while the page chooses, the hint sits at the top edge.
            val distractions = controller.hiddenDistractions
            val picking = distractions.pickingTabId == tab.id
            BackHandler(enabled = picking) { distractions.stopPicking() }
            V5DistractionsPanel(
                visible = picking,
                site = v5BarDomain(tab.url) ?: tab.url,
                privateTab = tab.isIncognito,
                hasHidden = picking && distractions.hiddenCount(tab) > 0,
                canUndo = distractions.canUndo,
                step = distractions.pickingStep,
                onUndo = distractions::undoLastHide,
                onShowHidden = { distractions.showAll(tab) },
                onDone = distractions::stopPicking,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            // A tap outlined a block: the card above the bar hides it, takes a larger one or lets go.
            V5DistractionChoice(
                visible = picking && distractions.pickingStep == DistractionPickingStep.Selected,
                canPickLarger = distractions.canPickLarger,
                onHide = { distractions.act(DistractionAction.Hide) },
                onLarger = { distractions.act(DistractionAction.Larger) },
                onCancel = { distractions.act(DistractionAction.Cancel) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            // Vola's offer to translate the page, above the bar (canvas «Translate»).
            BrowserTranslateOfferV5(controller = controller, tab = tab, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/**
 * The v5 address editor (V9a) bound to the current editor state: the suggestions are the current
 * ones, laid out the v5 way, and submitting goes the same way as from the current editor.
 */
@Composable
internal fun BrowserV5AddressEditor(
    selectedTab: BrowserTab,
    /** The active profile's accent: «Go» and the cursor take it. */
    accent: V5Accent,
    suggestionItems: List<AddressSuggestionItem>,
    highlightedIndex: Int,
    value: TextFieldValue,
    focusNonce: Int,
    clipboard: ClipboardOffer?,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
    onSelect: (AddressSuggestionItem) -> Unit,
    onFill: (AddressSuggestionItem) -> Unit,
    onClipboard: () -> Unit,
    onCancel: () -> Unit,
    onHighlightChange: (Int) -> Unit,
    /** A new tab with pinned sites: they show through the empty editor. */
    overStart: Boolean = false,
    /** A command's answer while the editor stays open: «No such command», «New tab». */
    commandFeedback: AddressCommandFeedback? = null,
    /** «Find on page» in the list: search the open page for the typed text. */
    onFindOnPage: (String) -> Unit = {},
    /** Where a search goes, named beside the first search row. */
    searchEngine: String? = null,
) {
    val darkShell = MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE
    val highlighted = suggestionItems.getOrNull(highlightedIndex)
    // Only a page can be searched: not a new tab, whose start shows through the editor.
    val pageUrl = selectedTab.url.takeUnless { it == BLANK_URL || overStart }
    val layout = remember(suggestionItems, value.text, pageUrl) {
        V5SuggestionLayoutRules.layout(suggestionItems, value.text, pageUrl)
    }
    // Delete takes the completion away until the next letter; a letter typed over the selected
    // address is typing, not deleting.
    var deleting by remember { mutableStateOf(false) }
    // A row picked with the arrow keys is what Go opens, so no completion shows beside it.
    val completion = V5AddressCompletionRules.suffix(value, layout.best, deleting).takeIf { highlighted == null }
    VolaV5Theme(dark = darkShell, accent = accent, privateBrowsing = selectedTab.isIncognito) {
        V5AddressEditor(
            state = V5EditorState(
                value = value,
                layout = layout,
                highlightedId = highlighted?.stableId,
                clipboard = clipboard,
                isPrivate = selectedTab.isIncognito,
                focusNonce = focusNonce,
                overStart = overStart,
                completion = completion,
                searchEngine = searchEngine,
            ),
            onValueChange = { next ->
                deleting = next.text.length < value.text.length && value.text.startsWith(next.text)
                onValueChange(next)
            },
            // Enter opens what the arrow keys picked, else the typed text, like the current editor.
            onSubmit = { text -> if (highlighted != null) onSelect(highlighted) else onSubmit(text) },
            onSelect = onSelect,
            onFill = onFill,
            onClipboard = onClipboard,
            onCancel = onCancel,
            onHighlight = { item -> onHighlightChange(suggestionItems.indexOf(item)) },
            onFindOnPage = onFindOnPage,
            notice = commandFeedback?.let { feedback ->
                V5CommandNotice(
                    text = feedback.localizedText(),
                    rejected = feedback.tone == AddressCommandFeedbackTone.Reject,
                )
            },
        )
    }
}

/** Below this background luminance the current theme is dark. */
private const val DARK_LUMINANCE = 0.5f

/** Tab progress is reported in percent. */
private const val PROGRESS_MAX = 100f
