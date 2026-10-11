package dev.sk2andy.materialbrowser.ui.theme.v5

import android.animation.ValueAnimator
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/*
 * v5 motion (system-v5.md, section 7; plan-v5.md, section 5). Every moment is a spring, so a new
 * gesture or a released finger carries on from the current value and velocity. With system
 * animations removed (animator duration scale 0) motion collapses to a 150 ms opacity change:
 * callers check [V5Motion.animationsEnabled] and use [V5Motion.reducedFade] instead of moving content.
 */

/** A spring as the spec writes it: damping ratio / stiffness, mass 1. */
@Immutable
data class V5Spring(val dampingRatio: Float, val stiffness: Float) {
    fun <T> spec(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        spring(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = visibilityThreshold)
}

object V5Motion {
    /** Tap on the bar: the capsule stretches into the field over the keyboard. */
    val addressMorph = V5Spring(0.8f, 380f)

    /** The bar collapses under the finger while scrolling and settles after release. */
    val barCollapseSettle = V5Spring(Spring.DampingRatioNoBouncy, 600f)

    /** The collapsed bar comes back: a hint of overshoot, livelier than the settle. */
    val barReveal = V5Spring(0.9f, 700f)

    /** Page and bar follow the finger 1 : 1; the neighbour tab slides in as a snapshot. */
    val tabSwipe = V5Spring(0.85f, 500f)

    /** Swipe up from the bar: the page shrinks into its card, the others appear from 0.92. */
    val overviewEnter = V5Spring(0.82f, 300f)

    /** Long press on a link: the card lifts out of the touch point. */
    val glance = V5Spring(0.75f, 400f)

    /** Pull to refresh springs back. */
    val pullRefreshReturn = V5Spring(Spring.DampingRatioNoBouncy, 400f)

    /** Switching profile in the overview: the grid slides aside and the accent changes with it. */
    val profileSwitch = V5Spring(Spring.DampingRatioNoBouncy, 632f)

    /** A sheet (the menu, settings layers) rises from the bottom and settles without a bounce. */
    val sheet = V5Spring(Spring.DampingRatioNoBouncy, 500f)

    /** Color and opacity: the site color of the bar, the accent, dimming. */
    val effects = V5Spring(Spring.DampingRatioNoBouncy, 1600f)
    val effectsFast = V5Spring(Spring.DampingRatioNoBouncy, 3800f)

    /** Press feedback on buttons, cards and rows. */
    val press = V5Spring(Spring.DampingRatioNoBouncy, 1500f)
    const val PRESS_SCALE_BUTTON = 0.97f
    const val PRESS_SCALE_CARD = 0.985f

    // Gesture constants of the moments above.
    /** A tab swipe commits past 30 % of the width or 1000 dp/s. */
    const val TAB_SWIPE_COMMIT_FRACTION = 0.30f
    const val TAB_SWIPE_COMMIT_VELOCITY_DP = 1000f
    const val OVERVIEW_CARD_START_SCALE = 0.92f
    /** A card swiped sideways in the overview closes past 40 % of its width or 1000 dp/s. */
    const val CARD_CLOSE_COMMIT_FRACTION = 0.40f
    /** How transparent a card gets at a full width of travel while it is swiped. */
    const val CARD_CLOSE_FADE = 0.6f
    const val OVERVIEW_CARD_STAGGER_MILLIS = 15
    const val GLANCE_PRESS_MILLIS = 400
    const val GLANCE_LIFT_SCALE = 1.03f
    const val GLANCE_PAGE_SCALE = 0.97f
    const val GLANCE_ARC_MAX_DP = 20f
    /** A sheet dragged down closes past 25 % of its height or 1000 dp/s. */
    const val SHEET_DISMISS_FRACTION = 0.25f
    const val SHEET_DISMISS_VELOCITY_DP = 1000f
    /** Dragged up past its rest, a sheet moves at this fraction of the finger. */
    const val SHEET_OVERDRAG_RESISTANCE = 0.2f
    /** Predictive back lowers the sheet by up to this much before it closes. */
    const val SHEET_BACK_TRAVEL_DP = 48f
    /** A sheet is never taller than this share of the window. */
    const val SHEET_MAX_HEIGHT_FRACTION = 0.92f
    const val PULL_RESISTANCE = 0.55f
    const val PULL_THRESHOLD_DP = 80f

    /** The whole of motion when system animations are removed. */
    const val REDUCED_FADE_MILLIS = 150

    fun <T> reducedFade(): FiniteAnimationSpec<T> = tween(durationMillis = REDUCED_FADE_MILLIS)

    /** False when the user removed animations (Settings → Accessibility → Remove animations). */
    val animationsEnabled: Boolean get() = ValueAnimator.areAnimatorsEnabled()
}
