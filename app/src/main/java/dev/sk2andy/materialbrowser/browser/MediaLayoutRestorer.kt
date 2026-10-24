package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class PendingMediaLayoutRestoration<V : Any>(
    val request: MediaLayoutRestorationGate.Request,
    val view: V,
    val restore: (acceptCompletion: () -> Boolean) -> Unit,
) {
    var started = false
}

/**
 * The page's layout coming back after fullscreen or picture-in-picture video (#500): the cover stays
 * up while the system bars return, the engine view lays out again and the page restores its media,
 * and comes down only after the host has drawn a few frames past that. [V] is the engine view
 * ([android.view.View] in the app).
 */
internal class MediaLayoutRestorer<V : Any>(private val frames: Frames<V>) {
    /** What the restoration asks of the view system. */
    interface Frames<V> {
        fun requestApplyInsets(view: V)

        /** Insets, layout and a redraw for [view] and its parent. */
        fun requestLayout(view: V)

        fun postOnAnimation(view: V, block: () -> Unit)

        /** The next frame of the whole window, past every view's layout. */
        fun postOnWindowAnimation(block: () -> Unit)

        /** The system bars are back and the keyboard is gone: the page sees its final insets. */
        fun isInsetReady(view: V): Boolean

        fun postDelayed(task: Runnable, delayMillis: Long)

        fun removeCallbacks(task: Runnable)
    }

    private val gate = MediaLayoutRestorationGate()

    var pending: PendingMediaLayoutRestoration<V>? = null
        private set

    /** The cover over the page stays while this is true. */
    var isPending by mutableStateOf(false)
        private set
    private var insetTimeout: Runnable? = null

    /**
     * Starts restoring [view]: [restore] runs once the insets are ready (or after a timeout) and
     * reports through its argument whether its completion still counts.
     */
    fun schedule(
        view: V,
        onCancelled: () -> Unit = {},
        restore: (acceptCompletion: () -> Boolean) -> Unit,
    ) {
        cancel()
        val request = gate.begin(onCancelled)
        pending = PendingMediaLayoutRestoration(request = request, view = view, restore = restore)
        isPending = true
        frames.requestApplyInsets(view)
        frames.postOnAnimation(view) {
            frames.postOnAnimation(view) {
                val current = pending?.takeIf { candidate -> candidate.request === request }
                    ?: return@postOnAnimation
                if (frames.isInsetReady(current.view)) start()
            }
        }
        insetTimeout = Runnable {
            if (pending?.request === request) start()
        }.also { timeout -> frames.postDelayed(timeout, INSET_TIMEOUT_MILLIS) }
    }

    /** The insets are ready: the pending restoration, if any and not yet started, runs. */
    fun start() {
        val pending = pending ?: return
        if (pending.started) return
        pending.started = true
        insetTimeout?.let(frames::removeCallbacks)
        insetTimeout = null
        requestLayoutBeforeRestore(pending.view) {
            pending.restore {
                if (this.pending !== pending) return@restore false
                // The content ACK only confirms DOM geometry. Keep the cover until the Gecko
                // host has also received its layout/inset passes and drawn after them.
                fun finishAfterHostFrames() {
                    if (this.pending === pending && gate.complete(pending.request)) {
                        this.pending = null
                        isPending = false
                    }
                }
                fun awaitHostFrames(remaining: Int) {
                    if (remaining == 0) {
                        finishAfterHostFrames()
                    } else {
                        frames.postOnWindowAnimation { awaitHostFrames(remaining - 1) }
                    }
                }
                awaitHostFrames(HOST_FRAMES)
                frames.postDelayed({ finishAfterHostFrames() }, FRAME_TIMEOUT_MILLIS)
                true
            }
        }
    }

    /** Lays [view] out again and calls [onReady] two frames later, once that layout is drawn. */
    fun requestLayoutBeforeRestore(view: V, onReady: () -> Unit) {
        frames.requestLayout(view)
        frames.postOnAnimation(view) {
            frames.postOnAnimation(view, onReady)
        }
    }

    fun cancel() {
        pending ?: return
        pending = null
        insetTimeout?.let(frames::removeCallbacks)
        insetTimeout = null
        isPending = false
        gate.cancel()
    }

    private companion object {
        const val INSET_TIMEOUT_MILLIS = 350L
        const val FRAME_TIMEOUT_MILLIS = 500L
        const val HOST_FRAMES = 3
    }
}
