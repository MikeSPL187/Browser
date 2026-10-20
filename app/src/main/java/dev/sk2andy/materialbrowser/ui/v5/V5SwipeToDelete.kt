package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A list row a swipe to the left deletes (the library's rows, language packs): past 40 % of the
 * row or on a fling to the left it goes, as a card in the overview closes (СС-5); a shorter, slower
 * swipe springs back. The red under the
 * row shows what the swipe does. Without system animations the row jumps instead of springing.
 * A swipe is not the only way: the [content] row offers «Delete» to TalkBack itself. A row that
 * [springsBack] asks before it goes: it returns to its place under the question, and leaves the list
 * only when the deletion is confirmed. A row still here a moment after its deletion (it was refused
 * or undone) springs back too, rather than stay pushed off the screen.
 */
@Composable
internal fun V5SwipeToDelete(
    key: Any,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    springsBack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = V5Theme.colors
    val offset = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current.density
    Box(modifier.onSizeChanged { widthPx = it.width.toFloat() }) {
        if (offset.value < 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(horizontal = V5Spacing.marginPhone)
                    .clip(V5Theme.shapes.s)
                    .background(colors.danger)
                    .padding(horizontal = V5Spacing.space4),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(VolaIcons.Delete, contentDescription = null, tint = colors.onAccentFill)
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-widthPx, 0f)) }
                    },
                    onDragStopped = { velocity ->
                        val deletes = V5SwipeToDeleteRules.deletes(offset.value, velocity, widthPx, density)
                        val target = if (deletes) -widthPx else 0f
                        if (V5Motion.animationsEnabled) {
                            offset.animateTo(target, V5Motion.tabSwipe.spec())
                        } else {
                            offset.snapTo(target)
                        }
                        if (target < 0f) {
                            onDelete()
                            // A row that left the list took this coroutine with it; one still here comes back.
                            if (!springsBack) delay(V5SwipeToDeleteRules.RETURN_AFTER_MILLIS)
                            if (V5Motion.animationsEnabled) offset.animateTo(0f, V5Motion.tabSwipe.spec()) else offset.snapTo(0f)
                        }
                    },
                ),
        ) { content() }
    }
}

/** Pure rules of the swipe, for unit tests. */
internal object V5SwipeToDeleteRules {
    /** How long a deleted row may stay in the list before it is taken as kept and springs back. */
    const val RETURN_AFTER_MILLIS = 1_000L

    /** A row swiped to the left past the overview's card-close share of its width is deleted. */
    fun deletes(offsetPx: Float, widthPx: Float): Boolean =
        widthPx > 0f && -offsetPx >= widthPx * V5Motion.CARD_CLOSE_COMMIT_FRACTION

    /**
     * The same with the overview card's fling (one system, СС-5): a row let go while flung to the
     * left at [V5Motion.TAB_SWIPE_COMMIT_VELOCITY_DP] goes even short of 40 %; a fling back keeps it.
     */
    fun deletes(offsetPx: Float, velocityPxPerSecond: Float, widthPx: Float, density: Float): Boolean {
        if (widthPx <= 0f || offsetPx >= 0f) return false
        if (deletes(offsetPx, widthPx)) return true
        return velocityPxPerSecond < 0f && abs(velocityPxPerSecond) >= V5Motion.TAB_SWIPE_COMMIT_VELOCITY_DP * density
    }
}
