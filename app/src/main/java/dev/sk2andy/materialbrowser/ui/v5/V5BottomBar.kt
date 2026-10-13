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
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTint
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
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
 * of the current bar, so the page follows the finger exactly as it does today. While the page
 * scrolls the bar collapses to a 28 dp strip with the domain; a tap brings it back. Shown only with
 * the ui.v5 flag and outside the address editor (V9 replaces that).
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
    tint: V5SiteTint,
    collapsed: Boolean,
    onExpand: () -> Unit,
    onAddress: () -> Unit,
    onShield: () -> Unit,
    onReloadOrStop: () -> Unit,
    onTabs: () -> Unit,
    onBarPositioned: (boundsInRoot: Rect, topInWindowPx: Int) -> Unit,
    menuButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * A second row above the bar, such as the listening mini player (V19b). It is part of the bar:
     * the page ends above it, and it folds away with the bar while scrolling.
     */
    accessory: (@Composable () -> Unit)? = null,
) {
    val colors = V5Theme.colors
    // The site's color follows navigation on the effects spring; text and icons switch at once,
    // because white and dark text mixed half way would be gray with poor contrast.
    val container by animateColorAsState(tint.container, V5Motion.effects.spec(), label = "v5 bar tint")
    val barColors = V5BarColors(
        container = container,
        onTint = tint.onTint,
        onTintSecondary = tint.onTintSecondary,
        field = tint.onTint.copy(alpha = V5_FIELD_ALPHA).compositeOver(container),
    )
    // 0 = the 28 dp strip while scrolling, 1 = the full bar. Collapsing settles without overshoot,
    // coming back has a hint of it; with animations removed the bar only fades between the two.
    val expanded by animateFloatAsState(
        targetValue = if (collapsed) 0f else 1f,
        animationSpec = when {
            !V5Motion.animationsEnabled -> V5Motion.reducedFade()
            collapsed -> V5Motion.barCollapseSettle.spec()
            else -> V5Motion.barReveal.spec()
        },
        label = "v5 bar collapse",
    )
    val fullHeight = if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) V5Sizes.barLargeFont else V5Sizes.bar
    val showBarLabel = stringResource(R.string.compact_mode_show_bar)
    Column(
        modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(barColors.container)
            .then(
                // Collapsed, the strip and the navigation area under it are one target.
                if (collapsed) {
                    Modifier
                        .v5BarGestures(gestures, onTabs)
                        .clickable(role = Role.Button, onClickLabel = showBarLabel, onClick = onExpand)
                } else {
                    Modifier
                },
            ),
    ) {
        CompositionLocalProvider(LocalV5BarColors provides barColors, LocalContentColor provides barColors.onTint) {
            Box(Modifier.fillMaxWidth().height(V5Spacing.hairline).background(colors.edge))
            LoadingLine(visible = state.isLoading, progress = state.progress)
            // The bar's top, which the page ends at, is the accessory's when there is one.
            Column(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        onBarPositioned(
                            coordinates.boundsInRoot(),
                            coordinates.boundsInWindow().top.roundToInt(),
                        )
                    },
            ) {
                if (accessory != null && expanded > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, (placeable.height * expanded).roundToInt()) {
                                    placeable.place(0, 0)
                                }
                            }
                            .graphicsLayer { alpha = expanded },
                    ) {
                        accessory()
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(lerp(V5Sizes.barCollapsed, fullHeight, expanded))
                        .clipToBounds(),
                ) {
                    if (expanded > 0f) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(Alignment.Top, unbounded = true)
                                .heightIn(min = fullHeight)
                                .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2)
                                .graphicsLayer { alpha = expanded },
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
                    }
                    if (expanded < 1f) {
                        Text(
                            text = state.domain ?: stringResource(R.string.address_empty_hint),
                            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                            color = barColors.onTint,
                            maxLines = 1,
                            overflow = TextOverflow.MiddleEllipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(horizontal = V5Spacing.space6)
                                .graphicsLayer { alpha = 1f - expanded },
                        )
                    }
                }
            }
            // The strip runs on under the system navigation (edge to edge).
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/** Swipe along the bar switches tabs, swipe up opens the overview: the same on both bar heights. */
@Composable
private fun Modifier.v5BarGestures(gestures: V5BarGestures, onTabs: () -> Unit): Modifier {
    val tabDrag = rememberDraggableState(gestures.onTabDrag)
    return this
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
}

/** From this font scale the full bar grows to [V5Sizes.barLargeFont]. */
private const val LARGE_FONT_SCALE = 1.5f

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
    val bar = LocalV5BarColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) V5Motion.PRESS_SCALE_BUTTON else 1f,
        animationSpec = V5Motion.press.spec(),
        label = "v5 capsule press",
    )
    val openLabel = stringResource(R.string.address_empty_hint)
    Row(
        modifier
            .heightIn(min = V5Sizes.barCapsule)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(V5Theme.shapes.full)
            .background(LocalV5BarColors.current.field)
            .testTag(AddressBarTestTags.PrimaryField)
            .v5BarGestures(gestures, onTabs)
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
            color = if (state.domain != null) bar.onTint else bar.onTintSecondary,
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
                    tint = bar.onTint,
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
            .background(LocalV5BarColors.current.field),
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
                .background(LocalV5BarColors.current.onTint),
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

/** The bar's colors after the site tint (system-v5.md, section 1.3). */
@Immutable
private data class V5BarColors(
    val container: Color,
    val onTint: Color,
    val onTintSecondary: Color,
    val field: Color,
)

private val LocalV5BarColors = staticCompositionLocalOf {
    V5BarColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
}

/** onTint over the bar color for the capsule and the round buttons. */
private const val V5_FIELD_ALPHA = 0.12f
