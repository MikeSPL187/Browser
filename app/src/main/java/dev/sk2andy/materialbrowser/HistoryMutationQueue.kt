package dev.sk2andy.materialbrowser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * History edits confirmed on the History screen, saved one at a time in the order they were
 * confirmed. An edit confirmed while another is still being written waits for [mutex] instead of
 * being dropped. [inFlight] counts the edits not saved yet, so leaving the screen can wait for
 * them; [onIdle] runs when the last one is saved.
 *
 * Call [enqueue] on the thread [scope] runs on (the main thread in the app), so the counter needs
 * no lock. [scope] should outlive the screen: an edit confirmed just before it closes still runs.
 */
internal class HistoryMutationQueue(
    private val scope: CoroutineScope,
    private val mutex: Mutex = Mutex(),
    private val onIdle: () -> Unit = {},
) {
    var inFlight: Int = 0
        private set

    val isBusy: Boolean get() = inFlight > 0

    fun enqueue(mutation: suspend () -> Unit) {
        inFlight++
        scope.launch {
            try {
                mutex.withLock { mutation() }
            } finally {
                inFlight--
                if (inFlight == 0) onIdle()
            }
        }
    }
}
