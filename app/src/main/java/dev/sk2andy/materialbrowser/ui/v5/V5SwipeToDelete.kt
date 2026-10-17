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
import androidx.compose.ui.unit.IntOffset
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * A list row a swipe to the left deletes (the library's rows, language packs): past 40 % of the
 * row it goes, as a card in the overview closes; a shorter swipe springs back. The red under the
 * row shows what the swipe does. Without system animations the row jumps instead of springing.
 * A swipe is not the only way: the [content] row offers «Delete» to TalkBack itself.
 */
@Composable
internal fun V5SwipeToDelete(
    key: Any,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = V5Theme.colors
    val offset = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableFloatStateOf(0f) }
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
                    onDragStopped = {
                        val target = if (V5SwipeToDeleteRules.deletes(offset.value, widthPx)) -widthPx else 0f
                        if (V5Motion.animationsEnabled) {
                            offset.animateTo(target, V5Motion.tabSwipe.spec())
                        } else {
                            offset.snapTo(target)
                        }
                        if (target < 0f) onDelete()
                    },
                ),
        ) { content() }
    }
}

/** Pure rules of the swipe, for unit tests. */
internal object V5SwipeToDeleteRules {
    /** A row swiped to the left past the overview's card-close share of its width is deleted. */
    fun deletes(offsetPx: Float, widthPx: Float): Boolean =
        widthPx > 0f && -offsetPx >= widthPx * V5Motion.CARD_CLOSE_COMMIT_FRACTION
}
