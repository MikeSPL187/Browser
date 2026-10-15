package dev.sk2andy.materialbrowser.data

/**
 * How rows of Android's DownloadManager that failed start again (V22), by download id, in memory
 * only. Whoever enqueues a download hands in how to enqueue it once more: the same address, name,
 * type and headers, with the cookies read again from the profile it came from. After a restart of
 * the app none is left, so those rows offer no retry: which profile a row came from is not known
 * then, and another profile's cookies must never reach it.
 */
internal object SystemDownloadRetries {
    /** Enqueues the download again and answers the new row's id, or null when it could not. */
    private val restarts = linkedMapOf<Long, () -> Long?>()

    @Synchronized
    fun remember(id: Long, restart: () -> Long?) {
        restarts.remove(id)
        restarts[id] = restart
        while (restarts.size > MAX_REMEMBERED) restarts.remove(restarts.keys.first())
    }

    @Synchronized
    fun canRetry(id: Long): Boolean = id in restarts

    /**
     * Enqueues row [id] again and answers the new row's id; the new row may start again the same
     * way, and [id] no longer can. Null when nothing is remembered for [id] or enqueueing failed;
     * then [id] keeps its retry.
     */
    fun retry(id: Long): Long? {
        val restart = synchronized(this) { restarts[id] } ?: return null
        val newId = runCatching(restart).getOrNull() ?: return null
        synchronized(this) {
            restarts.remove(id)
            remember(newId, restart)
        }
        return newId
    }

    /** Keeps only rows that may still fail: [ids] are the rows that are neither gone nor saved. */
    @Synchronized
    fun retainUnfinished(ids: Collection<Long>) {
        restarts.keys.retainAll(ids.toSet())
    }

    @Synchronized
    fun forget(ids: Collection<Long>) {
        ids.forEach(restarts::remove)
    }

    const val MAX_REMEMBERED = 20
}
