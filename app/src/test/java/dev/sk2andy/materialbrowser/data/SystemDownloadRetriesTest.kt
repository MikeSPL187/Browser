package dev.sk2andy.materialbrowser.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemDownloadRetriesTest {
    private val remembered = mutableSetOf<Long>()

    @After
    fun forgetAll() {
        SystemDownloadRetries.forget(remembered + (1L..200L))
    }

    @Test
    fun `retry enqueues again and moves the retry to the new row`() {
        var nextId = 101L
        var enqueued = 0
        remember(7L) {
            enqueued += 1
            nextId++
        }

        assertEquals(101L, SystemDownloadRetries.retry(7L))

        assertEquals(1, enqueued)
        assertFalse(SystemDownloadRetries.canRetry(7L))
        assertTrue("the new row starts again the same way", SystemDownloadRetries.canRetry(101L))
        assertEquals(102L, SystemDownloadRetries.retry(101L))
        assertEquals(2, enqueued)
    }

    @Test
    fun `a row that could not be enqueued again keeps its retry`() {
        remember(8L) { null }
        assertNull(SystemDownloadRetries.retry(8L))
        assertTrue(SystemDownloadRetries.canRetry(8L))

        remember(9L) { error("no download service") }
        assertNull(SystemDownloadRetries.retry(9L))
        assertTrue(SystemDownloadRetries.canRetry(9L))
    }

    @Test
    fun `nothing is remembered for a row the app did not enqueue in this run`() {
        assertFalse(SystemDownloadRetries.canRetry(42L))
        assertNull(SystemDownloadRetries.retry(42L))
    }

    @Test
    fun `only the latest rows are remembered`() {
        (1L..SystemDownloadRetries.MAX_REMEMBERED + 1L).forEach { id -> remember(id) { null } }

        assertFalse(SystemDownloadRetries.canRetry(1L))
        assertTrue(SystemDownloadRetries.canRetry(2L))
        assertTrue(SystemDownloadRetries.canRetry(SystemDownloadRetries.MAX_REMEMBERED + 1L))
    }

    @Test
    fun `saved, removed and cleared rows are forgotten`() {
        remember(11L) { null }
        remember(12L) { null }
        remember(13L) { null }

        SystemDownloadRetries.retainUnfinished(listOf(11L, 13L))
        assertTrue(SystemDownloadRetries.canRetry(11L))
        assertFalse(SystemDownloadRetries.canRetry(12L))

        SystemDownloadRetries.forget(listOf(13L))
        assertFalse(SystemDownloadRetries.canRetry(13L))
        assertTrue(SystemDownloadRetries.canRetry(11L))
    }

    private fun remember(id: Long, restart: () -> Long?) {
        remembered += id
        SystemDownloadRetries.remember(id, restart)
    }
}
