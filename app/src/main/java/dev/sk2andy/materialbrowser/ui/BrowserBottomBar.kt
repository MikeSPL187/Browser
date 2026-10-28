@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)

package dev.sk2andy.materialbrowser.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AddressBarDockEdge
import dev.sk2andy.materialbrowser.data.BrowserAddressBarStyle
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaIsland
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
internal object AddressBarDockTestTags {
    const val EdgeTab = "address_bar_edge_tab"
    const val ParkAction = "address_bar_park_action"
    const val ParkIcon = "address_bar_park_icon"
    const val CompactAddress = "address_bar_compact_address"
    const val CompactContent = "address_bar_compact_content"
    const val CompactHandle = "address_bar_compact_handle"
}

@Composable
internal fun AddressBarCompactContent(
    domain: String,
    secure: Boolean = false,
    showCastButton: Boolean,
    dockingEnabled: Boolean,
    dockTargetEdge: AddressBarDockEdge,
    onDock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .testTag(AddressBarDockTestTags.CompactContent),
        horizontalArrangement = when {
            !dockingEnabled -> Arrangement.Center
            AddressBarDockingRules.parkActionPrecedesAddress(dockTargetEdge) -> Arrangement.Start
            else -> Arrangement.End
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (
            dockingEnabled &&
            AddressBarDockingRules.parkActionPrecedesAddress(dockTargetEdge)
        ) {
            AddressBarParkAction(
                edge = dockTargetEdge,
                onDock = onDock,
            )
        }
        if (secure) {
            Icon(
                imageVector = VolaIcons.Lock,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = VolaSpacing.x1)
                    .size(COMPACT_LOCK_ICON_SIZE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = domain,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .offset(
                    x = AddressBarDockingRules
                        .compactAddressContentOffsetDp(dockTargetEdge).dp,
                )
                .testTag(AddressBarDockTestTags.CompactAddress),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
        if (showCastButton) {
            CastRouteButton()
        }
        if (
            dockingEnabled &&
            !AddressBarDockingRules.parkActionPrecedesAddress(dockTargetEdge)
        ) {
            AddressBarParkAction(
                edge = dockTargetEdge,
                onDock = onDock,
            )
        }
    }
}

@Composable
private fun AddressBarParkAction(
    edge: AddressBarDockEdge,
    onDock: () -> Unit,
) {
    IconButton(
        onClick = onDock,
        modifier = Modifier.testTag(AddressBarDockTestTags.ParkAction),
    ) {
        AddressBarParkIcon(
            edge = edge,
            contentDescription = stringResource(R.string.action_dock_address_bar),
        )
    }
}

@Composable
private fun AddressBarParkIcon(
    edge: AddressBarDockEdge,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .testTag(AddressBarDockTestTags.ParkIcon)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Icon(
            imageVector = VolaIcons.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
                .graphicsLayer {
                    rotationZ = AddressBarDockingRules.parkChevronRotationDegrees(edge)
                },
        )
    }
}

@Composable
internal fun AddressCommandFeedback.localizedText(): String = when (message) {
    AddressCommandFeedbackMessage.CacheCleared ->
        stringResource(R.string.command_feedback_cache_cleared)
    AddressCommandFeedbackMessage.CookiesCleared ->
        stringResource(R.string.command_feedback_cookies_cleared)
    AddressCommandFeedbackMessage.Reloaded ->
        stringResource(R.string.command_feedback_reloaded)
    AddressCommandFeedbackMessage.LoadingStopped ->
        stringResource(R.string.command_feedback_loading_stopped)
    AddressCommandFeedbackMessage.TabPinned ->
        stringResource(R.string.command_feedback_tab_pinned)
    AddressCommandFeedbackMessage.TabUnpinned ->
        stringResource(R.string.command_feedback_tab_unpinned)
    AddressCommandFeedbackMessage.DuplicateTabsClosed -> pluralStringResource(
        R.plurals.command_feedback_duplicates_closed,
        count,
        count,
    )
    AddressCommandFeedbackMessage.TabMoved -> stringResource(
        R.string.command_feedback_tab_moved,
        targetProfileLabel.orEmpty(),
    )
    AddressCommandFeedbackMessage.ProfileSwitched -> stringResource(
        R.string.command_feedback_profile_switched,
        targetProfileLabel.orEmpty(),
    )
    AddressCommandFeedbackMessage.RegularTabCreated ->
        stringResource(R.string.command_feedback_regular_tab_created)
    AddressCommandFeedbackMessage.IncognitoTabCreated ->
        stringResource(R.string.command_feedback_incognito_tab_created)
    AddressCommandFeedbackMessage.SettingsOpened ->
        stringResource(R.string.command_feedback_settings_opened)
    AddressCommandFeedbackMessage.Rejected ->
        stringResource(R.string.command_feedback_rejected)
}

@Composable
internal fun Modifier.addressBarVerticalGesture(
    enabled: Boolean = true,
    initialProgress: FloatState? = null,
    onProgress: (Float) -> Unit = {},
    onStarted: () -> Unit = {},
    onCancelled: () -> Unit = {},
    onSwipeUp: () -> Unit,
): Modifier {
    val currentInitialProgress by rememberUpdatedState(initialProgress)
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnStarted by rememberUpdatedState(onStarted)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val gestureView = LocalView.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    if (!enabled) return this
    return pointerInput(enabled, touchSlop) {
        val threshold = AddressBarGestureRules.OPEN_TABS_THRESHOLD_DP.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            var lastY = down.position.y
            var accumulatedX = 0f
            var accumulatedY = 0f
            var gestureActive = false
            var state = AddressBarOverviewGestureRules.stateForProgress(
                progress = currentInitialProgress?.floatValue ?: 0f,
                threshold = threshold,
            )
            var committed = false
            try {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.position - change.previousPosition
                    if (!gestureActive) {
                        accumulatedX += delta.x
                        accumulatedY += delta.y
                        when (
                            AddressBarOverviewGestureRules.direction(
                                dragX = accumulatedX,
                                dragY = accumulatedY,
                                touchSlop = touchSlop,
                            )
                        ) {
                            AddressBarOverviewGestureDirection.Pending -> {
                                lastY = change.position.y
                                if (!change.pressed) break
                                continue
                            }
                            AddressBarOverviewGestureDirection.Rejected -> break
                            AddressBarOverviewGestureDirection.Upward -> {
                                gestureActive = true
                                currentOnStarted()
                                lastY = down.position.y
                            }
                        }
                    }
                    val update = AddressBarOverviewGestureRules.update(
                        state = state,
                        deltaY = change.position.y - lastY,
                        threshold = threshold,
                    )
                    state = update.state
                    lastY = change.position.y
                    change.consume()
                    currentOnProgress(update.progress)
                    if (!change.pressed) {
                        val release = AddressBarOverviewGestureRules.release(state)
                        if (release.shouldCommit) {
                            committed = true
                            currentOnProgress(release.progress)
                            gestureView.performHapticFeedback(
                                HapticFeedbackConstants.VIRTUAL_KEY,
                            )
                            currentOnSwipeUp()
                        }
                        break
                    }
                }
            } finally {
                if (gestureActive && !committed) currentOnCancelled()
            }
        }
    }
}

/** The lock next to the site in the compact capsule, sized to its label. */
private val COMPACT_LOCK_ICON_SIZE = 14.dp

/** Space the bottom address bar keeps above and below itself. */
internal val ADDRESS_BAR_VERTICAL_MARGIN = 12.dp
internal val ADDRESS_BAR_HORIZONTAL_MARGIN = VolaSpacing.x4

/** Height of the expanded bottom address bar for [style]. */
internal fun addressBarExpandedHeight(style: BrowserAddressBarStyle): Dp =
    if (style == BrowserAddressBarStyle.Segmented) {
        SegmentedAddressBarGeometry.EXPANDED_HEIGHT
    } else {
        AddressBarMotion.EXPANDED_HEIGHT
    }

/** The aura rim and glow of the island in the Air layout (`.halo` in vola4.css). */
@Composable
internal fun Modifier.islandAuraRim(shape: Shape): Modifier {
    val colors = VolaTheme.extendedColors
    return this
        .shadow(
            elevation = VolaIsland.glowElevation,
            shape = shape,
            ambientColor = colors.aura1,
            spotColor = colors.aura2,
        )
        .border(
            width = VolaIsland.rimWidth,
            brush = Brush.linearGradient(listOf(colors.aura1, colors.aura2)),
            shape = shape,
        )
}
