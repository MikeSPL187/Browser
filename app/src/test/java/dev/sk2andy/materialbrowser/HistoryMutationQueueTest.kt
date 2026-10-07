package dev.sk2andy.materialbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryMutationQueueTest {
    @Test
    fun `edits confirmed during a write wait and run in order`() = runBlocking {
        val log = mutableListOf<String>()
        val firstWrite = CompletableDeferred<Unit>()
        var idleCalls = 0
        val queue = HistoryMutationQueue(scope = this, onIdle = { idleCalls++ })

        queue.enqueue {
            log += "A started"
            firstWrite.await()
            log += "A saved"
        }
        queue.enqueue { log += "B saved" }
        queue.enqueue { log += "C saved" }
        assertEquals(3, queue.inFlight)

        while (log.isEmpty()) yield()
        repeat(3) { yield() }
        assertEquals(listOf("A started"), log)
        assertTrue(queue.isBusy)

        firstWrite.complete(Unit)
        while (queue.isBusy) yield()

        assertEquals(listOf("A started", "A saved", "B saved", "C saved"), log)
        assertEquals(1, idleCalls)
    }

    @Test
    fun `the queue is idle again after each burst`() = runBlocking {
        var idleCalls = 0
        val queue = HistoryMutationQueue(scope = this, onIdle = { idleCalls++ })

        queue.enqueue {}
        while (queue.isBusy) yield()
        queue.enqueue {}
        queue.enqueue {}
        while (queue.isBusy) yield()

        assertFalse(queue.isBusy)
        assertEquals(0, queue.inFlight)
        assertEquals(2, idleCalls)
    }
}
