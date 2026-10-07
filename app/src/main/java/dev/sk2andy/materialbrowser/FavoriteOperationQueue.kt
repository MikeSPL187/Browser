package dev.sk2andy.materialbrowser

/**
 * Serializes the favorites manager's writes. A tap that arrives while a write is running is not
 * dropped: it waits here and runs in order once the write ends, on whatever library it left.
 */
internal class FavoriteOperationQueue(
    /** True once the screen is gone; queued operations then never run. */
    private val isClosed: () -> Boolean = { false },
) {
    /** A write has begun and has not ended yet. */
    var isBusy: Boolean = false
        private set

    private val pending = ArrayDeque<() -> Unit>()

    /** Runs [operation] now, or after the running write. */
    fun run(operation: () -> Unit) {
        if (isBusy) pending.addLast(operation) else operation()
    }

    /** Called by an operation that starts an asynchronous write. */
    fun begin() {
        isBusy = true
    }

    /** Called when that write ends, whatever its outcome; runs the queued operations in order. */
    fun end() {
        isBusy = false
        while (!isBusy && !isClosed()) {
            val next = pending.removeFirstOrNull() ?: return
            next()
        }
    }
}
