package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.DownloadEntry

/**
 * The list behind the library's Downloads screen: what is downloading and what is done, and the
 * pause, cancel and clear actions on it. The screen's activity owns one and polls it while visible.
 *
 * Reading and changing downloads blocks, so it runs through [Worker]. Only one read runs at a time:
 * a refresh asked for while one is running is folded into a single follow-up read, so a slow store
 * neither piles up reads nor keeps the list from updating, and results land in order.
 */
class DownloadsController internal constructor(
    private val store: DownloadStore,
    private val worker: Worker,
) {
    /** Where downloads live: the system download manager and the engine's own transfers. */
    internal interface DownloadStore {
        fun snapshot(): List<DownloadEntry>

        fun clear(entries: Collection<DownloadEntry>): Boolean

        fun cancel(entry: DownloadEntry): Boolean

        fun togglePause(entry: DownloadEntry): Boolean
    }

    /** Runs [work] off the main thread and hands its result back on it. */
    internal interface Worker {
        fun <T> run(work: () -> T, onResult: (T) -> Unit)
    }

    internal var downloads by mutableStateOf<List<DownloadEntry>>(emptyList())
        private set

    internal var isClearing by mutableStateOf(false)
        private set

    private var isRefreshing = false
    private var refreshAgain = false

    /** How long to wait before the next refresh: short while something is downloading. */
    internal val pollDelayMillis: Long
        get() = if (downloads.any { entry -> entry.status.isActive }) ACTIVE_POLL_MILLIS else IDLE_POLL_MILLIS

    internal fun refresh() {
        if (isRefreshing) {
            refreshAgain = true
            return
        }
        isRefreshing = true
        worker.run(store::snapshot) { entries ->
            downloads = entries
            isRefreshing = false
            if (refreshAgain) {
                refreshAgain = false
                refresh()
            }
        }
    }

    /** Removes finished [entries] from the list; [onResult] hears whether all of them went. */
    internal fun clearFinished(entries: List<DownloadEntry>, onResult: (Boolean) -> Unit = {}) {
        if (isClearing) return
        isClearing = true
        worker.run({ store.clear(entries) }) { cleared ->
            isClearing = false
            refresh()
            onResult(cleared)
        }
    }

    internal fun cancel(entry: DownloadEntry, onResult: (Boolean) -> Unit = {}) =
        change({ store.cancel(entry) }, onResult)

    internal fun togglePause(entry: DownloadEntry, onResult: (Boolean) -> Unit = {}) =
        change({ store.togglePause(entry) }, onResult)

    private fun change(action: () -> Boolean, onResult: (Boolean) -> Unit) {
        worker.run(action) { changed ->
            refresh()
            onResult(changed)
        }
    }

    internal companion object {
        const val ACTIVE_POLL_MILLIS = 750L
        const val IDLE_POLL_MILLIS = 3_000L
    }
}
