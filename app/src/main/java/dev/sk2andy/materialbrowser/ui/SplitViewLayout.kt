package dev.sk2andy.materialbrowser.ui

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.SplitPane
import dev.sk2andy.materialbrowser.browser.SplitViewRules
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaSplit
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import kotlin.math.roundToInt

internal object SplitViewTestTags {
    const val CompanionPane = "split_view_companion_pane"
    const val Divider = "split_view_divider"
    const val Swap = "split_view_swap"
    const val Close = "split_view_close"
}

/**
 * Split View's other tab in its card (board W-Split). The page is live, but the first tap only
 * makes this card the active one, as on the board: the bar, find in page and the menu follow.
 */
@Composable
internal fun SplitCompanionPane(
    controller: BrowserController,
    layout: SplitViewLayout,
    onActivate: () -> Unit,
) {
    val companion = layout.companion
    val pane = layout.state.companionPane
    val title = companion.title.ifBlank { companion.url }
    val description = stringResource(
        if (pane == SplitPane.Top) R.string.split_view_pane_top else R.string.split_view_pane_bottom,
        title,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .contentFramePadding(layout.frames.of(pane))
            .background(VolaTheme.extendedColors.card),
    ) {
        if (companion.url == BLANK_URL) {
            Text(
                text = stringResource(R.string.new_tab_title),
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            // The selected tab's session follows the activity in BrowserController; this one
            // follows the pane: active while the screen is in front, in the background otherwise.
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle, companion.id) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> controller.setSplitCompanionActive(true)
                        Lifecycle.Event.ON_STOP -> controller.setSplitCompanionActive(false)
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            val engineViewRevision = controller.engineViewRevision
            AndroidView(
                factory = { context -> FrameLayout(context) },
                update = { container ->
                    // Reading the tab and the revision re-attaches when either changes.
                    if (companion.id.isNotEmpty() && engineViewRevision >= 0) {
                        controller.attachSplitCompanionView(container) { }
                    }
                },
                onRelease = controller::detachBrowserEngineView,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClickLabel = stringResource(R.string.split_view_activate),
                    role = Role.Button,
                    onClick = onActivate,
                )
                .semantics { contentDescription = description }
                .testTag(SplitViewTestTags.CompanionPane),
        )
    }
}

/**
 * The divider between the cards: drag it to resize, it snaps to a third, a half or two thirds
 * when let go, and closes Split View at an edge. Its pill also swaps the tabs and closes.
 */
@Composable
internal fun BoxScope.SplitViewDivider(
    layout: SplitViewLayout,
    heightPx: Float,
    onRatioChange: (Float) -> Unit,
    onSwap: () -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val dividerPx = with(density) { VolaSplit.dividerHeight.toPx() }
    val controlsPx = with(density) { VolaSplit.controlsHeight.toPx() }
    val card = layout.card
    val available = (heightPx - card.topPx - card.bottomPx - dividerPx).coerceAtLeast(1f)
    val ratio = layout.state.topRatio
    val currentRatio by rememberUpdatedState(ratio)
    var dragRatio by remember { mutableFloatStateOf(ratio) }
    val dragState = rememberDraggableState { delta ->
        dragRatio = SplitViewRules.dragRatio(dragRatio + delta / available)
        onRatioChange(dragRatio)
    }
    val dividerTopPx = card.topPx + available * ratio
    val resizeLabel = stringResource(R.string.split_view_resize)
    val swapLabel = stringResource(R.string.split_view_swap)
    val closeLabel = stringResource(R.string.split_view_close)
    val dragModifier = Modifier.draggable(
        state = dragState,
        orientation = Orientation.Vertical,
        onDragStarted = { dragRatio = currentRatio },
        onDragStopped = {
            val settled = SplitViewRules.settle(dragRatio)
            if (settled == null) onClose() else onRatioChange(settled)
        },
    )
    // The aura gap between the cards answers the drag across its whole width.
    Box(
        modifier = Modifier
            .offset { IntOffset(0, dividerTopPx.roundToInt()) }
            .fillMaxWidth()
            .height(VolaSplit.dividerHeight)
            .then(dragModifier),
    )
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset { IntOffset(0, (dividerTopPx + (dividerPx - controlsPx) / 2f).roundToInt()) }
            .height(VolaSplit.controlsHeight)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = VolaSplit.CONTROLS_ALPHA),
                CircleShape,
            )
            .padding(horizontal = VolaSplit.controlsPadding)
            .testTag(SplitViewTestTags.Divider),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onSwap,
            modifier = Modifier
                .size(VolaSplit.controlsHeight)
                .testTag(SplitViewTestTags.Swap),
        ) {
            Icon(
                VolaIcons.SwapVert,
                contentDescription = swapLabel,
                modifier = Modifier.size(VolaSplit.controlIconSize),
            )
        }
        Box(
            modifier = Modifier
                .size(VolaSplit.handleWidth, VolaSplit.controlsHeight)
                .then(dragModifier)
                .clearAndSetSemantics {
                    contentDescription = resizeLabel
                    customActions = SplitViewRules.SNAP_RATIOS.map { snap ->
                        CustomAccessibilityAction("${(snap * PERCENT).roundToInt()}%") {
                            onRatioChange(snap)
                            true
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(VolaSplit.handleWidth, VolaSplit.handleHeight)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = VolaSplit.HANDLE_ALPHA),
                        CircleShape,
                    ),
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(VolaSplit.controlsHeight)
                .testTag(SplitViewTestTags.Close),
        ) {
            Icon(
                VolaIcons.Close,
                contentDescription = closeLabel,
                modifier = Modifier.size(VolaSplit.controlIconSize),
            )
        }
    }
}

private const val PERCENT = 100
