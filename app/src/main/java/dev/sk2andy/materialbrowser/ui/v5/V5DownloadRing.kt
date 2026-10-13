package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import kotlin.math.roundToInt
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes

internal object V5DownloadRingTestTags {
    const val Ring = "v5_download_ring"
}

/** What is downloading (V22): how many files and, when every size is known, how far along. */
@Immutable
sealed interface V5DownloadRingState {
    val count: Int

    data class Progress(override val count: Int, val fraction: Float) : V5DownloadRingState

    /** Some size is unknown: the ring runs instead of filling. */
    data class Indeterminate(override val count: Int) : V5DownloadRingState
}

/**
 * The downloads ring around «•••» (V22, system-v5.md section 10): a thin ring that fills with the
 * progress of everything downloading, on a faint track. The button and its touch target do not
 * change. A size not yet known runs as an arc; without system animations it is only the track.
 */
@Composable
internal fun V5DownloadRing(state: V5DownloadRingState, color: Color, modifier: Modifier = Modifier) {
    val animated = V5Motion.animationsEnabled
    val fraction by animateFloatAsState(
        targetValue = (state as? V5DownloadRingState.Progress)?.fraction?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = if (animated) V5Motion.effects.spec() else V5Motion.reducedFade(),
        label = "v5 download ring",
    )
    val running = state is V5DownloadRingState.Indeterminate && animated
    val turn by if (running) {
        rememberInfiniteTransition(label = "v5 download ring turn").animateFloat(
            initialValue = 0f,
            targetValue = FULL_TURN,
            animationSpec = infiniteRepeatable(tween(TURN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "v5 download ring angle",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Canvas(modifier.testTag(V5DownloadRingTestTags.Ring)) {
        val stroke = V5Sizes.downloadRing.toPx()
        val inset = stroke / 2f
        val topLeft = Offset(inset, inset)
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = color.copy(alpha = TRACK_ALPHA),
            startAngle = 0f,
            sweepAngle = FULL_TURN,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke),
        )
        val sweep = when (state) {
            is V5DownloadRingState.Progress -> FULL_TURN * fraction
            is V5DownloadRingState.Indeterminate -> if (running) RUNNING_ARC else 0f
        }
        if (sweep > 0f) {
            drawArc(
                color = color,
                startAngle = START_ANGLE + turn,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** «2 files downloading · 45 %», for the menu row and TalkBack. */
@Composable
internal fun v5DownloadsSummary(state: V5DownloadRingState): String {
    val files = pluralStringResource(R.plurals.v5_downloads_active, state.count, state.count)
    return when (state) {
        is V5DownloadRingState.Progress -> {
            val percent = stringResource(R.string.settings_value_percent, (state.fraction * PERCENT).roundToInt())
            "$files · $percent"
        }
        is V5DownloadRingState.Indeterminate -> files
    }
}

private const val FULL_TURN = 360f
private const val PERCENT = 100

/** Progress starts at twelve o'clock, as a clock hand does. */
private const val START_ANGLE = -90f
private const val RUNNING_ARC = 90f
private const val TURN_MILLIS = 1_000
private const val TRACK_ALPHA = 0.24f
