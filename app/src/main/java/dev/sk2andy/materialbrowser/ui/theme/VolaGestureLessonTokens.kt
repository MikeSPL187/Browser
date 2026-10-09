package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The gesture lesson (in the first run, after an update, or from settings): a pretend page to
 * swipe, a pointer that shows the way, and confetti at the end. Its colors come from
 * MaterialTheme, so it follows the palette, the workspace accent, dark mode and private tabs the
 * way the first-run screens do ([VolaFirstRunTokens]).
 */
internal object VolaGestureLessonTokens {
    val screenPadding = PaddingValues(horizontal = 24.dp, vertical = VolaSpacing.x5)
    val welcomePadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
    val celebrationPadding = PaddingValues(horizontal = 28.dp, vertical = 32.dp)
    val skipPadding = PaddingValues(top = 6.dp, end = VolaSpacing.x2)

    /** The title row, the progress line and the step's title and description above the card. */
    val progressGap = 10.dp
    val progressHeight = 6.dp
    const val PROGRESS_TRACK_ALPHA = 0.16f
    val headerGap = 28.dp
    val copyGap = VolaSpacing.x2
    val practiceGap = 22.dp

    /** Under the card: the direction badge, then the two hints. */
    val badgeGap = VolaSpacing.x3
    val badgeShadow = 5.dp
    val badgePadding = PaddingValues(horizontal = 18.dp, vertical = 7.dp)
    val hintGap = VolaSpacing.x1
    val footerGap = 6.dp

    /**
     * The card the gestures are practised on takes the room the screen has left, up to its
     * largest size; at its smallest the pretend page still fits with its address bar.
     */
    val practiceMaxHeight = 390.dp
    val practiceMinHeight = 240.dp

    /** Below this height a screen on its side puts the copy beside the card, not above it. */
    val stackedMinHeight = 560.dp
    val besideGap = 24.dp
    val practiceShape = RoundedCornerShape(32.dp)
    val practiceElevation = VolaSpacing.x2
    const val PRACTICE_ALPHA = 0.94f
    val practiceInset = VolaSpacing.x5
    val practiceInnerShape = RoundedCornerShape(24.dp)

    /** The pretend page and its address bar, the target of the first two gestures. */
    val pageShape = RoundedCornerShape(24.dp)
    val pagePadding = 18.dp
    val pageTitleHeight = 18.dp
    val pageTitleShape = RoundedCornerShape(10.dp)
    const val PAGE_TITLE_ALPHA = 0.8f
    const val PAGE_LINES = 4
    val pageLineHeight = 10.dp
    val pageLineShape = RoundedCornerShape(VolaSpacing.x2)
    const val PAGE_LINE_ALPHA = 0.12f
    val addressBarHeight = 48.dp
    val addressBarShape = RoundedCornerShape(24.dp)
    val addressBarElevation = 6.dp

    /** The tab card shown in the overview and swiped away in the last step. */
    val tabCardWidth = 190.dp
    val closeTabCardWidth = 210.dp
    val tabCardHeight = 260.dp
    val tabCardShape = RoundedCornerShape(26.dp)
    val tabCardElevation = 10.dp
    val tabCardPadding = VolaSpacing.x4
    val tabCardGap = VolaSpacing.x3
    val tabCardImageShape = RoundedCornerShape(18.dp)

    /** The closing card fades as it goes up, never below this. */
    const val CLOSE_CARD_MIN_ALPHA = 0.25f

    /** Distances that complete a step before the card has been measured. */
    val switchFallbackThreshold = 72.dp
    val closeFallbackThreshold = 44.dp

    /** The pointer that shows the gesture: a dashed trail, a halo and a ringed dot. */
    const val POINTER_LOOP_MILLIS = 1_900
    val pointerTrailWidth = 3.dp
    val pointerDash = 10.dp
    val pointerDashGap = 7.dp
    val pointerDashTravel = 24.dp
    val pointerHaloRadius = 25.dp
    val pointerHaloGrowth = 4.dp
    val pointerRadius = 13.dp
    val pointerRing = 3.dp
    val pointerDot = 4.dp
    const val POINTER_TRAIL_ALPHA = 0.38f
    const val POINTER_HALO_ALPHA = 0.16f

    /** The step's title and description cross-fade. */
    const val COPY_FADE_IN_MILLIS = 180
    const val COPY_FADE_OUT_MILLIS = 100

    /** The lesson's own welcome: the floating mark, the list of gestures and «Learn the gestures». */
    val welcomeHeroGap = VolaSpacing.x5
    val welcomeTextGap = VolaSpacing.x3
    val welcomeSectionGap = 28.dp
    val welcomeCardShape = RoundedCornerShape(28.dp)
    val welcomeCardElevation = VolaSpacing.x2
    const val WELCOME_CARD_ALPHA = 0.94f
    val welcomeCardPadding = PaddingValues(horizontal = VolaSpacing.x5, vertical = 18.dp)
    val welcomeCardTitleGap = 14.dp
    val welcomeButtonGap = VolaSpacing.x3
    val rowPadding = 6.dp
    val rowIconSize = 38.dp
    val rowGap = VolaSpacing.x3

    /** Arrow glyphs sit low in their line; lifted to look centred in the circle. */
    val rowSymbolLift = 2.dp

    val heroSize = 174.dp
    val heroTileOffsetX = VolaSpacing.x2
    val heroTileOffsetY = 26.dp
    val heroTileSize = 70.dp
    val heroTileShape = RoundedCornerShape(24.dp)
    const val HERO_TILE_ROTATION = -14f
    val heroDotOffsetX = (-4).dp
    val heroDotOffsetY = (-18).dp
    val heroDotSize = 62.dp
    const val HERO_SHAPE_ALPHA = 0.8f
    val heroDiscSize = 126.dp
    val heroDiscElevation = 10.dp
    val heroDiscShadow = 18.dp
    val heroDiscInset = VolaSpacing.x2
    val heroMarkSize = 94.dp
    const val HERO_GLOW_START_ALPHA = 0.18f
    const val HERO_GLOW_END_ALPHA = 0.24f

    /** The mark floats up and down by this many pixels, tilting a quarter as much. */
    const val HERO_FLOAT_PX = 6f
    const val HERO_FLOAT_TILT = 0.25f
    const val HERO_FLOAT_MILLIS = 1_800

    /** The end: a burst of confetti, then the check and «Start browsing», then a fade out. */
    const val CONFETTI_BURST_MILLIS = 1_250
    const val CONFETTI_STREAM_MILLIS = 2_200
    val confettiDotRadius = 5.dp
    val confettiStripWidth = 6.dp
    val confettiStripLength = 16.dp
    const val CELEBRATION_CONTENT_DELAY_MILLIS = 620L
    const val CELEBRATION_ENTER_MILLIS = 720
    const val CELEBRATION_ENTER_SCALE = 0.92f
    const val CELEBRATION_EXIT_MILLIS = 720
    val celebrationBadgeSize = 132.dp
    val celebrationBadgeShadow = 18.dp
    val celebrationIconSize = 72.dp
    val celebrationIconGap = 30.dp
    val celebrationTextGap = 14.dp
    val celebrationButtonGap = 34.dp
}
