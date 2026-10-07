package dev.sk2andy.materialbrowser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteOperationQueueTest {
    @Test
    fun `an idle queue runs at once`() {
        val queue = FavoriteOperationQueue()
        val ran = mutableListOf<String>()
        queue.run { ran += "rename" }
        assertEquals(listOf("rename"), ran)
        assertFalse(queue.isBusy)
    }

    @Test
    fun `taps during a write wait and run in order, each on what the last one left`() {
        val queue = FavoriteOperationQueue()
        var library = listOf("a", "b")
        val ran = mutableListOf<String>()
        // A delete starts a write.
        queue.run {
            ran += "delete"
            queue.begin()
        }
        // A rename and an undo arrive before it commits.
        queue.run {
            ran += "rename"
            library = library.map { if (it == "b") "B" else it }
            queue.begin()
        }
        queue.run {
            ran += "undo saw $library"
        }
        assertEquals(listOf("delete"), ran)

        library = listOf("b")
        queue.end()
        // The rename starts its own write; the undo waits for that one too.
        assertEquals(listOf("delete", "rename"), ran)
        assertTrue(queue.isBusy)

        queue.end()
        assertEquals(listOf("delete", "rename", "undo saw [B]"), ran)
        assertFalse(queue.isBusy)
    }

    @Test
    fun `a closed screen runs nothing it queued`() {
        var closed = false
        val queue = FavoriteOperationQueue(isClosed = { closed })
        val ran = mutableListOf<String>()
        queue.run { queue.begin() }
        queue.run { ran += "open" }
        closed = true
        queue.end()
        assertEquals(emptyList<String>(), ran)
    }
}
