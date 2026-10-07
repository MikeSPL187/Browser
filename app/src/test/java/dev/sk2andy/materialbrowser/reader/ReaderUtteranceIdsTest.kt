package dev.sk2andy.materialbrowser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderUtteranceIdsTest {
    @Test
    fun `the same chunk in a new playback gets a different id`() {
        val stopped = ReaderUtteranceIds.id(generation = 1, ordinal = 0, cursor = 0)
        val replayed = ReaderUtteranceIds.id(generation = 2, ordinal = 0, cursor = 0)

        assertNotEquals(stopped, replayed)
        assertEquals(1, ReaderUtteranceIds.generationOf(stopped))
        assertEquals(2, ReaderUtteranceIds.generationOf(replayed))
    }

    @Test
    fun `a late callback of a stopped playback is not current`() {
        val stale = ReaderUtteranceIds.generationOf(ReaderUtteranceIds.id(3, 4, 120))

        assertEquals(3, stale)
        assertFalse(ReaderUtteranceIds.isCurrent(checkNotNull(stale), currentGeneration = 4))
        assertTrue(ReaderUtteranceIds.isCurrent(checkNotNull(stale), currentGeneration = 3))
    }

    @Test
    fun `ids without a generation are ignored`() {
        assertNull(ReaderUtteranceIds.generationOf(null))
        assertNull(ReaderUtteranceIds.generationOf("reader-0-0"))
        assertNull(ReaderUtteranceIds.generationOf("other-1-0-0"))
        assertNull(ReaderUtteranceIds.generationOf("reader-x-0-0"))
    }
}
