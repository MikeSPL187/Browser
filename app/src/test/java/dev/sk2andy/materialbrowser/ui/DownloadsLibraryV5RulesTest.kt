package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryTrailing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsLibraryV5RulesTest {
    private fun entry(status: DownloadStatus, bytes: Long = 0, total: Long = 0, retry: DownloadRetry? = null) =
        DownloadEntry(
            id = 7,
            name = "guide.pdf",
            source = "polar-notes.ru",
            status = status,
            bytes = bytes,
            total = total,
            lastModified = 0,
            mime = "application/pdf",
        ).let { if (retry != null) it.copy(retry = retry) else it }

    @Test
    fun `a download under way shows its progress, unknown while the size is`() {
        assertEquals(V5LibraryTrailing.Progress(0.5f), DownloadsLibraryV5Rules.trailing(entry(DownloadStatus.Running, 5, 10), "9:41"))
        assertEquals(V5LibraryTrailing.Progress(null), DownloadsLibraryV5Rules.trailing(entry(DownloadStatus.Pending), "9:41"))
    }

    @Test
    fun `a failed download offers retry only when it can start again, a finished one its time`() {
        assertEquals(
            V5LibraryTrailing.Retry,
            DownloadsLibraryV5Rules.trailing(entry(DownloadStatus.Failed, retry = DownloadRetry.Available), "9:41"),
        )
        assertEquals(V5LibraryTrailing.None, DownloadsLibraryV5Rules.trailing(entry(DownloadStatus.Failed), "9:41"))
        assertEquals(V5LibraryTrailing.Value("9:41"), DownloadsLibraryV5Rules.trailing(entry(DownloadStatus.Successful), "9:41"))
    }

    @Test
    fun `only a download under way is swiped away, never a finished file`() {
        assertTrue(DownloadsLibraryV5Rules.swipeCancels(entry(DownloadStatus.Running)))
        assertFalse(DownloadsLibraryV5Rules.swipeCancels(entry(DownloadStatus.Running).copy(supportsCancel = false)))
        assertFalse(DownloadsLibraryV5Rules.swipeCancels(entry(DownloadStatus.Successful)))
        assertFalse(DownloadsLibraryV5Rules.swipeCancels(entry(DownloadStatus.Failed)))
    }

    @Test
    fun `a failed file is red whatever its kind`() {
        assertEquals(V5Tile.Sky, DownloadsLibraryV5Rules.tile(DownloadKind.Image, failed = false))
        assertEquals(V5Tile.Red, DownloadsLibraryV5Rules.tile(DownloadKind.Image, failed = true))
    }
}
