@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)

package dev.sk2andy.materialbrowser.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.ClipboardAction
import dev.sk2andy.materialbrowser.browser.ClipboardOfferRules
import dev.sk2andy.materialbrowser.browser.cast.CastUiState
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import kotlin.math.absoluteValue
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.VolaIsland
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.v5.V5BarGestures

@Composable
internal fun BoxScope.BrowserAddressChrome(
    controller: BrowserController,
    selectedTab: BrowserTab,
    addressEditorVisible: Boolean,
    showInteractiveBlankStart: Boolean,
    commandFeedback: AddressCommandFeedback?,
    suggestionItems: List<AddressSuggestionItem>,
    highlightedSuggestionIndex: Int,
    bottomBarTopPx: MutableFloatState,
    linkPeekAddressBarExpanded: Boolean,
    castUiState: CastUiState,
    settingsVisible: Boolean,
    addressValue: TextFieldValue,
    addressFocusNonce: Int,
    browserDragOffset: MutableFloatState,
    browserWidthPx: Float,
    tabSwitchTravelPx: Float,
    tabHandoffAlpha: Animatable<Float, AnimationVector1D>,
    tabOverviewOpening: Boolean,
    tabOverviewVisible: Boolean,
    overviewGestureProgress: MutableFloatState,
    onToggleCastPlayback: () -> Unit,
    onSeekCast: (Long) -> Unit,
    onCastVolumeChange: (Float) -> Unit,
    onDisconnectCast: () -> Unit,
    openAddressEditor: () -> Unit,
    onAddressEditorDismiss: () -> Unit,
    onAddressValueChanged: (TextFieldValue) -> Unit,
    onHighlightedSuggestionChanged: (Int) -> Unit,
    selectSuggestion: (AddressSuggestionItem) -> Unit,
    fillAddressFromSuggestion: (AddressSuggestionItem) -> Unit,
    submitAddressOrCommand: (String) -> Unit,
    onLiveFrameCleared: () -> Unit,
    onTabHandoffChanged: (TabHandoff?) -> Unit,
    onTabs: () -> Unit,
    onOverviewGestureStarted: () -> Unit,
    onOverviewGestureCancelled: () -> Unit,
    openNewTabAndEdit: () -> Unit,
    openNewPrivateTabAndEdit: () -> Unit,
    toggleFavoriteWithFeedback: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit,
    onSettings: () -> Unit,
    onPrivacyXRay: () -> Unit,
    onReaderStudio: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val rootView = LocalView.current
    val longPressActionHapticNonce = controller.contentActions.longPressActionHapticNonce
    LaunchedEffect(longPressActionHapticNonce) {
        if (longPressActionHapticNonce != 0) {
            rootView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    val permissionActivityVisible = controller.hasPermissionActivity(selectedTab.id)
    val addressEditor = controller.addressBar.editor
    val clipboardOffer = rememberAddressClipboardOffer(
        enabled = addressEditorVisible && commandFeedback == null,
        usedCopiedAtElapsedMillis = addressEditor.usedClipCopiedAtElapsedMillis,
    )
    val onClipboardOffer: () -> Unit = clipboardOffer@{
        val offer = clipboardOffer ?: return@clipboardOffer
        // The only clipboard read: the system shows its "pasted" notice now.
        val text = readClipboardText(context) ?: return@clipboardOffer
        addressEditor.markClipUsed(offer.copiedAtElapsedMillis)
        rootView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        when (val action = ClipboardOfferRules.action(offer.offer, text)) {
            is ClipboardAction.Open -> {
                controller.submitAddress(action.address)
                onAddressEditorDismiss()
            }
            is ClipboardAction.Paste -> addressEditor.fill(action.text)
            null -> Unit
        }
    }
    // A form on the page has the keyboard: the bar slides under it instead of riding on top of
    // the keyboard over the form (#123, H3), and comes back when the keyboard closes.
    val hiddenByPageKeyboard = AddressBarInsetRules.hiddenByPageKeyboard(
        imeVisible = WindowInsets.isImeVisible,
        browserChromeOwnsIme = addressEditorVisible || controller.findInPageState != null,
    )
    val pageKeyboardHide = remember { Animatable(0f) }
    LaunchedEffect(hiddenByPageKeyboard) {
        pageKeyboardHide.animateTo(if (hiddenByPageKeyboard) 1f else 0f, VolaMotion.standard())
    }
    // v5 tab swipe (system-v5.md, section 7): a tick when the swipe crosses the switch threshold and
    // when it falls back under it, 30 % of the width or 1000 dp/s, and the tabSwipe spring.
    var v5SwipeThresholdReached by remember { mutableStateOf(false) }
    val onTabDrag: (Float) -> Unit = { delta ->
        if (!addressEditorVisible && !tabOverviewVisible) {
            if (browserDragOffset.floatValue == 0f && delta != 0f) {
                controller.refreshSelectedTabPreviewBeforeDeparture()
            }
            val proposed = browserDragOffset.floatValue + delta
            val tabs = controller.activeTabs
            val currentIndex = tabs.indexOfFirst { it.id == controller.selectedTabId }
            val hasTarget = if (proposed < 0f) {
                currentIndex in 0 until tabs.lastIndex
            } else {
                currentIndex > 0
            }
            browserDragOffset.floatValue = if (hasTarget) {
                proposed.coerceIn(-tabSwitchTravelPx, tabSwitchTravelPx)
            } else {
                (proposed * AddressBarTabSwitchRules.EDGE_RESISTANCE).coerceIn(
                    -with(density) { AddressBarTabSwitchRules.EDGE_TRAVEL_MAX_DP.dp.toPx() },
                    with(density) { AddressBarTabSwitchRules.EDGE_TRAVEL_MAX_DP.dp.toPx() },
                )
            }
            // Past the last tab the threshold is the rubber band's end, where letting go opens a new tab.
            val pastLast = !hasTarget && proposed < 0f && currentIndex == tabs.lastIndex
            val reached = if (pastLast) {
                AddressBarTabSwitchRules.opensNewTabPastLast(
                    offsetPx = browserDragOffset.floatValue,
                    edgeTravelMaxPx = with(density) { AddressBarTabSwitchRules.EDGE_TRAVEL_MAX_DP.dp.toPx() },
                    newTabFraction = V5Motion.TAB_SWIPE_NEW_TAB_EDGE_FRACTION,
                )
            } else {
                hasTarget &&
                    browserDragOffset.floatValue.absoluteValue >= browserWidthPx * V5Motion.TAB_SWIPE_COMMIT_FRACTION
            }
            if (reached != v5SwipeThresholdReached) {
                v5SwipeThresholdReached = reached
                rootView.performV5Haptic(if (reached) V5Haptic.ThresholdReached else V5Haptic.ThresholdLeft)
            }
        }
    }
    val onTabDragStopped: suspend (Float) -> Unit = { velocity ->
        val direction = browserDragOffset.floatValue.compareTo(0f)
        val tabs = controller.activeTabs
        val currentIndex = tabs.indexOfFirst { it.id == controller.selectedTabId }
        val targetTab = tabs.getOrNull(currentIndex - direction)
        val minTravel = with(density) { AddressBarTabSwitchRules.FLING_MIN_TRAVEL_DP.dp.toPx() }
        val flingVelocityDp = V5Motion.TAB_SWIPE_COMMIT_VELOCITY_DP
        val fastEnough = browserDragOffset.floatValue.absoluteValue >= minTravel &&
            velocity.absoluteValue >= with(density) { flingVelocityDp.dp.toPx() } &&
            velocity.compareTo(0f) == direction
        val farEnough =
            browserDragOffset.floatValue.absoluteValue >= browserWidthPx * V5Motion.TAB_SWIPE_COMMIT_FRACTION
        v5SwipeThresholdReached = false
        val shouldSwitch = targetTab != null && (farEnough || fastEnough)
        // The rubber band past the last tab, let go near its end, opens a new tab once it springs back.
        val opensNewTab = targetTab == null && direction < 0 &&
            currentIndex == tabs.lastIndex &&
            AddressBarTabSwitchRules.opensNewTabPastLast(
                offsetPx = browserDragOffset.floatValue,
                edgeTravelMaxPx = with(density) { AddressBarTabSwitchRules.EDGE_TRAVEL_MAX_DP.dp.toPx() },
                newTabFraction = V5Motion.TAB_SWIPE_NEW_TAB_EDGE_FRACTION,
            )
        val settle = Animatable(browserDragOffset.floatValue)
        settle.animateTo(
            targetValue = if (shouldSwitch) direction * tabSwitchTravelPx else 0f,
            initialVelocity = velocity,
            animationSpec = if (V5Motion.animationsEnabled) V5Motion.tabSwipe.spec() else V5Motion.reducedFade(),
        ) { browserDragOffset.floatValue = value }
        if (shouldSwitch) {
            onLiveFrameCleared()
            tabHandoffAlpha.snapTo(1f)
            onTabHandoffChanged(
                TabHandoff(
                    tab = targetTab,
                    preview = controller.previews[targetTab.id].takeUnless {
                        targetTab.isIncognito
                    },
                    favicon = controller.favicons[targetTab.id],
                    previewTopInsetPx = controller.previewTopInsetPx(targetTab.id),
                ),
            )
            browserDragOffset.floatValue = 0f
            controller.selectTab(targetTab.id)
            rootView.performConfirmHaptic()
        } else {
            browserDragOffset.floatValue = 0f
            if (opensNewTab) {
                controller.createTab()
                rootView.performV5Haptic(V5Haptic.Confirm)
            }
        }
    }
    val onBarPositioned: (Rect, Int) -> Unit = { boundsInRoot, topInWindowPx ->
        bottomBarTopPx.floatValue = boundsInRoot.top
        controller.setPreviewContentBottomInWindowPx(topInWindowPx)
    }
    // Compact Mode is a v4 setting with no switch under v5 (V28, step 4): a phone that kept it on
    // would start every page with the v5 bar folded and no way back, so v5 turns it off.
    LaunchedEffect(controller.addressBar.compactMode) {
        if (controller.addressBar.compactMode) controller.addressBar.updateCompactMode(false)
    }
    val compactBar = controller.isBottomBarCompact &&
        // A new tab is where an address is typed: its bar stays open even in Compact Mode.
        selectedTab.url != BLANK_URL &&
        !linkPeekAddressBarExpanded
    // V7: the v5 bar at rest and while collapsed; command feedback is a line above it (V28, B6b).
    // The editor and the overview draw their own. The v5 link preview (V21) keeps the bar under it.
    val showV5Bar = !addressEditorVisible &&
        !tabOverviewOpening &&
        !tabOverviewVisible
    // The tab a swipe on the bar steps to, the one after (+1) or before (−1): TalkBack takes the same step.
    val adjacentTab: (Int) -> BrowserTab? = { step ->
        val tabs = controller.activeTabs
        val index = tabs.indexOfFirst { it.id == controller.selectedTabId }
        if (index < 0) null else tabs.getOrNull(index + step)
    }
    if (showV5Bar) {
        BrowserV5Bar(
            controller = controller,
            selectedTab = selectedTab,
            collapsed = compactBar,
            onExpand = controller::expandBottomBar,
            gestures = V5BarGestures(
                tabSwipeEnabled = true,
                onTabDrag = onTabDrag,
                onTabDragStopped = onTabDragStopped,
                overviewEnabled = true,
                overviewProgress = overviewGestureProgress,
                onOverviewProgress = { progress ->
                    onOverviewGestureStarted()
                    overviewGestureProgress.floatValue = progress.coerceIn(0f, 1f)
                },
                onOverviewStarted = { onOverviewGestureStarted() },
                onOverviewCancelled = onOverviewGestureCancelled,
                hasTab = { step -> adjacentTab(step) != null },
                onStepTab = { step -> adjacentTab(step)?.let { controller.selectTab(it.id) } },
            ),
            permissionActivityVisible = permissionActivityVisible,
            commandFeedback = commandFeedback,
            onAddress = openAddressEditor,
            onTabs = onTabs,
            onBarPositioned = onBarPositioned,
            actions = V5MenuActions(
                onNewTab = openNewTabAndEdit,
                onNewPrivateTab = openNewPrivateTabAndEdit,
                onDuplicateTab = { controller.duplicateSelectedTab() },
                onCloseTab = { controller.closeTabFromUser(selectedTab.id) },
                onToggleFavorite = { toggleFavoriteWithFeedback(selectedTab.id) },
                onTogglePinned = {
                    if (controller.tabOrder.setPinned(selectedTab.id, !selectedTab.isPinned)) {
                        rootView.performConfirmHaptic()
                    }
                },
                onFindInPage = {
                    onAddressEditorDismiss()
                    controller.openFindInPage()
                },
                onReaderStudio = onReaderStudio,
                onFavorites = onOpenFavorites,
                onDownloads = onOpenDownloads,
                onHistory = onOpenHistory,
                onSettings = onSettings,
                onPrivacyXRay = {
                    onPrivacyXRay()
                    rootView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                },
            ),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val hide = pageKeyboardHide.value
                    translationY = hide * VolaIsland.pageKeyboardTravel.toPx()
                    alpha = 1f - hide
                }
                .clearSemanticsWhen(hiddenByPageKeyboard || settingsVisible),
        )
    }
    // V9a: typing an address happens in the v5 editor; over a new tab with pinned sites it lets
    // them show through while empty (V28, B6a). A command's answer is a line in the editor above
    // the field, as above the bar.
    if (addressEditorVisible) {
        BrowserV5AddressEditor(
            selectedTab = selectedTab,
            accent = controller.v5Accent,
            suggestionItems = suggestionItems,
            highlightedIndex = highlightedSuggestionIndex,
            value = addressValue,
            focusNonce = addressFocusNonce,
            clipboard = clipboardOffer?.offer,
            onValueChange = onAddressValueChanged,
            onSubmit = submitAddressOrCommand,
            onSelect = selectSuggestion,
            onFill = fillAddressFromSuggestion,
            onClipboard = onClipboardOffer,
            onCancel = onAddressEditorDismiss,
            onHighlightChange = onHighlightedSuggestionChanged,
            overStart = showInteractiveBlankStart,
            commandFeedback = commandFeedback,
            searchEngine = controller.searchEngine.displayName,
            backdrop = rememberPageBackdrop(controller, selectedTab, visible = true),
            onFindOnPage = { query ->
                onAddressEditorDismiss()
                if (controller.openFindInPage()) controller.updateFindInPageQuery(query)
            },
        )
    }
    if (
        !selectedTab.isIncognito &&
        !tabOverviewVisible &&
        !addressEditorVisible &&
        !settingsVisible
    ) {
        CastControls(
            state = castUiState,
            onTogglePlayback = onToggleCastPlayback,
            onSeek = onSeekCast,
            onVolumeChange = onCastVolumeChange,
            onDisconnect = onDisconnectCast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = V5Sizes.bar + V5Spacing.space2)
                .zIndex(10f),
        )
    }

}
