package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
import dev.sk2andy.materialbrowser.data.DownloadStatus
import dev.sk2andy.materialbrowser.ui.v5.V5DownloadNotice
import dev.sk2andy.materialbrowser.ui.v5.V5DownloadRingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadRingRulesTest {
    private fun entry(id: Long, status: DownloadStatus, bytes: Long, total: Long) = DownloadEntry(
        id = id,
        name = "file$id.pdf",
        source = "https://example.com/file$id.pdf",
        status = status,
        bytes = bytes,
        total = total,
        lastModified = 0L,
        mime = "application/pdf",
    )

    @Test
    fun `nothing downloading shows no ring`() {
        assertNull(DownloadRingRules.ring(emptyList()))
        assertNull(DownloadRingRules.ring(listOf(entry(1, DownloadStatus.Successful, 10, 10))))
    }

    @Test
    fun `the ring sums the bytes of every active download`() {
        val ring = DownloadRingRules.ring(
            listOf(
                entry(1, DownloadStatus.Running, bytes = 25, total = 100),
                entry(2, DownloadStatus.Paused, bytes = 75, total = 100),
                entry(3, DownloadStatus.Successful, bytes = 500, total = 500),
            ),
        )
        assertEquals(V5DownloadRingState.Progress(count = 2, fraction = 0.5f), ring)
    }

    @Test
    fun `an unknown size makes the ring run`() {
        val ring = DownloadRingRules.ring(
            listOf(
                entry(1, DownloadStatus.Running, bytes = 25, total = 100),
                entry(2, DownloadStatus.Pending, bytes = 0, total = -1),
            ),
        )
        assertEquals(V5DownloadRingState.Indeterminate(count = 2), ring)
    }

    @Test
    fun `bytes past the size do not overfill the ring`() {
        val ring = DownloadRingRules.ring(listOf(entry(1, DownloadStatus.Running, bytes = 150, total = 100)))
        assertEquals(V5DownloadRingState.Progress(count = 1, fraction = 1f), ring)
    }

    @Test
    fun `a download that was running and now finished or failed has ended`() {
        val previous = listOf(
            entry(1, DownloadStatus.Running, bytes = 5, total = 10),
            entry(2, DownloadStatus.Running, bytes = 5, total = 10),
            entry(3, DownloadStatus.Running, bytes = 5, total = 10),
            entry(4, DownloadStatus.Successful, bytes = 10, total = 10),
        )
        val current = listOf(
            entry(1, DownloadStatus.Successful, bytes = 10, total = 10),
            entry(2, DownloadStatus.Cancelled, bytes = 5, total = 10),
            entry(3, DownloadStatus.Running, bytes = 8, total = 10),
            entry(4, DownloadStatus.Successful, bytes = 10, total = 10),
        )
        assertEquals(listOf(1L), DownloadRingRules.ended(previous, current).map { it.id })
    }

    @Test
    fun `an engine transfer saved under a new id is matched by its name`() {
        val transfer = entry(-7, DownloadStatus.Running, bytes = 5, total = 10).copy(name = "report.pdf")
        val saved = entry(42, DownloadStatus.Successful, bytes = 10, total = 10).copy(name = "report.pdf")
        assertEquals(listOf(saved), DownloadRingRules.ended(listOf(transfer), listOf(saved)))
    }

    @Test
    fun `a failure is told before a finish`() {
        val done = entry(1, DownloadStatus.Successful, bytes = 10, total = 10)
        val failed = entry(2, DownloadStatus.Failed, bytes = 5, total = 10)
        assertEquals(V5DownloadNotice.Failed(2, "file2.pdf"), DownloadRingRules.notice(listOf(done, failed)))
        assertEquals(V5DownloadNotice.Done(1, "file1.pdf"), DownloadRingRules.notice(listOf(done)))
        assertNull(DownloadRingRules.notice(emptyList()))
    }

    @Test
    fun `a failure offers a retry only when the browser can fetch it again`() {
        val failed = entry(2, DownloadStatus.Failed, 0, 0)
        assertEquals(V5DownloadNotice.Failed(2, "file2.pdf"), DownloadRingRules.notice(listOf(failed)))
        assertEquals(
            V5DownloadNotice.Failed(2, "file2.pdf", canRetry = true),
            DownloadRingRules.notice(listOf(failed.copy(retry = DownloadRetry.Available))),
        )
        val blob = failed.copy(retry = DownloadRetry.Unavailable(DownloadRetryUnavailable.Blob, "https://example.com/"))
        assertEquals(
            V5DownloadNotice.Failed(2, "file2.pdf", pageUrl = "https://example.com/"),
            DownloadRingRules.notice(listOf(blob)),
        )
    }

    @Test
    fun `only a web page is offered instead of a retry`() {
        fun failed(retry: DownloadRetry) = entry(3, DownloadStatus.Failed, 0, 0).copy(retry = retry)
        val changed = DownloadRetry.Unavailable(DownloadRetryUnavailable.ResponseChanged, "http://example.com/report")
        assertEquals("http://example.com/report", DownloadRingRules.page(failed(changed)))
        assertNull(DownloadRingRules.page(failed(DownloadRetry.Available)))
        assertNull(DownloadRingRules.page(failed(DownloadRetry.Unavailable(DownloadRetryUnavailable.PrivateClosed))))
        val extension = DownloadRetry.Unavailable(DownloadRetryUnavailable.Blob, "moz-extension://abc/page.html")
        assertNull(DownloadRingRules.page(failed(extension)))
    }

    @Test
    fun `progress comes first, then a failure, then a finish`() {
        val progress = V5DownloadRingState.Progress(count = 1, fraction = 0.5f)
        val failed = V5DownloadNotice.Failed(2, "file2.pdf")
        val done = V5DownloadNotice.Done(1, "file1.pdf")
        assertEquals(progress, DownloadRingRules.shown(progress, failed, justDone = true))
        assertEquals(V5DownloadRingState.Failed, DownloadRingRules.shown(null, failed, justDone = true))
        assertEquals(V5DownloadRingState.Done, DownloadRingRules.shown(null, done, justDone = true))
        assertNull(DownloadRingRules.shown(null, done, justDone = false))
        assertNull(DownloadRingRules.shown(null, null, justDone = false))
    }
}
