package dev.sk2andy.materialbrowser.audit

import android.os.Looper

/**
 * Looks at the app's main thread once a second while a walk step runs. A step that takes minutes
 * either waits on an idle app (a test wait) or on a busy main thread; the samples tell which, and
 * where the busy thread spends its time, without a profiler on the CI emulator.
 */
internal class MainThreadSampler : AutoCloseable {
    private val main = Looper.getMainLooper().thread
    private val stacks = HashMap<String, Int>()
    private var samples = 0
    private val sampler = Thread({
        while (!Thread.currentThread().isInterrupted) {
            try {
                Thread.sleep(SAMPLE_MILLIS)
            } catch (_: InterruptedException) {
                return@Thread
            }
            record(main.stackTrace)
        }
    }, "audit-main-thread-sampler").apply {
        isDaemon = true
        start()
    }

    @Synchronized
    private fun record(stack: Array<StackTraceElement>) {
        samples++
        if (idle(stack)) return
        // The top frames say what the thread is doing; the app's own frames below say for whom.
        val top = stack.take(TOP_FRAMES)
        val app = stack.drop(TOP_FRAMES).filter { it.className.startsWith(APP_PACKAGE) }.take(APP_FRAMES)
        val signature = (top + app).joinToString(" < ") { frame ->
            "${frame.className.substringAfterLast('.')}.${frame.methodName}:${frame.lineNumber}"
        }
        stacks[signature] = (stacks[signature] ?: 0) + 1
    }

    /** How busy the main thread was, and the stacks it was seen in most. */
    @Synchronized
    fun summary(): String {
        if (samples == 0) return "main thread not sampled"
        val busy = stacks.values.sum()
        val top = stacks.entries.sortedByDescending { it.value }.take(STACKS_SHOWN)
            .joinToString(" | ") { (stack, count) -> "${count}× $stack" }
        return "main thread busy in $busy of $samples samples" + if (top.isEmpty()) "" else ": $top"
    }

    override fun close() {
        sampler.interrupt()
    }

    private companion object {
        const val SAMPLE_MILLIS = 1_000L
        const val TOP_FRAMES = 6
        const val APP_FRAMES = 3
        const val APP_PACKAGE = "dev.sk2andy.materialbrowser."
        const val STACKS_SHOWN = 2

        /** Parked in the message queue: nothing to run. */
        fun idle(stack: Array<StackTraceElement>) =
            stack.firstOrNull()?.methodName == "nativePollOnce"
    }
}
