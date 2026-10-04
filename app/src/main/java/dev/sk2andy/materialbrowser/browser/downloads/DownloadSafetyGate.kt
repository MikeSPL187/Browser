package dev.sk2andy.materialbrowser.browser.downloads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.BrowserDownloadRequest
import java.net.URI

/** A download held back by [DownloadSafetyGate] until the user answers. */
class PendingDownloadSafety(
    val fileName: String,
    val sourceHost: String?,
    val findings: List<DownloadSafetyFinding>,
    internal val onSave: () -> Unit,
    internal val onCancel: () -> Unit,
)

/**
 * Holds back a download that [DownloadSafetyCheck] has something to say about, one at a time.
 * Downloads without findings pass straight through. The sheet reads [pending].
 */
class DownloadSafetyGate {
    var pending by mutableStateOf<PendingDownloadSafety?>(null)
        private set
    private val queue = ArrayDeque<PendingDownloadSafety>()

    /** True when [request] now waits for the user: [save] runs on «Download», [cancel] otherwise. */
    fun hold(request: BrowserDownloadRequest, save: () -> Unit, cancel: () -> Unit): Boolean {
        val findings = DownloadSafetyCheck.findings(request.url, request.fileName, request.mimeType)
        if (findings.isEmpty()) return false
        val item = PendingDownloadSafety(
            fileName = request.fileName,
            sourceHost = runCatching { URI(request.url).host }.getOrNull()?.removePrefix("www."),
            findings = findings,
            onSave = save,
            onCancel = cancel,
        )
        if (pending == null) pending = item else queue.addLast(item)
        return true
    }

    fun save() {
        val item = pending ?: return
        pending = queue.removeFirstOrNull()
        item.onSave()
    }

    fun cancel() {
        val item = pending ?: return
        pending = queue.removeFirstOrNull()
        item.onCancel()
    }

    /** Drops every held download, as when the browser closes. */
    fun cancelAll() {
        val items = listOfNotNull(pending) + queue
        pending = null
        queue.clear()
        items.forEach { it.onCancel() }
    }
}
