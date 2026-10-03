package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaWorkspaceSwipe
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The workspace swipe of the tab overview (board W-WorkspaceSwipe, decision П8). A drag to the
 * left brings the next workspace in from the right; a drag to the right, the previous one.
 */
internal object WorkspaceSwipeRules {
    /** Past this share of the width a released drag switches the workspace. */
    const val COMMIT_FRACTION = 0.3f

    /** With no workspace that way the content still follows the finger, four times slower. */
    const val EDGE_RESISTANCE = 0.25f

    /** How far the leaving content fades: it never quite vanishes under the finger. */
    const val MAX_FADE = 0.6f

    /** The arriving workspace slides in over this share of the width. */
    const val ENTER_FRACTION = 0.25f

    /** -1 for a drag to the left (next workspace), 1 to the right (previous), 0 for none. */
    fun direction(offset: Float): Int = sign(offset).toInt()

    /** The workspace a drag in [direction] brings, or null at either end of the dock. */
    fun neighbor(ids: List<String>, activeId: String, direction: Int): String? {
        val index = ids.indexOf(activeId)
        if (index < 0 || direction == 0) return null
        return ids.getOrNull(index - direction)
    }

    /** The slide for a tap on [targetId] in the dock: one to the right comes from the right. */
    fun directionTo(ids: List<String>, activeId: String, targetId: String): Int {
        val from = ids.indexOf(activeId)
        val to = ids.indexOf(targetId)
        return if (from < 0 || to < 0 || to >= from) -1 else 1
    }

    /** Where the content sits for a raw drag of [rawOffset]: resisted when nothing waits there. */
    fun visibleOffset(rawOffset: Float, hasNeighbor: Boolean): Float =
        if (hasNeighbor) rawOffset else rawOffset * EDGE_RESISTANCE

    /** A release switches when the drag went far enough, or was flung, toward a workspace. */
    fun shouldCommit(
        offset: Float,
        width: Float,
        velocity: Float,
        flingVelocity: Float,
        hasNeighbor: Boolean,
    ): Boolean {
        if (!hasNeighbor || width <= 0f || offset == 0f) return false
        val farEnough = abs(offset) >= width * COMMIT_FRACTION
        val flung = abs(velocity) >= flingVelocity && sign(velocity) == sign(offset)
        return farEnough || flung
    }

    /** How much of the way to the next workspace the content is, from 0 to 1. */
    fun progress(offset: Float, width: Float): Float =
        if (width <= 0f) 0f else (abs(offset) / width).coerceIn(0f, 1f)
}

/**
 * The overview's content while a workspace changes, by a swipe or a tap in the dock: it follows the
 * finger, slides out, waits for the switch (a locked workspace asks for biometrics first) and the
 * next workspace slides in from the other side. A refused switch slides the content back.
 */
@Stable
internal class WorkspaceSwitchState(private val scope: CoroutineScope) {
    private val offset = Animatable(0f)
    private var rawOffset = 0f
    private var shownProfileId: String? = null
    var width by mutableFloatStateOf(0f)

    /** A switch is running: the dock and the swipe wait for it. */
    var switching by mutableStateOf(false)
        private set

    /** The workspace the content is heading to, whose aura shows through while it moves. */
    var targetProfileId by mutableStateOf<String?>(null)
        private set

    val translation: Float get() = offset.value
    val progress: Float get() = WorkspaceSwipeRules.progress(offset.value, width)

    /** Moves the content with the finger; true when it just crossed the commit line. */
    fun drag(delta: Float, neighborOf: (Int) -> String?): Boolean {
        val wasPast = abs(offset.value) >= width * WorkspaceSwipeRules.COMMIT_FRACTION
        rawOffset += delta
        val neighbor = neighborOf(WorkspaceSwipeRules.direction(rawOffset))
        targetProfileId = neighbor
        val visible = WorkspaceSwipeRules.visibleOffset(rawOffset, neighbor != null)
        scope.launch { offset.snapTo(visible) }
        val isPast = neighbor != null && abs(visible) >= width * WorkspaceSwipeRules.COMMIT_FRACTION
        return isPast && !wasPast
    }

    /** Ends a drag: switches when [WorkspaceSwipeRules.shouldCommit] says so, else springs back. */
    fun release(
        velocity: Float,
        flingVelocity: Float,
        switchTo: (profileId: String, direction: Int) -> Unit,
    ) {
        val target = targetProfileId
        val committed = target != null && WorkspaceSwipeRules.shouldCommit(
            offset = offset.value,
            width = width,
            velocity = velocity,
            flingVelocity = flingVelocity,
            hasNeighbor = true,
        )
        val direction = WorkspaceSwipeRules.direction(offset.value)
        rawOffset = 0f
        if (committed && target != null) {
            switchTo(target, direction)
        } else {
            scope.launch {
                offset.animateTo(0f, VolaMotion.standard())
                targetProfileId = null
            }
        }
    }

    /**
     * Slides the content out in [direction], runs [select] (false when the workspace stayed shut),
     * then slides the chosen workspace in, or the old one back.
     */
    suspend fun slideTo(
        profileId: String,
        direction: Int,
        select: suspend (String) -> Boolean,
        onSelected: suspend () -> Unit,
    ) {
        if (switching) return
        switching = true
        targetProfileId = profileId
        try {
            offset.animateTo(direction * width, VolaMotion.fast())
            if (select(profileId)) {
                shownProfileId = profileId
                offset.snapTo(-direction * width * WorkspaceSwipeRules.ENTER_FRACTION)
                onSelected()
            }
            targetProfileId = null
            offset.animateTo(0f, VolaMotion.standard())
        } finally {
            withContext(NonCancellable) {
                offset.snapTo(0f)
                targetProfileId = null
                switching = false
            }
        }
    }

    /**
     * The overview shows [profileId]. When it changed elsewhere (settings, a link) it slides in all
     * the same; the first workspace the overview shows just appears, under the hero entry.
     */
    suspend fun show(profileId: String) {
        val previous = shownProfileId
        shownProfileId = profileId
        if (switching || previous == null || previous == profileId) return
        offset.snapTo(width * WorkspaceSwipeRules.ENTER_FRACTION)
        offset.animateTo(0f, VolaMotion.standard())
    }
}

@Composable
internal fun rememberWorkspaceSwitchState(scope: CoroutineScope): WorkspaceSwitchState =
    remember(scope) { WorkspaceSwitchState(scope) }

/** The swipe itself, over the whole overview; cards that take horizontal drags keep them. */
internal fun Modifier.workspaceSwipe(
    enabled: Boolean,
    state: WorkspaceSwitchState,
    flingVelocity: Float,
    neighborOf: (Int) -> String?,
    onCommitLine: () -> Unit,
    switchTo: (profileId: String, direction: Int) -> Unit,
): Modifier = this
    .onSizeChanged { size -> state.width = size.width.toFloat() }
    .pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val velocity = VelocityTracker()
        detectHorizontalDragGestures(
            onDragStart = { velocity.resetTracking() },
            onDragEnd = {
                state.release(velocity.calculateVelocity().x, flingVelocity, switchTo)
            },
            onDragCancel = { state.release(0f, flingVelocity, switchTo) },
            onHorizontalDrag = { change, delta ->
                velocity.addPosition(change.uptimeMillis, change.position)
                change.consume()
                if (state.drag(delta, neighborOf)) onCommitLine()
            },
        )
    }

/** The content of the overview as the workspace changes: it moves and fades with the switch. */
internal fun Modifier.workspaceSwitchLayer(state: WorkspaceSwitchState): Modifier =
    graphicsLayer {
        translationX = state.translation
        alpha = 1f - state.progress * WorkspaceSwipeRules.MAX_FADE
    }

/** The fling speed that switches a workspace however short the drag, in pixels a second. */
@Composable
internal fun workspaceFlingVelocity(): Float =
    with(LocalDensity.current) { VolaWorkspaceSwipe.flingVelocity.toPx() }
