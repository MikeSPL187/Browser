package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsControllerTest {
    private val running = entry(1, DownloadStatus.Running)
    private val done = entry(2, DownloadStatus.Successful)

    @Test
    fun `a refresh shows the store's downloads and polls faster while one runs`() {
        val store = FakeStore(listOf(running, done))
        val controller = DownloadsController(store, ImmediateWorker)

        controller.refresh()

        assertEquals(listOf(running, done), controller.downloads)
        assertEquals(DownloadsController.ACTIVE_POLL_MILLIS, controller.pollDelayMillis)
        store.entries = listOf(done)
        controller.refresh()
        assertEquals(DownloadsController.IDLE_POLL_MILLIS, controller.pollDelayMillis)
    }

    @Test
    fun `a refresh that finishes late never replaces a newer list`() {
        val store = FakeStore(listOf(running))
        val worker = QueuedWorker()
        val controller = DownloadsController(store, worker)

        controller.refresh()
        val stale = worker.take()
        store.entries = listOf(done)
        controller.refresh()
        worker.runAll()
        stale()
        worker.runAll()

        assertEquals(listOf(done), controller.downloads)
    }

    @Test
    fun `a slow store runs one read at a time and still updates the list`() {
        val store = FakeStore(listOf(running))
        val worker = QueuedWorker()
        val controller = DownloadsController(store, worker)

        controller.refresh()
        repeat(10) {
            store.entries = listOf(running, entry(10L + it, DownloadStatus.Running))
            controller.refresh()
            assertEquals(1, worker.pending)
        }
        store.entries = listOf(done)
        worker.take()()

        assertEquals(listOf(done), controller.downloads)
        assertEquals(1, worker.pending)
        worker.runAll()
        assertEquals(listOf(done), controller.downloads)
        assertEquals(2, store.snapshots)
        controller.refresh()
        assertEquals(1, worker.pending)
    }

    @Test
    fun `clearing runs once at a time, then refreshes and reports the outcome`() {
        val store = FakeStore(listOf(done))
        val worker = QueuedWorker()
        val controller = DownloadsController(store, worker)
        val outcomes = mutableListOf<Boolean>()

        controller.clearFinished(listOf(done)) { outcomes += it }
        controller.clearFinished(listOf(done)) { outcomes += it }
        assertTrue(controller.isClearing)
        worker.runAll()

        assertFalse(controller.isClearing)
        assertEquals(listOf(true), outcomes)
        assertEquals(1, store.clears)
        assertEquals(emptyList<DownloadEntry>(), controller.downloads)
    }

    @Test
    fun `pause and cancel go to the store and refresh the list`() {
        val store = FakeStore(listOf(running))
        val controller = DownloadsController(store, ImmediateWorker)
        val outcomes = mutableListOf<Boolean>()

        controller.togglePause(running) { outcomes += it }
        assertEquals(DownloadStatus.Paused, controller.downloads.single().status)
        controller.cancel(running) { outcomes += it }
        assertEquals(emptyList<DownloadEntry>(), controller.downloads)
        controller.cancel(running) { outcomes += it }

        assertEquals(listOf(true, true, false), outcomes)
    }

    private class FakeStore(var entries: List<DownloadEntry>) : DownloadsController.DownloadStore {
        var clears = 0
        var snapshots = 0

        override fun snapshot(): List<DownloadEntry> {
            snapshots++
            return entries
        }

        override fun clear(entries: Collection<DownloadEntry>): Boolean {
            clears++
            this.entries = this.entries - entries.toSet()
            return true
        }

        override fun cancel(entry: DownloadEntry): Boolean {
            val before = entries
            entries = entries.filterNot { it.id == entry.id }
            return entries != before
        }

        override fun togglePause(entry: DownloadEntry): Boolean {
            entries = entries.map { current ->
                if (current.id != entry.id) current
                else current.copy(
                    status = if (current.status == DownloadStatus.Paused) DownloadStatus.Running else DownloadStatus.Paused,
                )
            }
            return true
        }
    }

    private object ImmediateWorker : DownloadsController.Worker {
        override fun <T> run(work: () -> T, onResult: (T) -> Unit) = onResult(work())
    }

    /** Holds work until the test runs it, to finish refreshes out of order. */
    private class QueuedWorker : DownloadsController.Worker {
        private val queue = ArrayDeque<() -> Unit>()

        override fun <T> run(work: () -> T, onResult: (T) -> Unit) {
            queue.addLast { onResult(work()) }
        }

        val pending: Int get() = queue.size

        fun take(): () -> Unit = queue.removeFirst()

        fun runAll() {
            while (queue.isNotEmpty()) queue.removeFirst()()
        }
    }

    private fun entry(id: Long, status: DownloadStatus) = DownloadEntry(
        id = id,
        name = "file-$id.pdf",
        source = "",
        status = status,
        bytes = 1,
        total = 2,
        lastModified = id,
        mime = "",
    )
}
