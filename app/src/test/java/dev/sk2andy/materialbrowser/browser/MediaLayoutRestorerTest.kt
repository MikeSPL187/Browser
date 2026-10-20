package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLayoutRestorerTest {
    private val frameQueue = ArrayDeque<() -> Unit>()
    private val delayed = mutableListOf<Runnable>()
    private var insetsReady = true
    private val layouts = mutableListOf<String>()

    private val restorer = MediaLayoutRestorer(
        object : MediaLayoutRestorer.Frames<String> {
            override fun requestApplyInsets(view: String) = Unit
            override fun requestLayout(view: String) {
                layouts += view
            }
            override fun postOnAnimation(view: String, block: () -> Unit) {
                frameQueue.addLast(block)
            }
            override fun postOnWindowAnimation(block: () -> Unit) {
                frameQueue.addLast(block)
            }
            override fun isInsetReady(view: String) = insetsReady
            override fun postDelayed(task: Runnable, delayMillis: Long) {
                delayed += task
            }
            override fun removeCallbacks(task: Runnable) {
                delayed -= task
            }
        },
    )

    private fun drawFrames() {
        while (frameQueue.isNotEmpty()) frameQueue.removeFirst()()
    }

    private fun fireTimeouts() {
        val tasks = delayed.toList()
        delayed.clear()
        tasks.forEach(Runnable::run)
    }

    @Test
    fun theCoverStaysUntilThePageRestoredAndTheHostDrewItsFrames() {
        var accepted: Boolean? = null
        restorer.schedule("video") { acceptCompletion -> accepted = acceptCompletion() }
        assertTrue(restorer.isPending)

        drawFrames()

        assertEquals(true, accepted)
        assertEquals(listOf("video"), layouts)
        assertFalse(restorer.isPending)
        assertNull(restorer.pending)
    }

    @Test
    fun withoutReadyInsetsTheRestorationWaitsForTheTimeout() {
        insetsReady = false
        var restored = 0
        restorer.schedule("video") { restored++ }

        drawFrames()
        assertEquals(0, restored)

        fireTimeouts()
        drawFrames()
        assertEquals(1, restored)
    }

    @Test
    fun theInsetCallbackStartsItOnlyOnce() {
        insetsReady = false
        var restored = 0
        restorer.schedule("video") { restored++ }

        restorer.start()
        restorer.start()
        drawFrames()

        assertEquals(1, restored)
        assertTrue(delayed.isEmpty())
    }

    @Test
    fun cancellingTellsTheCallerAndVoidsTheLateCompletion() {
        var cancelled = 0
        var acceptCompletion: (() -> Boolean)? = null
        restorer.schedule("video", onCancelled = { cancelled++ }) { acceptCompletion = it }
        drawFrames()
        val lateCompletion = requireNotNull(acceptCompletion)

        restorer.cancel()

        assertEquals(1, cancelled)
        assertFalse(lateCompletion())
        assertFalse(restorer.isPending)
    }

    @Test
    fun aNewRestorationReplacesTheOldOne() {
        var firstCancelled = 0
        var restoredView: String? = null
        restorer.schedule("first", onCancelled = { firstCancelled++ }) { restoredView = "first" }

        restorer.schedule("second") { restoredView = "second" }
        drawFrames()

        assertEquals(1, firstCancelled)
        assertEquals("second", restoredView)
    }

    @Test
    fun theFrameTimeoutFinishesWhenFramesStall() {
        restorer.schedule("video") { acceptCompletion -> acceptCompletion() }
        repeat(4) { frameQueue.removeFirst()() }
        // Restoration ran; the host frames never came.
        frameQueue.clear()
        assertTrue(restorer.isPending)

        fireTimeouts()

        assertFalse(restorer.isPending)
    }
}
