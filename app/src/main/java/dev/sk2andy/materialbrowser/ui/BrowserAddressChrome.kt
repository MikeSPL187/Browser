@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)

package dev.sk2andy.materialbrowser.ui

import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.ClipboardAction
import dev.sk2andy.materialbrowser.browser.ClipboardOfferRules
import dev.sk2andy.materialbrowser.browser.cast.CastUiState
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import dev.sk2andy.materialbrowser.data.TabDeletionRules
import dev.sk2andy.materialbrowser.reader.ReaderStudioSessionRules
import dev.sk2andy.materialbrowser.shared.browser.AddressBarLongPressAction
import dev.sk2andy.materialbrowser.shared.browser.AddressBarLongPressActionRules
import dev.sk2andy.materialbrowser.shared.browser.AddressBarLongPressContext
import eightbitlab.com.blurview.BlurTarget
import kotlin.math.absoluteValue
import dev.sk2andy.materialbrowser.ui.theme.VolaCompactMode
import dev.sk2andy.materialbrowser.ui.theme.VolaIsland
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberUiV5Flag
import dev.sk2andy.materialbrowser.ui.v5.V5BarGestures

@Composable
internal fun BoxScope.BrowserAddressChrome(
    controller: BrowserController,
    selectedTab: BrowserTab,
    addressEditorVisible: Boolean,
    showInteractiveBlankStart: Boolean,
    blankTabModeProgress: Float,
    blankTabModeRevealOrigin: Offset,
    profileWallpaperRuntime: ProfileWallpaperRuntime?,
    commandFeedback: AddressCommandFeedback?,
    suggestionItems: List<AddressSuggestionItem>,
    highlightedSuggestionIndex: Int,
    browserHeightPx: Float,
    bottomBarTopPx: MutableFloatState,
    browserContentBlurTarget: BlurTarget?,
    /** False when the framed page card ends above the bar, so there is no page to blur. */
    pageBehindAddressBar: Boolean,
    linkPeekAddressBarExpanded: Boolean,
    castUiState: CastUiState,
    settingsVisible: Boolean,
    addressValue: TextFieldValue,
    domainCompletion: String?,
    addressFocusNonce: Int,
    showAiModeToggle: Boolean,
    aiModeSelectedState: MutableState<Boolean>,
    browserDragOffset: MutableFloatState,
    browserWidthPx: Float,
    tabSwitchTravelPx: Float,
    tabHandoffAlpha: Animatable<Float, AnimationVector1D>,
    tabOverviewOpening: Boolean,
    tabOverviewVisible: Boolean,
    overviewGestureProgress: MutableFloatState,
    addressBarMorphInFront: Boolean,
    browserRootBottomInWindowPx: Int,
    visibleSnoozedTabCount: Int,
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
    moveSuggestionHighlight: (Int) -> Unit,
    submitAddressOrCommand: (String) -> Unit,
    onLiveFrameCleared: () -> Unit,
    onTabHandoffChanged: (TabHandoff?) -> Unit,
    onTabs: () -> Unit,
    onOverviewGestureStarted: () -> Unit,
    onOverviewGestureCancelled: () -> Unit,
    openNewTabAndEdit: () -> Unit,
    openNewPrivateTabAndEdit: () -> Unit,
    toggleFavoriteWithFeedback: (String) -> Unit,
    onBlankTabModeRevealOriginChanged: (Offset) -> Unit,
    onSnoozedTabs: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit,
    onSettings: () -> Unit,
    onPrivacyXRay: () -> Unit,
    onNewTabButtonBoundsChanged: (Rect?) -> Unit,
    onReaderStudio: () -> Unit,
    onOpenCandyTrail: () -> Unit,
    onSnooze: () -> Unit,
    onAddSiteCapsule: () -> Unit,
    onAddressBarLongPressAction: (AddressBarLongPressAction) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val wideWindow = rememberWideBrowserWindow(windowWidthDp = browserWidthPx / density.density)
    val wideTabStripEnabled = wideWindow && !linkPeekAddressBarExpanded
    val tabSwipeEnabled = !wideWindow
    val blankTabTitle = stringResource(R.string.new_tab_title)
    val privateTabTitle = stringResource(R.string.private_tab_title)
    val wideTabs = if (wideTabStripEnabled) {
        controller.activeTabs.map { tab ->
            val hidden = controller.privateTabsLock.hides(tab)
            WideAddressTabItem(
                id = tab.id,
                title = if (hidden) {
                    privateTabTitle
                } else {
                    tab.title.ifBlank {
                        if (tab.url == BLANK_URL) {
                            blankTabTitle
                        } else {
                            AddressResolver.displayText(tab.url)
                        }
                    }
                },
                favicon = controller.favicons[tab.id]?.takeUnless { hidden },
                canClose = TabDeletionRules.canDelete(tab),
            )
        }
    } else {
        emptyList()
    }
    LaunchedEffect(tabSwipeEnabled) {
        if (!tabSwipeEnabled) browserDragOffset.floatValue = 0f
    }
    val rootView = LocalView.current
    val longPressActionHapticNonce = controller.contentActions.longPressActionHapticNonce
    LaunchedEffect(longPressActionHapticNonce) {
        if (longPressActionHapticNonce != 0) {
            rootView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    val chromeBackdropSource = browserContentBlurTarget.asCandyChromeBackdropSource()
    val qrScanFailureMessage = stringResource(R.string.toast_qr_scan_failed)
    val qrScanner = rememberQrCodeScanner()
    var qrScanInProgress by remember { mutableStateOf(false) }
    val selectedSiteState = controller.siteProtectionState(selectedTab.id)
    val selectedSiteHasHost = controller.supportsPageContentActions &&
        selectedSiteState.host != null
    val permissionActivityVisible = controller.hasPermissionActivity(selectedTab.id)
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val fullWindowHeightPx = context
        .getSystemService(WindowManager::class.java)
        .currentWindowMetrics
        .bounds
        .height()
    val visibleViewportHeightPx = AddressBarInsetRules.visibleViewportHeightPx(
        rootHeightPx = browserHeightPx.toInt(),
        fullWindowHeightPx = fullWindowHeightPx,
        rootBottomInWindowPx = browserRootBottomInWindowPx,
        imeBottomPx = imeBottomPx,
    )
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
    val uiV5 by rememberUiV5Flag()
    // V9a: under ui.v5 typing an address happens in the v5 editor on a phone; over a new tab with
    // pinned sites it lets them show through while empty (V28, B6a). A command's answer is a line
    // in the editor above the field, as above the bar.
    val v5Editor = uiV5 && addressEditorVisible && !wideWindow
    if (addressEditorVisible && !v5Editor) {
        if (!showInteractiveBlankStart) {
            AddressEditorBackdrop(
                showStartContent = selectedTab.url == BLANK_URL,
                modeProgress = blankTabModeProgress,
                revealOriginInRoot = blankTabModeRevealOrigin,
                wallpaper = profileWallpaperRuntime.takeUnless { selectedTab.isIncognito },
                onDismiss = onAddressEditorDismiss,
            )
            controller.profiles.firstOrNull { it.id == controller.activeProfileId }?.let { workspace ->
                AddressEditorHeader(
                    workspace = workspace,
                    newTab = selectedTab.url == BLANK_URL,
                    privateTab = selectedTab.isIncognito,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding(),
                )
            }
        }
        if (commandFeedback == null) {
            AddressSuggestions(
                suggestions = if (showInteractiveBlankStart) emptyList() else suggestionItems,
                query = addressValue.text,
                highlightedIndex = highlightedSuggestionIndex,
                onHighlight = onHighlightedSuggestionChanged,
                onSelect = selectSuggestion,
                onFill = fillAddressFromSuggestion,
                rootHeightPx = browserHeightPx,
                bottomBarTopPx = bottomBarTopPx,
                modifier = Modifier.align(Alignment.BottomCenter),
                workspaces = controller.profiles.associateBy { it.id },
                clipboardOffer = clipboardOffer?.offer,
                onClipboardOffer = onClipboardOffer,
            )
        }
    }

    // Docking to a side edge is a v4 gesture: the v5 bar has its own place on a phone (V28, B7).
    val addressBarDockingAvailable = !(uiV5 && !wideWindow) && AddressBarDockingRules.isAvailable(
        settingEnabled = controller.addressBar.isDockingEnabled,
        isBlankTab = selectedTab.url == BLANK_URL,
    )
    val effectiveAddressBarDockPlacement = controller.addressBar.dockPlacement
        .takeIf { addressBarDockingAvailable && !linkPeekAddressBarExpanded }
    val addressBarLongPressAction = controller.addressBar.longPressAction
    val hasHttpPage = BrowserUriPolicy.normalizeHttpUrl(selectedTab.url) != null
    val addressBarLongPressEnabled = AddressBarLongPressActionRules.isAvailable(
        action = addressBarLongPressAction,
        context = AddressBarLongPressContext(
            hasPage = selectedTab.url != BLANK_URL,
            hasHttpPage = hasHttpPage,
            isPrivate = selectedTab.isIncognito,
            canGoBack = selectedTab.canGoBack,
            canOpenReader = ReaderStudioSessionRules.isSupportedSource(selectedTab.url),
            canToggleFavorite = selectedTab.url != BLANK_URL && !selectedTab.isIncognito,
            canToggleDesktopView = controller.canToggleSelectedDesktopView,
            canParkAddressBar = addressBarDockingAvailable,
            canCreateSiteCapsule = controller.canCreateSiteCapsule &&
                !selectedTab.isIncognito &&
                hasHttpPage,
            canCreatePrivateTab = controller.canCreatePrivateTabInActiveProfile,
            canSnoozeTab = controller.canSnoozeSelectedTab,
            hasMoveTargetProfile = controller.compatibleMoveTargetProfiles(selectedTab.id).isNotEmpty(),
        ),
    )
    val addressBarLongPressLabel = stringResource(
        addressBarLongPressAction.runtimeLabelRes(
            isFavorite = controller.isSelectedTabFavorite,
            isPinned = selectedTab.isPinned,
            isDesktopView = controller.isSelectedDesktopView,
        ),
    )
    var addressBarBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    val autoDockGeometryAvailable = addressBarDockingAvailable &&
        effectiveAddressBarDockPlacement == null &&
        !addressEditorVisible &&
        !settingsVisible &&
        !tabOverviewOpening &&
        !tabOverviewVisible &&
        !linkPeekAddressBarExpanded &&
        commandFeedback == null
    LaunchedEffect(
        autoDockGeometryAvailable,
        addressBarBoundsInRoot,
        browserWidthPx,
        visibleViewportHeightPx,
    ) {
        val bounds = addressBarBoundsInRoot
        if (autoDockGeometryAvailable && bounds != null) {
            controller.addressBar.setBoundsInViewport(
                leftPx = bounds.left,
                topPx = bounds.top,
                rightPx = bounds.right,
                bottomPx = bounds.bottom,
                viewportWidthPx = browserWidthPx,
                viewportHeightPx = visibleViewportHeightPx.toFloat(),
            )
        } else {
            controller.addressBar.clearBoundsInViewport()
        }
    }
    DisposableEffect(controller) {
        onDispose(controller.addressBar::clearBoundsInViewport)
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
        if (tabSwipeEnabled && !addressEditorVisible && !tabOverviewVisible) {
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
            if (uiV5) {
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
    }
    val onTabDragStopped: suspend (Float) -> Unit = { velocity ->
        val direction = browserDragOffset.floatValue.compareTo(0f)
        val tabs = controller.activeTabs
        val currentIndex = tabs.indexOfFirst { it.id == controller.selectedTabId }
        val targetTab = tabs.getOrNull(currentIndex - direction)
        val minTravel = with(density) { AddressBarTabSwitchRules.FLING_MIN_TRAVEL_DP.dp.toPx() }
        val flingVelocityDp = if (uiV5) {
            V5Motion.TAB_SWIPE_COMMIT_VELOCITY_DP
        } else {
            AddressBarTabSwitchRules.FLING_MIN_VELOCITY_DP
        }
        val fastEnough = browserDragOffset.floatValue.absoluteValue >= minTravel &&
            velocity.absoluteValue >= with(density) { flingVelocityDp.dp.toPx() } &&
            velocity.compareTo(0f) == direction
        val farEnough = if (uiV5) {
            browserDragOffset.floatValue.absoluteValue >= browserWidthPx * V5Motion.TAB_SWIPE_COMMIT_FRACTION
        } else {
            AddressBarTabSwitchRules.hasReachedDistance(
                dragDistance = browserDragOffset.floatValue.absoluteValue,
                viewportWidth = browserWidthPx,
            )
        }
        v5SwipeThresholdReached = false
        val shouldSwitch = tabSwipeEnabled && targetTab != null && (farEnough || fastEnough)
        // v5: the rubber band past the last tab, let go near its end, opens a new tab once it springs back.
        val opensNewTab = uiV5 && tabSwipeEnabled && targetTab == null && direction < 0 &&
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
            animationSpec = if (uiV5) {
                if (V5Motion.animationsEnabled) V5Motion.tabSwipe.spec() else V5Motion.reducedFade()
            } else if (shouldSwitch) {
                tween(AddressBarTabSwitchRules.SETTLE_MILLIS, easing = FastOutSlowInEasing)
            } else {
                spring(
                    dampingRatio = AddressBarTabSwitchRules.CANCEL_DAMPING_RATIO,
                    stiffness = AddressBarTabSwitchRules.CANCEL_STIFFNESS,
                )
            },
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
        addressBarBoundsInRoot = boundsInRoot
        if (
            effectiveAddressBarDockPlacement != null &&
            !addressEditorVisible &&
            commandFeedback == null
        ) {
            bottomBarTopPx.floatValue = Float.NaN
            controller.setPreviewContentBottomInWindowPx(0)
        } else {
            bottomBarTopPx.floatValue = boundsInRoot.top
            controller.setPreviewContentBottomInWindowPx(topInWindowPx)
        }
    }
    // Compact Mode is a v4 setting with no switch under v5 (V28, step 4): a phone that kept it on
    // would start every page with the v5 bar folded and no way back, so v5 turns it off.
    val drawsV5Bar = uiV5 && !wideWindow
    LaunchedEffect(drawsV5Bar, controller.addressBar.compactMode) {
        if (drawsV5Bar && controller.addressBar.compactMode) controller.addressBar.updateCompactMode(false)
    }
    val compactBar = controller.isBottomBarCompact &&
        // A new tab is where an address is typed: its bar stays open even in Compact Mode.
        selectedTab.url != BLANK_URL &&
        !linkPeekAddressBarExpanded &&
        !wideWindow
    // V7: the v5 bar replaces the bar at rest and while collapsed; command feedback is a line above
    // it (V28, B6b). Editing, the overview morph and wide windows stay on the current bar until their
    // v5 steps. The v5 link preview (V21) keeps the v5 bar under it: the current bar's expanded state
    // is for the current preview.
    val showV5Bar = uiV5 &&
        !addressEditorVisible &&
        !wideWindow &&
        effectiveAddressBarDockPlacement == null &&
        !tabOverviewOpening &&
        !tabOverviewVisible &&
        !addressBarMorphInFront
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
                tabSwipeEnabled = tabSwipeEnabled,
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
    if (v5Editor) {
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
        )
    }
    // V10a: the v5 overview has its own toolbar, so no bar shows while it opens or is open.
    val v5Overview = uiV5 && !wideWindow && (tabOverviewOpening || tabOverviewVisible)
    if (!showV5Bar && !v5Editor && !v5Overview) BrowserBottomBar(
        tab = selectedTab,
        pageTranslationProvider = controller.pageTranslationProvider,
        compact = compactBar,
        dockState = AddressBarDockState(
            enabled = addressBarDockingAvailable,
            placement = effectiveAddressBarDockPlacement,
        ),
        dockTargetEdge = controller.addressBar.lastDockEdge,
        editing = addressEditorVisible,
        addressBarStyle = controller.appearanceSettings.addressBarStyle,
        actionLayout = controller.addressBar.actionLayout,
        // Cast and the system code scanner need Google Play services, which Vola does not ship.
        showCastButton = false,
        showQrScanner = false,
        tabCount = controller.activeTabs.size,
        wideTabs = wideTabs,
        wideTabStripEnabled = wideTabStripEnabled,
        tabSwipeEnabled = tabSwipeEnabled,
        onWideTabSelected = controller::selectTab,
        onWideTabClosed = { tabId -> controller.closeTabFromUser(tabId) },
        userScriptMenuCommands = if (controller.isUserScriptSupported) {
            controller.selectedUserScriptMenuCommands
        } else {
            emptyList()
        },
        supportsPageContentActions = controller.supportsPageContentActions,
        onUserScriptMenuCommand = controller::invokeUserScriptMenuCommand,
        commandFeedback = commandFeedback,
        backdropSource = chromeBackdropSource.takeIf { pageBehindAddressBar },
        blurSourceVisible = pageBehindAddressBar &&
            browserContentBlurTarget != null &&
            !tabOverviewVisible,
        feedbackGesturesEnabled = !addressEditorVisible && !settingsVisible,
        onBack = controller::goBack,
        onForward = controller::goForward,
        onAddress = openAddressEditor,
        addressBarLongPressEnabled = addressBarLongPressEnabled,
        addressBarLongPressLabel = addressBarLongPressLabel,
        onAddressBarLongPress = {
            onAddressBarLongPressAction(addressBarLongPressAction)
        },
        editValue = addressValue,
        onEditValueChange = onAddressValueChanged,
        ghostCompletion = domainCompletion,
        onAcceptGhostCompletion = {
            domainCompletion?.let { completion ->
                onAddressValueChanged(
                        TextFieldValue(
                            text = completion,
                            selection = TextRange(completion.length),
                        ),
                    )
                rootView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        },
        addressFocusNonce = addressFocusNonce,
        onMoveAddressSuggestion = moveSuggestionHighlight,
        onActivateAddressSuggestion = {
            val highlighted = suggestionItems.getOrNull(highlightedSuggestionIndex)
            if (highlighted == null) {
                submitAddressOrCommand(
                    AddressEditorCompletionRules.submissionText(
                        input = addressValue.text,
                        ghostCompletion = domainCompletion,
                    ),
                )
            } else {
                selectSuggestion(highlighted)
            }
        },
        onDismissEditor = { onAddressEditorDismiss() },
        onSubmitAddress = submitAddressOrCommand,
        showAiModeToggle = showAiModeToggle,
        aiModeSelected = aiModeSelectedState.value,
        onAiModeSelectedChange = { selected ->
            aiModeSelectedState.value = selected
            rootView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        },
        onScanQrCode = {
            if (!qrScanInProgress) {
                qrScanInProgress = true
                qrScanner.startScan(
                    onSuccess = { scannedValue ->
                        qrScanInProgress = false
                        if (scannedValue.isEmpty()) {
                            Toast.makeText(
                                context,
                                qrScanFailureMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            controller.submitAddress(scannedValue)
                            onAddressEditorDismiss()
                        }
                    },
                    onCanceled = { qrScanInProgress = false },
                    onFailure = {
                        qrScanInProgress = false
                        Toast.makeText(
                            context,
                            qrScanFailureMessage,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }
        },
        onExpand = controller::expandBottomBar,
        onDock = { controller.addressBar.updateDocked(true) },
        onParkRight = controller.addressBar::parkOnRight,
        onDockPlacementChanged = controller.addressBar::updateDockPlacement,
        onRestoreDock = { controller.addressBar.updateDocked(false) },
        onTabDrag = onTabDrag,
        onTabDragStopped = onTabDragStopped,
        onTabs = onTabs,
        overviewGestureEnabled = !tabOverviewOpening && !tabOverviewVisible,
        overviewGestureProgress = overviewGestureProgress,
        showingTabOverview = tabOverviewVisible,
        visualOnly = addressBarMorphInFront,
        rootBottomInWindowPx = browserRootBottomInWindowPx,
        onOverviewGestureProgress = { progress ->
            onOverviewGestureStarted()
            overviewGestureProgress.floatValue = progress.coerceIn(0f, 1f)
        },
        onOverviewGestureStarted = { onOverviewGestureStarted() },
        onOverviewGestureCancelled = onOverviewGestureCancelled,
        onReload = controller::reload,
        onStop = controller::stopLoading,
        onNewTab = openNewTabAndEdit,
        onHome = { controller.openNormalHome() },
        onDuplicateTab = { controller.duplicateSelectedTab() },
        onFindInPage = {
            onAddressEditorDismiss()
            controller.openFindInPage()
        },
        onCloseTab = { controller.closeTabFromUser(selectedTab.id) },
        onToggleIncognito = {
            if (controller.setBlankTabIncognito(enabled = !selectedTab.isIncognito)) {
                rootView.performConfirmHaptic()
            }
        },
        blankTabModeProgress = blankTabModeProgress,
        onIncognitoControlCenterChanged = onBlankTabModeRevealOriginChanged,
        isFavorite = controller.isSelectedTabFavorite,
        onToggleFavorite = { toggleFavoriteWithFeedback(selectedTab.id) },
        isPinned = selectedTab.isPinned,
        onTogglePinned = {
            if (controller.tabOrder.setPinned(selectedTab.id, !selectedTab.isPinned)) {
                rootView.performConfirmHaptic()
            }
        },
        canToggleDomainMute = controller.canToggleSelectedDomainMute,
        isDomainMuted = controller.isSelectedDomainMuted,
        canSnooze = controller.canSnoozeSelectedTab,
        onDomainMutedChange = controller::setSelectedDomainMuted,
        canToggleDesktopView = controller.canToggleSelectedDesktopView,
        isDesktopView = controller.isSelectedDesktopView,
        onDesktopViewChange = controller::setSelectedDesktopView,
        onCompactModeChange = controller.addressBar::updateCompactMode,
        canToggleForceVerticalScrolling = selectedSiteHasHost,
        isForceVerticalScrollingEnabled = selectedSiteState.forceVerticalScrolling,
        onForceVerticalScrollingChange = { enabled ->
            if (controller.setForceVerticalScrolling(selectedTab.id, enabled)) {
                rootView.performConfirmHaptic()
            }
        },
        snoozedTabCount = visibleSnoozedTabCount,
        onSnoozedTabs = {
            onAddressEditorDismiss()
            onSnoozedTabs()
        },
        onFavorites = {
            onAddressEditorDismiss()
            onOpenFavorites()
        },
        onDownloads = {
            onAddressEditorDismiss()
            onOpenDownloads()
        },
        onHistory = {
            onAddressEditorDismiss()
            onOpenHistory()
        },
        firefoxExtensionActions = controller.firefoxExtensionActions,
        menuLayout = controller.browserMenuLayout,
        onFirefoxExtensionAction = { actionKey ->
            onAddressEditorDismiss()
            controller.clickFirefoxExtensionAction(actionKey)
        },
        onSettings = {
            onAddressEditorDismiss()
            onSettings()
        },
        onPrivacyXRay = {
            onPrivacyXRay()
            rootView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        },
        permissionActivityVisible = permissionActivityVisible,
        addressBarPulseNonce = controller.contentActions.addressBarPulseNonce,
        newTabPulseNonce = controller.contentActions.linkPeekNewTabPulseNonce,
        onNewTabButtonBounds = onNewTabButtonBoundsChanged,
        onOpenExternal = controller::openSelectedPageExternally,
        onSummarizeWithAssistant = controller::summarizeSelectedPageWithAssistant,
        onShare = controller::shareSelectedPage,
        onPrint = controller::printSelectedPage,
        onTranslate = controller::translateSelectedPage,
        splitView = controller.splitView.state != null,
        onSplitViewChange = { enabled ->
            if (!enabled) {
                controller.splitView.close()
            } else if (!controller.openSplitView()) {
                Toast.makeText(
                    rootView.context,
                    R.string.split_view_no_companion,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        onReaderStudio = onReaderStudio,
        onOpenCandyTrail = onOpenCandyTrail,
        onSnooze = onSnooze,
        onAddSiteCapsule = onAddSiteCapsule,
        onBarPositioned = onBarPositioned,
        auraRim = controller.appearanceSettings.chromeStyle == BrowserChromeStyle.Air,
        handle = controller.addressBar.compactMode,
        backdropBlurRegionEnabled = pageBehindAddressBar &&
            browserDragOffset.floatValue == 0f &&
            !settingsVisible,
        onBackdropBlurRegionChanged = controller::setSelectedBrowserBackdropBlurRegion,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(
                when {
                    commandFeedback != null && !tabOverviewVisible -> 30f
                    addressBarMorphInFront -> 20f
                    else -> 0f
                },
            )
            .graphicsLayer {
                val hide = pageKeyboardHide.value
                translationY = hide * VolaIsland.pageKeyboardTravel.toPx()
                alpha = 1f - hide
            }
            // Settings cover the screen: TalkBack must not wander to the bar under them.
            .clearSemanticsWhen(hiddenByPageKeyboard || settingsVisible),
    )
    CompactModeHint(
        visible = controller.addressBar.compactModeHintVisible && !tabOverviewVisible,
        onDismiss = controller.addressBar::dismissCompactModeHint,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = VolaCompactMode.hintBottomOffset)
            .zIndex(25f),
    )

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
                .padding(
                    bottom = if (effectiveAddressBarDockPlacement != null) 12.dp else 88.dp,
                )
                .zIndex(10f),
        )
    }

}
