package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * v4 design tokens from the Tokens board (docs/vola/design/canvas/W-Tokens.dc.html, vola4.css).
 * Colors live in VolaColors.kt and VolaSchemes.kt, type in VolaType.kt.
 */

/** Corner radii: --r-xs … --r-sheet in vola4.css. */
internal object VolaShapes {
    val extraSmallRadius = 8.dp
    val smallRadius = 12.dp
    val mediumRadius = 16.dp
    val largeRadius = 20.dp
    val cardRadius = 24.dp
    val extraLargeRadius = 28.dp
    val sheetRadius = 32.dp

    /** Cards and tiles. */
    val card = RoundedCornerShape(cardRadius)

    /** Bottom sheets: rounded on top, flush with the screen edge below. */
    val sheet = RoundedCornerShape(topStart = sheetRadius, topEnd = sheetRadius)

    /** Material shape roles on the v4 radii. */
    val material = Shapes(
        extraSmall = RoundedCornerShape(extraSmallRadius),
        small = RoundedCornerShape(smallRadius),
        medium = RoundedCornerShape(mediumRadius),
        large = RoundedCornerShape(largeRadius),
        extraLarge = RoundedCornerShape(extraLargeRadius),
    )
}

/** Spacing on a 4 dp grid. Each step notes its typical use from the Tokens board. */
internal object VolaSpacing {
    /** Inside an icon container. */
    val x1 = 4.dp

    /** Between chips. */
    val x2 = 8.dp

    /** Inside a list row. */
    val x3 = 12.dp

    /** Screen edges. */
    val x4 = 16.dp

    /** Inside a card. */
    val x5 = 20.dp

    /** Between groups. */
    val x6 = 24.dp

    /** Between sections. */
    val x8 = 32.dp

    /** Above a screen title. */
    val x12 = 48.dp
}

/** The framed shell («Рама»): the page card on the workspace aura (`.page-card` in vola4.css). */
internal object VolaFrame {
    /** Aura visible left and right of the page card. */
    val sideGutter = 6.dp

    /** Aura between the page card and the address bar below it. */
    val barGap = 8.dp

    /** Page card corners, a little tighter than the screen corners around them. */
    val pageRadius = 26.dp

    val pageShape = RoundedCornerShape(pageRadius)

    /** Hairline around the card: onSurface at this opacity. */
    const val OUTLINE_ALPHA = 0.06f
    val outlineWidth = 1.dp

    /** Soft drop shadow under the card: rgba(10, 20, 24, 0.08) 0 8px 24px. */
    val shadowColor = Color(0xFF0A1418)
    const val SHADOW_ALPHA = 0.08f
    val shadowOffsetY = 8.dp
    val shadowBlur = 24.dp
}

/** The address island (boards Main, Scrolled, V4B-Page). */
internal object VolaIsland {
    /** The scrolled-away bar: a small capsule with the lock and the site. */
    val compactHeight = 40.dp

    /** The aura rim of the island in the Air layout (`.halo` in vola4.css). */
    val rimWidth = 1.5.dp

    /** Glow of the aura under the island in the Air layout. */
    val glowElevation = 14.dp
}

/** Shadow depth: --e1 … --e3 in vola4.css. */
internal object VolaElevation {
    /** Cards and tiles. */
    val level1 = 2.dp

    /** The address island and menus. */
    val level2 = 6.dp

    /** Sheets and dialogs. */
    val level3 = 12.dp
}

/**
 * M3 Expressive springs. Pair workspace switches, swipe-to-close and long presses with haptic
 * feedback.
 */
internal object VolaMotion {
    const val FAST_DAMPING_RATIO = 0.9f
    const val FAST_STIFFNESS = 1400f
    const val STANDARD_DAMPING_RATIO = 0.9f
    const val STANDARD_STIFFNESS = 700f
    const val SLOW_DAMPING_RATIO = 0.9f
    const val SLOW_STIFFNESS = 300f
    const val EFFECTS_DAMPING_RATIO = 1f
    const val EFFECTS_STIFFNESS = 1600f

    /** Buttons, switches and chips. */
    fun <T> fast(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(FAST_DAMPING_RATIO, FAST_STIFFNESS, visibilityThreshold)

    /** The address bar, sheets and cards. */
    fun <T> standard(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(STANDARD_DAMPING_RATIO, STANDARD_STIFFNESS, visibilityThreshold)

    /** Workspace switches and Split View. */
    fun <T> slow(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(SLOW_DAMPING_RATIO, SLOW_STIFFNESS, visibilityThreshold)

    /** Color, opacity and blur: no overshoot. */
    fun <T> effects(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(EFFECTS_DAMPING_RATIO, EFFECTS_STIFFNESS, visibilityThreshold)
}
