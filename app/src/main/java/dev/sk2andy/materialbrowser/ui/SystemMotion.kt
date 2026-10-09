package dev.sk2andy.materialbrowser.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the system plays animations. Off when the user turned on «Remove animations» in
 * accessibility settings or set the animator duration scale to off: a looping decoration then
 * stands still instead of moving forever.
 */
@Composable
internal fun rememberSystemAnimationsOn(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    } || EXPERIMENT_ANIMATIONS_ALWAYS_ON
}

/** Temporary: tells whether the lesson standing still is what stops a tap on Skip in CI. */
private const val EXPERIMENT_ANIMATIONS_ALWAYS_ON = true
