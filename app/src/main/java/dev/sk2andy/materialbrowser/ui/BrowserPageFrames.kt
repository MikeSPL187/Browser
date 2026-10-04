package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserContentFrame
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.SplitViewFrames
import dev.sk2andy.materialbrowser.browser.SplitViewRules
import dev.sk2andy.materialbrowser.browser.SplitViewState
import dev.sk2andy.materialbrowser.ui.theme.VolaFrame
import dev.sk2andy.materialbrowser.ui.theme.VolaIsland
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaSplit
import kotlin.math.roundToInt

/** Split View on screen: its state, the card it splits, and the two cards it makes. */
internal class SplitViewLayout(
    val state: SplitViewState,
    val companion: BrowserTab,
    val card: BrowserContentFrame,
    val frames: SplitViewFrames,
)

/**
 * Where the page sits: [primary] is the selected tab's card (the whole page card, or its half in
 * Split View), [coveredBottomPx] how much of its bottom the expanded address bar covers now.
 */
internal class BrowserPageFrames(
    val framed: Boolean,
    val primary: BrowserContentFrame,
    val split: SplitViewLayout?,
    val coveredBottomPx: () -> Float,
)

/**
 * Lays out the page card of the framed shell (and Split View's two cards) and tells the engines
 * about it. With a dynamic toolbar the engine lays the page out for the compact capsule and the
 * expanded bar only covers the card's bottom, so scrolling never resizes the page.
 */
@Composable
internal fun rememberBrowserPageFrames(
    controller: BrowserController,
    selectedTab: BrowserTab,
    chromeVisible: Boolean,
    addressBarDocked: Boolean,
    addressBarCompactShown: Boolean,
    browserHeightPx: Float,
): BrowserPageFrames {
    val density = LocalDensity.current
    val splitState = controller.splitView.state
    LaunchedEffect(controller.activeTabs, controller.selectedTabId) {
        controller.splitView.reconcile(
            activeTabIds = controller.activeTabs.map(BrowserTab::id),
            selectedTabId = controller.selectedTabId,
        )
    }
    val companion = splitState?.let { state ->
        controller.activeTabs.firstOrNull { tab -> tab.id == state.companionTabId }
    }
    val splitShown = companion != null && chromeVisible && !controller.isActiveProfileLocked
    // Split View is always framed: two cards on the aura, whatever the shell's style.
    val contentFramed = splitShown || BrowserContentFrameRules.isFramed(
        chromeStyle = controller.appearanceSettings.chromeStyle,
        browserChromeVisible = chromeVisible,
        isBlankPage = selectedTab.url == BLANK_URL,
    )
    val frameSafeInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    fun framedContent(addressBarHeight: Dp): BrowserContentFrame = BrowserContentFrameRules.resolve(
        framed = contentFramed,
        safeLeftPx = frameSafeInsets.getLeft(density, LayoutDirection.Ltr),
        safeTopPx = frameSafeInsets.getTop(density),
        safeRightPx = frameSafeInsets.getRight(density, LayoutDirection.Ltr),
        safeBottomPx = frameSafeInsets.getBottom(density),
        sideGutterPx = with(density) { VolaFrame.sideGutter.toPx() },
        barGapPx = with(density) { VolaFrame.barGap.toPx() },
        addressBarReservePx = if (addressBarDocked) {
            0f
        } else {
            with(density) { (addressBarHeight + ADDRESS_BAR_VERTICAL_MARGIN).toPx() }
        },
    )
    val expandedContentFrame = framedContent(
        addressBarExpandedHeight(controller.appearanceSettings.addressBarStyle),
    )
    val dynamicContentFrame = contentFramed &&
        !splitShown &&
        !addressBarDocked &&
        controller.supportsDynamicContentFrame
    val contentFrame = if (dynamicContentFrame) {
        framedContent(VolaIsland.compactHeight)
    } else {
        expandedContentFrame
    }
    val dynamicBottomMaxPx = if (dynamicContentFrame) {
        (expandedContentFrame.bottomPx - contentFrame.bottomPx).coerceAtLeast(0)
    } else {
        0
    }
    val split = if (splitShown && splitState != null && companion != null) {
        SplitViewLayout(
            state = splitState,
            companion = companion,
            card = contentFrame,
            frames = SplitViewRules.frames(
                card = contentFrame,
                heightPx = browserHeightPx.roundToInt(),
                dividerPx = with(density) { VolaSplit.dividerHeight.roundToPx() },
                topRatio = splitState.topRatio,
            ),
        )
    } else {
        null
    }
    val primaryFrame = split?.frames?.of(split.state.activePane) ?: contentFrame
    val coveredBottomPx = remember { Animatable(0f) }
    var coveredBottomMaxPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(dynamicBottomMaxPx, addressBarCompactShown) {
        val target = if (addressBarCompactShown) 0f else dynamicBottomMaxPx.toFloat()
        if (coveredBottomMaxPx != dynamicBottomMaxPx) {
            // A new card size is not a bar transition: settle at once.
            coveredBottomMaxPx = dynamicBottomMaxPx
            coveredBottomPx.snapTo(target)
        } else {
            coveredBottomPx.animateTo(target, VolaMotion.standard())
        }
    }
    LaunchedEffect(controller, dynamicBottomMaxPx) {
        snapshotFlow { coveredBottomPx.value.roundToInt() }.collect { coveredPx ->
            controller.updateContentFrameCoveredBottom(dynamicBottomMaxPx, coveredPx)
        }
    }
    SideEffect {
        controller.updateContentFrame(primaryFrame)
        controller.updateSplitCompanionFrame(
            split?.frames?.of(split.state.companionPane) ?: BrowserContentFrame.None,
        )
    }
    DisposableEffect(controller) {
        onDispose {
            controller.updateContentFrame(BrowserContentFrame.None)
            controller.updateSplitCompanionFrame(BrowserContentFrame.None)
            controller.updateContentFrameCoveredBottom(maxPx = 0, coveredPx = 0)
        }
    }
    return BrowserPageFrames(
        framed = contentFramed,
        primary = primaryFrame,
        split = split,
        coveredBottomPx = { coveredBottomPx.value },
    )
}
