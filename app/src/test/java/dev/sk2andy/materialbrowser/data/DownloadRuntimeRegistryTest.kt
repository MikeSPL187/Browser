package dev.sk2andy.materialbrowser.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRuntimeRegistryTest {
    private val id = DownloadRuntimeRegistry.nextTransferId()

    @After
    fun clearEntry() {
        DownloadRuntimeRegistry.completed(id)
    }

    @Test
    fun `runtime controls target encoded media entry and toggle current pause state`() {
        var cancelled = false
        start(
            cancel = { cancelled = true },
            pause = {
                DownloadRuntimeRegistry.paused(id, paused = true, updatedAt = 20L)
                true
            },
            resume = {
                DownloadRuntimeRegistry.paused(id, paused = false, updatedAt = 30L)
                true
            },
        )
        val entry = DownloadRuntimeRegistry.snapshot().single { it.id == -43L }
        assertTrue(entry.supportsPause)
        assertTrue(entry.supportsCancel)
        assertTrue(DownloadRuntimeRegistry.togglePause(entry.id))
        assertEquals(DownloadStatus.Paused, DownloadRuntimeRegistry.snapshot().single { it.id == entry.id }.status)
        assertTrue(DownloadRuntimeRegistry.togglePause(entry.id))
        assertEquals(DownloadStatus.Running, DownloadRuntimeRegistry.snapshot().single { it.id == entry.id }.status)
        assertTrue(DownloadRuntimeRegistry.cancel(entry.id))
        assertTrue(cancelled)
    }

    @Test
    fun `notification controls target active transfer identity`() {
        var cancelled = false
        start(
            cancel = { cancelled = true },
            pause = {
                DownloadRuntimeRegistry.paused(id, paused = true, updatedAt = 20L)
                true
            },
            resume = {
                DownloadRuntimeRegistry.paused(id, paused = false, updatedAt = 30L)
                true
            },
        )

        assertEquals(-43L, requireNotNull(DownloadRuntimeRegistry.entryForTransfer(id)).id)
        assertTrue(DownloadRuntimeRegistry.togglePauseTransfer(id))
        assertEquals(DownloadStatus.Paused, DownloadRuntimeRegistry.entryForTransfer(id)?.status)
        assertTrue(DownloadRuntimeRegistry.togglePauseTransfer(id))
        assertEquals(DownloadStatus.Running, DownloadRuntimeRegistry.entryForTransfer(id)?.status)
        assertTrue(DownloadRuntimeRegistry.cancelTransfer(id))
        assertTrue(cancelled)
    }

    @Test
    fun `terminal and completed transfers reject stale controls`() {
        var cancelled = false
        start(cancel = { cancelled = true })
        DownloadRuntimeRegistry.failed(id, cancelled = false, updatedAt = 20L)
        assertFalse(DownloadRuntimeRegistry.cancel(-43L))
        assertFalse(DownloadRuntimeRegistry.cancelTransfer(id))
        assertFalse(DownloadRuntimeRegistry.togglePause(-43L))
        assertFalse(DownloadRuntimeRegistry.togglePauseTransfer(id))
        assertFalse(cancelled)
        DownloadRuntimeRegistry.completed(id)
        assertFalse(DownloadRuntimeRegistry.cancel(-43L))
    }

    @Test
    fun `transfers without controls do not offer unsupported actions`() {
        start()
        val entry = DownloadRuntimeRegistry.snapshot().single { it.id == -43L }
        assertFalse(entry.supportsPause)
        assertFalse(entry.supportsCancel)
        assertFalse(DownloadRuntimeRegistry.cancel(entry.id))
        assertFalse(DownloadRuntimeRegistry.togglePause(entry.id))
    }

    @Test
    fun `failed rows start again once and wait as pending until the attempt ends`() {
        start(cancel = {})
        DownloadRuntimeRegistry.failed(id, cancelled = false, updatedAt = 20L)
        assertEquals(NOT_REMEMBERED, entry().retry)
        var attempts = 0
        assertTrue(DownloadRuntimeRegistry.offerRetry(id) { attempts++; true })
        assertEquals(DownloadRetry.Available, entry().retry)

        assertTrue(DownloadRuntimeRegistry.retry(-43L))
        assertEquals(1, attempts)
        assertEquals(DownloadStatus.Pending, entry().status)
        assertFalse(entry().supportsCancel)
        assertFalse(entry().supportsPause)
        assertFalse("a pending row does not start twice", DownloadRuntimeRegistry.retry(-43L))
        assertEquals(1, attempts)

        DownloadRuntimeRegistry.retryFailed(id, updatedAt = 30L)
        assertEquals(DownloadStatus.Failed, entry().status)
        assertEquals(30L, entry().lastModified)
        assertEquals(DownloadRetry.Available, entry().retry)
        assertTrue(DownloadRuntimeRegistry.retry(-43L))
        assertEquals(2, attempts)

        DownloadRuntimeRegistry.completed(id)
        assertFalse(DownloadRuntimeRegistry.retry(-43L))
    }

    @Test
    fun `a changed file withholds the retry and names the page`() {
        start()
        DownloadRuntimeRegistry.failed(id, cancelled = true, updatedAt = 20L)
        assertTrue(DownloadRuntimeRegistry.offerRetry(id) { true })
        assertTrue(DownloadRuntimeRegistry.retry(-43L))
        val changed = DownloadRetry.Unavailable(
            DownloadRetryUnavailable.ResponseChanged,
            pageUrl = "https://example.com/report",
        )
        DownloadRuntimeRegistry.retryFailed(id, updatedAt = 30L, retry = changed)
        assertEquals(DownloadStatus.Failed, entry().status)
        assertEquals(changed, entry().retry)
        assertFalse(DownloadRuntimeRegistry.retry(-43L))
        assertFalse(id in DownloadRuntimeRegistry.retryableTransferIds())
    }

    @Test
    fun `active and finished rows take no retry`() {
        start()
        assertFalse(DownloadRuntimeRegistry.offerRetry(id) { true })
        assertFalse(DownloadRuntimeRegistry.retry(-43L))
        DownloadRuntimeRegistry.completed(id)
        assertFalse(DownloadRuntimeRegistry.offerRetry(id) { true })
    }

    @Test
    fun `only the latest rows keep their retry`() {
        val ids = List(DownloadRuntimeRegistry.MAX_RETRIES + 1) { DownloadRuntimeRegistry.nextTransferId() }
        try {
            ids.forEachIndexed { index, transferId ->
                DownloadRuntimeRegistry.started(
                    id = transferId,
                    name = "file-$index.bin",
                    source = "https://example.com/$index",
                    mime = "application/octet-stream",
                    total = -1L,
                    startedAt = index.toLong(),
                )
                DownloadRuntimeRegistry.failed(transferId, cancelled = false, updatedAt = index.toLong())
                assertTrue(DownloadRuntimeRegistry.offerRetry(transferId) { true })
            }
            assertEquals(NOT_REMEMBERED, DownloadRuntimeRegistry.entryForTransfer(ids.first())?.retry)
            assertEquals(ids.drop(1).toSet(), DownloadRuntimeRegistry.retryableTransferIds() intersect ids.toSet())
            assertEquals(DownloadRetry.Available, DownloadRuntimeRegistry.entryForTransfer(ids.last())?.retry)
        } finally {
            ids.forEach(DownloadRuntimeRegistry::completed)
        }
    }

    @Test
    fun `clearing a row forgets its retry`() {
        start()
        DownloadRuntimeRegistry.failed(id, cancelled = false, updatedAt = 20L)
        assertTrue(DownloadRuntimeRegistry.offerRetry(id) { true })
        DownloadRuntimeRegistry.clear(listOf(-43L))
        assertFalse(id in DownloadRuntimeRegistry.retryableTransferIds())
        assertFalse(DownloadRuntimeRegistry.retry(-43L))
    }

    private fun entry() = requireNotNull(DownloadRuntimeRegistry.entryForTransfer(id))

    private fun start(
        cancel: (() -> Unit)? = null,
        pause: (() -> Boolean)? = null,
        resume: (() -> Boolean)? = null,
    ) = DownloadRuntimeRegistry.started(
        id = id,
        name = "download.bin",
        source = "https://example.com/download.bin",
        mime = "application/octet-stream",
        total = 100L,
        startedAt = 10L,
        mediaStoreId = 42L,
        cancel = cancel,
        pause = pause,
        resume = resume,
    )

    private companion object {
        val NOT_REMEMBERED = DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered)
    }
}
