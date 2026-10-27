package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
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
    fun `a download that can pause shows whether it is paused, one that cannot shows no glyph`() {
        val running = entry(DownloadStatus.Running, 5, 10).copy(supportsPause = true)
        assertEquals(V5LibraryTrailing.Progress(0.5f, paused = false), DownloadsLibraryV5Rules.trailing(running, "9:41"))
        assertEquals(
            V5LibraryTrailing.Progress(0.5f, paused = true),
            DownloadsLibraryV5Rules.trailing(running.copy(status = DownloadStatus.Paused), "9:41"),
        )
    }

    @Test
    fun `a swipe cancels a download under way and deletes one file that ended, whatever its end`() {
        assertTrue(DownloadsLibraryV5Rules.swipes(entry(DownloadStatus.Running)))
        assertFalse(DownloadsLibraryV5Rules.swipes(entry(DownloadStatus.Running).copy(supportsCancel = false)))
        assertTrue(DownloadsLibraryV5Rules.swipes(entry(DownloadStatus.Successful)))
        assertTrue(DownloadsLibraryV5Rules.swipes(entry(DownloadStatus.Failed)))
        assertTrue(DownloadsLibraryV5Rules.swipes(entry(DownloadStatus.Cancelled)))
        assertTrue(DownloadsLibraryV5Rules.swipeCancels(entry(DownloadStatus.Paused)))
        assertFalse(DownloadsLibraryV5Rules.swipeCancels(entry(DownloadStatus.Successful)))
    }

    @Test
    fun `a failed file is red whatever its kind`() {
        assertEquals(V5Tile.Sky, DownloadsLibraryV5Rules.tile(DownloadKind.Image, failed = false))
        assertEquals(V5Tile.Red, DownloadsLibraryV5Rules.tile(DownloadKind.Image, failed = true))
    }

    @Test
    fun `a failed download that cannot start again says why`() {
        fun hint(status: DownloadStatus, reason: DownloadRetryUnavailable, page: String? = null) =
            DownloadsLibraryV5Rules.unavailable(entry(status, retry = DownloadRetry.Unavailable(reason, page)))

        assertEquals(
            DownloadUnavailableHint.OpenPage,
            hint(DownloadStatus.Failed, DownloadRetryUnavailable.Blob, "https://polar-notes.ru/a"),
        )
        assertEquals(
            DownloadUnavailableHint.FromSite,
            hint(DownloadStatus.Failed, DownloadRetryUnavailable.Blob, "blob:https://polar-notes.ru/1"),
        )
        assertEquals(DownloadUnavailableHint.PrivateClosed, hint(DownloadStatus.Cancelled, DownloadRetryUnavailable.PrivateClosed))
        assertEquals(null, DownloadsLibraryV5Rules.unavailable(entry(DownloadStatus.Failed, retry = DownloadRetry.Available)))
        assertEquals(null, DownloadsLibraryV5Rules.unavailable(entry(DownloadStatus.Successful)))
    }

    @Test
    fun `the chips sort files into documents, images and archives, and all shows every kind`() {
        assertTrue(V5DownloadFilter.Documents.matches(DownloadKind.Pdf))
        assertTrue(V5DownloadFilter.Documents.matches(DownloadKind.Spreadsheet))
        assertFalse(V5DownloadFilter.Documents.matches(DownloadKind.Image))
        assertTrue(V5DownloadFilter.Images.matches(DownloadKind.Image))
        assertTrue(V5DownloadFilter.Archives.matches(DownloadKind.Archive))
        assertFalse(V5DownloadFilter.Archives.matches(DownloadKind.App))
        assertTrue(DownloadKind.entries.all(V5DownloadFilter.All::matches))
    }
}
