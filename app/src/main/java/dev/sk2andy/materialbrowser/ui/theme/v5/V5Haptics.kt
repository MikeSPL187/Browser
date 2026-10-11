package dev.sk2andy.materialbrowser.ui.theme.v5

import android.annotation.SuppressLint
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/*
 * v5 haptics (system-v5.md, section 7.1). Compose's HapticFeedbackType has no threshold or drag
 * constants, so this calls View.performHapticFeedback with the API 34 constants and older fallbacks.
 * Frequent actions (tapping the address bar, ordinary buttons) give no feedback, as Apple's HIG
 * advises; the user's system vibration setting is always respected.
 */

enum class V5Haptic {
    /** A tab swipe, the overview gesture or pull to refresh reached its threshold. */
    ThresholdReached,

    /** The finger went back under the threshold. */
    ThresholdLeft,

    /** A new tab was opened past the last one; a tab was closed with a swipe. */
    Confirm,

    /** Paging through cards or profiles. */
    SegmentTick,

    ToggleOn,
    ToggleOff,

    /** A tab starts being dragged. */
    DragStart,

    /** Refused or failed. */
    Reject,

    /** The link preview opens. */
    LongPress,
}

/** The feedback constant for [haptic] on [sdk], or null where the spec wants none. */
@SuppressLint("InlinedApi") // every API 34 constant is behind the `api34` check
internal fun v5HapticConstant(haptic: V5Haptic, sdk: Int = Build.VERSION.SDK_INT): Int? {
    val api34 = sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    return when (haptic) {
        V5Haptic.ThresholdReached ->
            if (api34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.CLOCK_TICK
        V5Haptic.ThresholdLeft -> if (api34) HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE else null
        V5Haptic.Confirm -> HapticFeedbackConstants.CONFIRM
        V5Haptic.SegmentTick ->
            if (api34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        V5Haptic.ToggleOn -> if (api34) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CONTEXT_CLICK
        V5Haptic.ToggleOff -> if (api34) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CONTEXT_CLICK
        V5Haptic.DragStart -> if (api34) HapticFeedbackConstants.DRAG_START else HapticFeedbackConstants.LONG_PRESS
        V5Haptic.Reject -> HapticFeedbackConstants.REJECT
        V5Haptic.LongPress -> HapticFeedbackConstants.LONG_PRESS
    }
}

/** Plays [haptic] on this view; nothing where the spec wants no feedback on this Android version. */
fun View.performV5Haptic(haptic: V5Haptic) {
    v5HapticConstant(haptic)?.let { performHapticFeedback(it) }
}
