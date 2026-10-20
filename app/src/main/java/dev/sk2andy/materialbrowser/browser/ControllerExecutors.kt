package dev.sk2andy.materialbrowser.browser

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs [task] on this executor unless it was shut down: [BrowserController.destroy] stops the
 * controller's executors, and a callback that arrives later then does nothing instead of throwing.
 */
internal fun Executor.executeIfRunning(task: () -> Unit) {
    try {
        execute(task)
    } catch (_: RejectedExecutionException) {
        // Shut down with the controller: nothing is left to do the work for.
    }
}

/**
 * Waits for a Web Push clean-up on the calling background thread, at most [timeoutMillis]. The
 * history executor runs it and every history write shares that thread, so a clean-up that hangs
 * counts as failed rather than holding the history back.
 */
internal fun awaitPushCleanup(
    timeoutMillis: Long = PUSH_CLEANUP_TIMEOUT_MILLIS,
    cleanup: suspend () -> Boolean,
): Boolean = runCatching {
    runBlocking { withTimeoutOrNull(timeoutMillis) { cleanup() } ?: false }
}.getOrDefault(false)

/** Long enough for a slow push server to answer, short enough not to stall history for long. */
internal const val PUSH_CLEANUP_TIMEOUT_MILLIS = 15_000L
