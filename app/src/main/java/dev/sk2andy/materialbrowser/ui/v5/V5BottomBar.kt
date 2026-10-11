package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.SiteConnectionKind
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.AddressBarTabCounterButton
import dev.sk2andy.materialbrowser.ui.AddressBarTestTags
import dev.sk2andy.materialbrowser.ui.PermissionRadarBadge
import dev.sk2andy.materialbrowser.ui.addressBarVerticalGesture
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import kotlin.math.roundToInt

/*
 * The v5 bottom bar (plan-v5.md V7, system-v5.md section 5.1): a 64 dp strip that runs on under
 * the system navigation, a 48 dp address capsule [shield · domain · reload] and two 48 dp round
 * buttons, tabs and the menu. There is no Back button: the system back gesture goes back.
 * A swipe along the capsule switches tabs, a swipe up opens the overview; both reuse the gestures
 * of the current bar, so the page follows the finger exactly as it does today. Shown only with the
 * ui.v5 flag, outside the address editor (V9 replaces that) and while the bar is not collapsed.
 * The strip is neutral `chrome` until V8 tints it with the site's color.
 */

/** What the bar shows; everything else arrives as callbacks. */
@Immutable
internal data class V5BarState(
    val tabId: String,
    /** The domain without scheme and `www.`, or null on a new tab (the hint shows instead). */
    val domain: String?,
    val isLoading: Boolean,
    val progress: Float,
    val tabCount: Int,
    val siteAvailable: Boolean,
    val permissionActivity: Boolean,
    val connectionKind: SiteConnectionKind,
    val blockedCount: Int,
)

/** The gestures the bar shares with the current one. */
internal class V5BarGestures(
    val tabSwipeEnabled: Boolean,
    val onTabDrag: (Float) -> Unit,
    val onTabDragStopped: suspend (Float) -> Unit,
    val overviewEnabled: Boolean,
    val overviewProgress: FloatState,
    val onOverviewProgress: (Float) -> Unit,
    val onOverviewStarted: () -> Unit,
    val onOverviewCancelled: () -> Unit,
)

@Composable
internal fun BoxScope.V5BottomBar(
    state: V5BarState,
    gestures: V5BarGestures,
    onAddress: () -> Unit,
    onShield: () -> Unit,
    onReloadOrStop: () -> Unit,
    onTabs: () -> Unit,
    onBarPositioned: (boundsInRoot: Rect, topInWindowPx: Int) -> Unit,
    menuButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Column(
        modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(colors.chrome),
    ) {
        Box(Modifier.fillMaxWidth().height(V5Spacing.hairline).background(colors.edge))
        LoadingLine(visible = state.isLoading, progress = state.progress)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = V5Sizes.bar)
                .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2)
                .onGloballyPositioned { coordinates ->
                    onBarPositioned(
                        coordinates.boundsInRoot(),
                        coordinates.boundsInWindow().top.roundToInt(),
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddressCapsule(
                state = state,
                gestures = gestures,
                onAddress = onAddress,
                onShield = onShield,
                onReloadOrStop = onReloadOrStop,
                onTabs = onTabs,
                modifier = Modifier.weight(1f),
            )
            RoundButton { AddressBarTabCounterButton(tabCount = state.tabCount, onClick = onTabs) }
            RoundButton { menuButton() }
        }
        // The strip runs on under the system navigation (edge to edge).
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun AddressCapsule(
    state: V5BarState,
    gestures: V5BarGestures,
    onAddress: () -> Unit,
    onShield: () -> Unit,
    onReloadOrStop: () -> Unit,
    onTabs: () -> Unit,
    modifier: Modifier,
) {
    val colors = V5Theme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) V5Motion.PRESS_SCALE_BUTTON else 1f,
        animationSpec = V5Motion.press.spec(),
        label = "v5 capsule press",
    )
    val tabDrag = rememberDraggableState(gestures.onTabDrag)
    val openLabel = stringResource(R.string.address_empty_hint)
    Row(
        modifier
            .heightIn(min = V5Sizes.barCapsule)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(V5Theme.shapes.full)
            .background(colors.fill)
            .testTag(AddressBarTestTags.PrimaryField)
            .addressBarVerticalGesture(
                enabled = gestures.overviewEnabled,
                initialProgress = gestures.overviewProgress,
                onProgress = gestures.onOverviewProgress,
                onStarted = gestures.onOverviewStarted,
                onCancelled = gestures.onOverviewCancelled,
                onSwipeUp = onTabs,
            )
            .draggable(
                state = tabDrag,
                orientation = Orientation.Horizontal,
                enabled = gestures.tabSwipeEnabled,
                onDragStopped = { velocity -> gestures.onTabDragStopped(velocity) },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = openLabel,
                onClick = onAddress,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(V5Sizes.barCapsule), contentAlignment = Alignment.Center) {
            PermissionRadarBadge(
                siteAvailable = state.siteAvailable,
                activityVisible = state.permissionActivity,
                connectionKind = state.connectionKind,
                blockedCount = state.blockedCount,
                onClick = onShield,
                tabId = state.tabId,
            )
        }
        Text(
            text = state.domain ?: openLabel,
            style = if (state.domain != null) V5Theme.type.headline else V5Theme.type.body,
            color = if (state.domain != null) colors.label else colors.labelTertiary,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        if (state.domain != null) {
            IconButton(onClick = onReloadOrStop) {
                Icon(
                    imageVector = if (state.isLoading) VolaIcons.Close else VolaIcons.Refresh,
                    contentDescription = stringResource(
                        if (state.isLoading) R.string.action_stop_loading else R.string.action_reload,
                    ),
                    tint = colors.label,
                )
            }
        } else {
            Spacer(Modifier.width(V5Sizes.barCapsule))
        }
    }
}

@Composable
private fun RoundButton(content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(V5Sizes.barCapsule)
            .clip(V5Theme.shapes.full)
            .background(V5Theme.colors.fill),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The 2 dp loading line along the top edge of the strip. */
@Composable
private fun LoadingLine(visible: Boolean, progress: Float) {
    val shown by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = V5Motion.effectsFast.spec(),
        label = "v5 loading line",
    )
    val value by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = V5Motion.effects.spec(),
        label = "v5 loading progress",
    )
    Box(Modifier.fillMaxWidth().height(V5Sizes.barProgress)) {
        Box(
            Modifier
                .fillMaxWidth(value)
                .height(V5Sizes.barProgress)
                .graphicsLayer { alpha = shown }
                .background(V5Theme.colors.label),
        )
    }
}

/** The domain the bar shows: no scheme, no `www.`, no path; null for a page without a host. */
internal fun v5BarDomain(url: String): String? {
    val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
    if (afterScheme.isEmpty()) return null
    val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        .substringAfterLast('@')
    val host = if (authority.startsWith("[")) {
        authority.substringBefore(']') + "]"
    } else {
        authority.substringBefore(':')
    }
    return host.lowercase().removePrefix("www.").takeIf { it.isNotBlank() && it != "[]" }
}
