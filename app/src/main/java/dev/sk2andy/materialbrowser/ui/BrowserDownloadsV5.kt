package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.DownloadsController
import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadRepository
import dev.sk2andy.materialbrowser.ui.v5.V5DownloadRingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What the downloads ring shows (V22, system-v5.md section 10): the progress of everything that is
 * downloading, summed in bytes; a running arc while some size is unknown; nothing when idle.
 */
internal object DownloadRingRules {
    fun ring(entries: List<DownloadEntry>): V5DownloadRingState? {
        val active = entries.filter { entry -> entry.status.isActive }
        if (active.isEmpty()) return null
        if (active.any { entry -> entry.total <= 0L }) return V5DownloadRingState.Indeterminate(active.size)
        val total = active.sumOf { entry -> entry.total }
        val bytes = active.sumOf { entry -> entry.bytes.coerceIn(0L, entry.total) }
        return V5DownloadRingState.Progress(active.size, bytes.toFloat() / total)
    }

    /** Polls a little longer after a download is asked for: its row can take a moment to appear. */
    const val START_POLLS = 3
}

/**
 * The downloads the v5 bar watches: read from the same store as the Downloads screen, only while
 * something downloads or was just asked for, so an idle browser does not poll.
 */
@Composable
internal fun rememberDownloadRing(controller: BrowserController): V5DownloadRingState? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloads = remember(context) { downloadsController(context.applicationContext, scope) }
    LaunchedEffect(downloads, controller.contentActions.downloadRevision) {
        var polls = 0
        do {
            downloads.refresh()
            delay(DownloadsController.ACTIVE_POLL_MILLIS)
            polls++
        } while (polls < DownloadRingRules.START_POLLS || downloads.downloads.any { entry -> entry.status.isActive })
    }
    return DownloadRingRules.ring(downloads.downloads)
}

private fun downloadsController(context: Context, scope: CoroutineScope): DownloadsController {
    val repository = DownloadRepository(context)
    return DownloadsController(
        store = object : DownloadsController.DownloadStore {
            override fun snapshot() = repository.snapshot()
            override fun clear(entries: Collection<DownloadEntry>) = repository.clear(entries)
            override fun cancel(entry: DownloadEntry) = repository.cancel(entry)
            override fun togglePause(entry: DownloadEntry) = repository.togglePause(entry)
        },
        worker = object : DownloadsController.Worker {
            override fun <T> run(work: () -> T, onResult: (T) -> Unit) {
                scope.launch { onResult(withContext(Dispatchers.IO) { work() }) }
            }
        },
    )
}
