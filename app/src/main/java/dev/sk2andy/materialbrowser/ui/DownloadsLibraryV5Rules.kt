package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadHistoryRules
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryTrailing

internal object DownloadsLibraryV5Rules {
    /** A file kind's tile; a download that did not finish is red whatever it is. */
    fun tile(kind: DownloadKind, failed: Boolean): V5Tile = if (failed) V5Tile.Red else when (kind) {
        DownloadKind.Pdf -> V5Tile.Red
        DownloadKind.Document -> V5Tile.Blue
        DownloadKind.Spreadsheet -> V5Tile.Green
        DownloadKind.Image -> V5Tile.Sky
        DownloadKind.Video -> V5Tile.Purple
        DownloadKind.Audio -> V5Tile.Pink
        DownloadKind.Archive -> V5Tile.Amber
        DownloadKind.App -> V5Tile.Teal
        DownloadKind.Other -> V5Tile.Gray
    }

    fun failed(entry: DownloadEntry): Boolean =
        entry.status == DownloadStatus.Failed || entry.status == DownloadStatus.Cancelled

    /**
     * A download under way shows its progress; one that failed and can start again, «Retry»; a
     * finished file, its [time].
     */
    fun trailing(entry: DownloadEntry, time: String): V5LibraryTrailing = when {
        entry.status.isActive -> V5LibraryTrailing.Progress(DownloadHistoryRules.progress(entry))
        !failed(entry) -> V5LibraryTrailing.Value(time)
        entry.retry == DownloadRetry.Available -> V5LibraryTrailing.Retry
        else -> V5LibraryTrailing.None
    }

    /**
     * A swipe cancels a download under way. A finished file is never swiped away: clearing it deletes
     * the file itself, so that waits for «Clear…» and its confirmation.
     */
    fun swipeCancels(entry: DownloadEntry): Boolean = entry.status.isActive && entry.supportsCancel
}
