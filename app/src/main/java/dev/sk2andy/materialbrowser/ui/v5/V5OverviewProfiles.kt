package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** Where the active profile stands among the profiles the overview pages through. */
@Immutable
data class V5ProfileDots(val count: Int, val active: Int)

/**
 * Paging through profiles in the overview (system-v5.md, «Обзор вкладок»; canvas «Overview»): dots on
 * the profile chip, and a sideways swipe on the bottom bar to the next or previous profile. The
 * swipe belongs to the bar, as in Safari, because a card swiped sideways closes.
 */
object V5ProfileDotsRules {
    /** More than five profiles show a window of five dots around the active one. */
    const val MAX_DOTS = 5

    /** The profiles whose dots show, the active one among them and as central as the ends allow. */
    fun window(dots: V5ProfileDots): IntRange {
        if (dots.count <= MAX_DOTS) return 0 until dots.count
        val start = (dots.active - MAX_DOTS / 2).coerceIn(0, dots.count - MAX_DOTS)
        return start until start + MAX_DOTS
    }

    /**
     * The step a swipe on the bar makes: +1 to the next profile (a swipe toward the start, as a page
     * turns), −1 to the previous one, 0 when it stays. It commits past 30 % of the bar's [widthPx] or
     * when flung at 1000 dp/s, as a tab swipe does.
     */
    fun step(offsetPx: Float, velocityPxPerSecond: Float, widthPx: Float, density: Float, rtl: Boolean): Int {
        if (widthPx <= 0f || offsetPx == 0f) return 0
        val far = abs(offsetPx) >= widthPx * V5Motion.TAB_SWIPE_COMMIT_FRACTION
        val flung = abs(velocityPxPerSecond) >= V5Motion.TAB_SWIPE_COMMIT_VELOCITY_DP * density &&
            sign(velocityPxPerSecond) == sign(offsetPx)
        if (!far && !flung) return 0
        val towardStart = if (rtl) offsetPx > 0f else offsetPx < 0f
        return if (towardStart) 1 else -1
    }

    /** The profile a [step] lands on, or null past the first or the last: the list does not wrap. */
    fun target(dots: V5ProfileDots, step: Int): Int? =
        (dots.active + step).takeIf { step != 0 && it in 0 until dots.count }
}

/** The dots on the chip; TalkBack hears the profile's name and the chip's actions instead. */
@Composable
internal fun V5ProfileDotsRow(dots: V5ProfileDots, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    Row(modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1)) {
        V5ProfileDotsRules.window(dots).forEach { index ->
            Box(
                Modifier
                    .size(V5Sizes.profileDot)
                    .clip(V5Theme.shapes.full)
                    .background(if (index == dots.active) colors.accent else colors.labelTertiary),
            )
        }
    }
}

/** A sideways swipe on what this modifies steps through the profiles; a tick marks each step. */
@Composable
internal fun Modifier.v5ProfileSwipe(dots: V5ProfileDots?, onStep: ((Int) -> Unit)?): Modifier {
    if (dots == null || dots.count < 2 || onStep == null) return this
    val view = LocalView.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentDots by rememberUpdatedState(dots)
    val currentStep by rememberUpdatedState(onStep)
    return pointerInput(rtl) {
        val tracker = VelocityTracker()
        var offset = 0f
        detectHorizontalDragGestures(
            onDragStart = {
                offset = 0f
                tracker.resetTracking()
            },
            onDragEnd = {
                val step = V5ProfileDotsRules.step(
                    offsetPx = offset,
                    velocityPxPerSecond = tracker.calculateVelocity().x,
                    widthPx = size.width.toFloat(),
                    density = density,
                    rtl = rtl,
                )
                V5ProfileDotsRules.target(currentDots, step)?.let {
                    view.performV5Haptic(V5Haptic.SegmentTick)
                    currentStep(step)
                }
            },
            onDragCancel = { offset = 0f },
        ) { change, dragAmount ->
            change.consume()
            offset += dragAmount
            tracker.addPosition(change.uptimeMillis, change.position)
        }
    }
}

/**
 * The grid of the profile paged to comes in from the side it was paged from on the profile spring
 * (system-v5.md 5, `profileSwitch`); without animations it only fades in.
 */
@Composable
internal fun Modifier.v5ProfileSlide(active: Int?): Modifier {
    val animated = V5Motion.animationsEnabled
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shift = remember { Animatable(0f) }
    val shown = remember { Animatable(1f) }
    val last = remember { LastProfile(active) }
    LaunchedEffect(active) {
        val previous = last.index
        last.index = active
        if (previous == null || active == null || previous == active) return@LaunchedEffect
        if (animated) {
            val from = if (active > previous) 1f else -1f
            shift.snapTo(from * V5Motion.PROFILE_SWITCH_SLIDE_FRACTION)
            launch { shift.animateTo(0f, V5Motion.profileSwitch.spec()) }
        }
        shown.snapTo(0f)
        shown.animateTo(1f, if (animated) V5Motion.effects.spec() else V5Motion.reducedFade())
    }
    return graphicsLayer {
        translationX = shift.value * size.width * (if (rtl) -1f else 1f)
        alpha = shown.value
    }
}

/** The profile the grid showed last, kept out of snapshot state: the effect reads and writes it. */
private class LastProfile(var index: Int?)
