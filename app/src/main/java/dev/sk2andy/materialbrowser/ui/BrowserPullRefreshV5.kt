package dev.sk2andy.materialbrowser.ui

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTints
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberUiV5Flag
import dev.sk2andy.materialbrowser.ui.v5.V5PullRefreshIndicator
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pull to refresh under v5 (V16, system-v5.md section 7): where the page and the indicator are. */
internal object V5PullRefreshRules {
    /**
     * How far the page moves for a finger [dragPx] down: 0.55 of the finger up to the threshold,
     * and past it 0.55 of that again, so the page gets heavier rather than stopping.
     */
    fun pageOffset(dragPx: Float, thresholdPx: Float): Float {
        if (!dragPx.isFinite() || dragPx <= 0f || !(thresholdPx > 0f)) return 0f
        val pulled = dragPx * V5Motion.PULL_RESISTANCE
        if (pulled <= thresholdPx) return pulled
        return thresholdPx + (pulled - thresholdPx) * V5Motion.PULL_RESISTANCE
    }

    fun armed(offsetPx: Float, thresholdPx: Float): Boolean = thresholdPx > 0f && offsetPx >= thresholdPx

    /** How much of the indicator's shape has formed: all of it at the threshold. */
    fun progress(offsetPx: Float, thresholdPx: Float): Float =
        if (thresholdPx > 0f) (offsetPx / thresholdPx).coerceIn(0f, 1f) else 0f

    /** Where the page waits while it reloads: the threshold, lower if the indicator needs the room. */
    fun holdOffset(thresholdPx: Float, topInsetPx: Float, indicatorPx: Float, marginPx: Float): Float =
        max(thresholdPx, topInsetPx + indicatorPx + 2 * marginPx)

    /** The indicator's top edge: in the middle of the gap between the status bar and the page. */
    fun indicatorTop(offsetPx: Float, topInsetPx: Float, indicatorPx: Float): Float =
        (topInsetPx + offsetPx - indicatorPx) / 2

    /** A reload asked for by a pull that never starts loading lets the page go after this. */
    const val START_GRACE_MILLIS = 2_000L
}

/**
 * The pull, its spring and its haptics. The page is moved by [V5PullGestureLayout]; without system
 * animations it stays put during the gesture and the indicator only fades in (system-v5.md 7).
 */
@Stable
internal class V5PullRefreshState(private val scope: CoroutineScope, private val view: View) {
    var offsetPx by mutableFloatStateOf(0f)
        private set
    var refreshing by mutableStateOf(false)
        private set

    internal var thresholdPx = 0f
    internal var holdPx = 0f
    internal var animated = true
    private var layout: V5PullGestureLayout? = null
    private var onRefresh: () -> Boolean = { false }
    private var armed = false
    private var loadingSeen = false
    private var settle: Job? = null
    private var watchdog: Job? = null

    private val listener = object : V5PullGestureLayout.Listener {
        override fun onPull(distancePx: Float) = pull(distancePx)

        override fun onRelease() = release()

        override fun onCancel() {
            armed = false
            settleTo(if (refreshing) holdPx else 0f)
        }
    }

    fun bind(
        layout: V5PullGestureLayout,
        enabled: Boolean,
        topInsetPx: Int,
        canScrollUp: () -> Boolean,
        onRefresh: () -> Boolean,
    ) {
        if (this.layout !== layout) {
            this.layout?.listener = null
            this.layout = layout
            layout.listener = listener
            layout.pageOffsetPx = offsetPx
        }
        layout.pullEnabled = enabled
        layout.topInsetPx = topInsetPx
        layout.canContentScrollUp = canScrollUp
        this.onRefresh = onRefresh
    }

    fun unbind() {
        layout?.listener = null
        layout = null
    }

    /** The selected tab is loading or not: a reload asked for by the pull ends with its load. */
    fun loading(isLoading: Boolean) {
        if (isLoading) {
            loadingSeen = true
        } else if (refreshing && loadingSeen) {
            finish()
        }
    }

    /** Another tab: no pull carries over. */
    fun reset() {
        settle?.cancel()
        watchdog?.cancel()
        armed = false
        refreshing = false
        move(0f)
    }

    private fun pull(distancePx: Float) {
        settle?.cancel()
        val target = V5PullRefreshRules.pageOffset(distancePx, thresholdPx)
        val nowArmed = V5PullRefreshRules.armed(target, thresholdPx)
        if (nowArmed != armed) view.performV5Haptic(if (nowArmed) V5Haptic.ThresholdReached else V5Haptic.ThresholdLeft)
        armed = nowArmed
        if (animated) move(target)
    }

    private fun release() {
        val start = armed && onRefresh()
        armed = false
        if (start) {
            refreshing = true
            loadingSeen = false
            watchdog?.cancel()
            watchdog = scope.launch {
                delay(V5PullRefreshRules.START_GRACE_MILLIS)
                if (!loadingSeen) finish()
            }
        }
        settleTo(if (start) holdPx else 0f)
    }

    private fun finish() {
        watchdog?.cancel()
        refreshing = false
        settleTo(0f)
    }

    private fun settleTo(target: Float) {
        settle?.cancel()
        if (!animated) {
            move(0f)
            return
        }
        val from = offsetPx
        settle = scope.launch {
            animate(from, target, animationSpec = V5Motion.pullRefreshReturn.spec()) { value, _ -> move(value) }
        }
    }

    private fun move(value: Float) {
        offsetPx = value
        layout?.pageOffsetPx = value
    }
}

/** Pull to refresh is v5's where v5 draws the browser: the flag is on and the window is narrow. */
@Composable
internal fun rememberPullRefreshV5(): Boolean {
    val flag by rememberUiV5Flag()
    return flag && !AddressBarWideLayoutRules.usesTabStrip(LocalConfiguration.current.screenWidthDp.toFloat())
}

@Composable
internal fun rememberV5PullRefresh(selectedTabId: String, isLoading: Boolean, topInsetPx: Int): V5PullRefreshState {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val state = remember { V5PullRefreshState(scope, view) }
    val animated = V5Motion.animationsEnabled
    SideEffect {
        with(density) {
            state.thresholdPx = V5Motion.PULL_THRESHOLD_DP.dp.toPx()
            state.holdPx = V5PullRefreshRules.holdOffset(
                thresholdPx = state.thresholdPx,
                topInsetPx = topInsetPx.toFloat(),
                indicatorPx = V5Sizes.pullRefreshIndicator.toPx(),
                marginPx = V5Spacing.space2.toPx(),
            )
        }
        state.animated = animated
    }
    LaunchedEffect(selectedTabId) { state.reset() }
    LaunchedEffect(selectedTabId, isLoading) { state.loading(isLoading) }
    return state
}

/**
 * What the pull reveals above the page: the page's own color (its `theme-color` or top
 * background, as Safari extends the page) and the indicator in the middle of the gap.
 */
@Composable
internal fun V5PullRefreshOverlay(
    state: V5PullRefreshState,
    siteColorRgb: Int?,
    privateBrowsing: Boolean,
    topInsetPx: Int,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE
    VolaV5Theme(dark = dark, privateBrowsing = privateBrowsing) {
        val colors = V5Theme.colors
        val gap = siteColorRgb?.let { Color(it).copy(alpha = 1f) } ?: colors.background
        val ink = if (siteColorRgb != null) {
            V5SiteTints.of(siteColorRgb, colors.background, colors.label).onTint
        } else {
            colors.accent
        }
        val animated = V5Motion.animationsEnabled
        val shown by remember(state) { derivedStateOf { state.refreshing || state.offsetPx > 0f } }
        val density = LocalDensity.current
        val indicatorPx = with(density) { V5Sizes.pullRefreshIndicator.toPx() }
        val restingTopPx = topInsetPx + with(density) { V5Spacing.space3.toPx() }
        Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            Spacer(
                Modifier.fillMaxSize().drawBehind {
                    val height = state.offsetPx
                    if (height > 0f) drawRect(gap, size = Size(size.width, height))
                },
            )
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(if (animated) V5Motion.effects.spec() else V5Motion.reducedFade()),
                exit = fadeOut(if (animated) V5Motion.effects.spec() else V5Motion.reducedFade()),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                val pullProgress = { V5PullRefreshRules.progress(state.offsetPx, state.thresholdPx) }
                V5PullRefreshIndicator(
                    progress = pullProgress.takeUnless { state.refreshing },
                    color = ink,
                    modifier = Modifier
                        .offset {
                            val top = if (animated) {
                                V5PullRefreshRules.indicatorTop(state.offsetPx, topInsetPx.toFloat(), indicatorPx)
                            } else {
                                restingTopPx
                            }
                            IntOffset(0, top.roundToInt())
                        }
                        .graphicsLayer {
                            alpha = if (state.refreshing || !animated) {
                                1f
                            } else {
                                pullProgress()
                            }
                        },
                )
            }
        }
    }
}

/** Below this background luminance the browser's theme is dark. */
private const val DARK_LUMINANCE = 0.5f
