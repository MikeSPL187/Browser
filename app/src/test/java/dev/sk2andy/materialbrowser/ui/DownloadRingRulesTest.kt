package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadStatus
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
}
