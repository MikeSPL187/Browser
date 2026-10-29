package dev.sk2andy.materialbrowser.ui

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadStatus

/** «18.6 of 30 MB» while it downloads; just the size so far when the total is unknown. */
internal fun downloadProgressLabel(context: android.content.Context, entry: DownloadEntry): String {
    val bytes = Formatter.formatShortFileSize(context, entry.bytes)
    return if (entry.total > 0L) {
        context.getString(
            R.string.downloads_progress_of,
            bytes,
            Formatter.formatShortFileSize(context, entry.total),
        )
    } else {
        bytes
    }
}

internal fun DownloadEntry.sourceHost(): String? = source.takeIf(String::isNotBlank)
    ?.let(Uri::parse)
    ?.host
    ?.takeIf(String::isNotBlank)

internal fun downloadStatusLabel(status: DownloadStatus): Int = when (status) {
    DownloadStatus.Pending -> R.string.downloads_status_pending
    DownloadStatus.Running -> R.string.downloads_status_running
    DownloadStatus.Paused -> R.string.downloads_status_paused
    DownloadStatus.Successful -> R.string.downloads_status_successful
    DownloadStatus.Failed -> R.string.downloads_status_failed
    DownloadStatus.Cancelled -> R.string.downloads_status_cancelled
}
