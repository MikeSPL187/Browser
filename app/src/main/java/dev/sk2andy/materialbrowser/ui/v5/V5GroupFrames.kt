package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

/*
 * One frame per open group in the v5 overview (canvas «OverviewV2», system-v5.md section 10): the
 * group's color outlines its header and its cards together and reaches a little into the side margins,
 * so the cards inside stay as wide and as tall as every other card. Lines of the grid report where they
 * are while on screen; a group whose header or last card has scrolled away runs on past the edge.
 */

/**
 * Which lines each frame spans ([frames], [members]: a group's header and card keys), and where the
 * grid's lines are on screen in root coordinates. [lines] outlives a change of the items, so a line
 * that does not move (a tab closed elsewhere) keeps its place.
 */
@Stable
internal class V5GroupFramesState(
    val frames: List<V5GroupFrame>,
    val members: Map<String, Set<String>>,
    val lines: SnapshotStateMap<String, Rect>,
    private val originState: MutableState<Offset>,
) {
    var origin: Offset
        get() = originState.value
        set(value) {
            originState.value = value
        }
}

@Composable
internal fun rememberV5GroupFrames(items: List<V5OverviewItem>): V5GroupFramesState {
    val lines = remember { mutableStateMapOf<String, Rect>() }
    val origin = remember { mutableStateOf(Offset.Zero) }
    return remember(items) {
        val members = items.mapNotNull { item ->
            when (item) {
                is V5OverviewItem.GroupHeader -> item.group.id to item.key
                is V5OverviewItem.Card -> item.group?.let { it.id to item.key }
                else -> null
            }
        }.groupBy({ it.first }, { it.second }).mapValues { (_, keys) -> keys.toSet() }
        V5GroupFramesState(V5OverviewGroupRules.frames(items), members, lines, origin)
    }
}

/** A line of the grid: reports its place while it is on screen, and forgets it when it leaves. */
@Composable
internal fun Modifier.v5GroupFrameLine(state: V5GroupFramesState, key: String): Modifier {
    DisposableEffect(state.lines, key) { onDispose { state.lines.remove(key) } }
    return onGloballyPositioned { coordinates ->
        state.lines[key] = Rect(coordinates.positionInRoot(), coordinates.size.toSize())
    }
}

/** The grid: draws each open group's frame behind its lines, clipped to the grid. */
@Composable
internal fun Modifier.v5GroupFrames(state: V5GroupFramesState): Modifier {
    if (state.frames.isEmpty()) return this
    val shape = V5Theme.shapes.l
    return this
        .onGloballyPositioned { state.origin = it.positionInRoot() }
        .clipToBounds()
        .drawBehind {
            val stroke = V5Sizes.groupBorder.toPx()
            // The frame stands in the side margin, outside the cards' own column.
            val left = (V5Spacing.marginPhone - V5Spacing.space2).toPx()
            val right = size.width - left
            val above = V5Spacing.space1.toPx()
            val below = V5Spacing.space2.toPx()
            state.frames.forEach { frame ->
                val shown = state.members[frame.group.id].orEmpty().mapNotNull { state.lines[it] }
                if (shown.isEmpty()) return@forEach
                val top = state.lines[frame.firstKey]?.let { it.top - state.origin.y - above } ?: -size.height
                val bottom = state.lines[frame.lastKey]?.let { it.bottom - state.origin.y + below } ?: (size.height * 2)
                val frameSize = Size(right - left - stroke, bottom - top - stroke)
                if (frameSize.width <= 0f || frameSize.height <= 0f) return@forEach
                val outline = shape.createOutline(frameSize, layoutDirection, this)
                translate(left + stroke / 2, top + stroke / 2) {
                    drawOutline(outline, frame.group.tile.color, style = Stroke(stroke))
                }
            }
        }
}
