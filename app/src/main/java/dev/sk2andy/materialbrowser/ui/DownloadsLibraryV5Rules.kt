package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadHistoryRules
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
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
     * A download under way shows its progress, with the pause or play glyph where it can pause; one
     * that failed and can start again, «Retry»; a finished file, its [time].
     */
    fun trailing(entry: DownloadEntry, time: String): V5LibraryTrailing = when {
        entry.status.isActive -> V5LibraryTrailing.Progress(
            DownloadHistoryRules.progress(entry),
            paused = (entry.status == DownloadStatus.Paused).takeIf { entry.supportsPause },
        )
        !failed(entry) -> V5LibraryTrailing.Value(time)
        entry.retry == DownloadRetry.Available -> V5LibraryTrailing.Retry
        else -> V5LibraryTrailing.None
    }

    /**
     * Why a failed download cannot start again, which its row says instead of a tap that does
     * nothing (DL-03): its page can be opened to fetch it there, its private tab is closed, or it
     * can only be fetched from the site again. Null for a row that can start again or did not fail.
     */
    fun unavailable(entry: DownloadEntry): DownloadUnavailableHint? {
        val retry = entry.retry as? DownloadRetry.Unavailable ?: return null
        return when {
            !failed(entry) -> null
            DownloadRingRules.page(entry) != null -> DownloadUnavailableHint.OpenPage
            retry.reason == DownloadRetryUnavailable.PrivateClosed -> DownloadUnavailableHint.PrivateClosed
            else -> DownloadUnavailableHint.FromSite
        }
    }

    /**
     * A swipe cancels a download under way and deletes a finished, failed or cancelled one (DL-01):
     * one file goes without «Clear…» taking all of them. Either way it asks first, since neither
     * can be undone.
     */
    fun swipes(entry: DownloadEntry): Boolean = if (entry.status.isActive) entry.supportsCancel else true

    /** What the question before a swipe is about: a download under way or what is left of one. */
    fun swipeCancels(entry: DownloadEntry): Boolean = entry.status.isActive
}

/** What a failed download's row says when it cannot start again; see [DownloadsLibraryV5Rules.unavailable]. */
internal enum class DownloadUnavailableHint { OpenPage, PrivateClosed, FromSite }
