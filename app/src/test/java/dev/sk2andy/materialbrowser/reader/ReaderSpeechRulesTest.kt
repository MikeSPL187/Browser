package dev.sk2andy.materialbrowser.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderSpeechRulesTest {
    @Test
    fun `current excerpt follows spoken sentence instead of transport action`() {
        val content = "Title\n\nFirst sentence being read. Second sentence follows now."

        assertEquals("Title", ReaderSpeechRules.currentExcerpt(content, 0))
        assertEquals(
            "First sentence being read.",
            ReaderSpeechRules.currentExcerpt(content, content.indexOf("sentence")),
        )
        assertEquals(
            "Second sentence follows now.",
            ReaderSpeechRules.currentExcerpt(content, content.indexOf("Second") + 8),
        )
    }

    @Test
    fun `current excerpt is safe when the title starts with a sentence boundary`() {
        val body = "A readable paragraph that is long enough to pass the reader threshold."

        assertEquals("!", ReaderSpeechRules.currentExcerpt("!", 0))
        assertEquals("!", ReaderSpeechRules.currentExcerpt("!\n\n$body", 0))
        assertEquals(".", ReaderSpeechRules.currentExcerpt(". Article\n\n$body", 0))
        assertEquals("?", ReaderSpeechRules.currentExcerpt("?x\n\n$body", 0))
        assertEquals("", ReaderSpeechRules.currentExcerpt("   ", 0))
        assertEquals(body, ReaderSpeechRules.currentExcerpt("\n\n$body", 0))
    }

    @Test
    fun `current excerpt on a line break after a period starts at the next sentence`() {
        val content = "First sentence.\n\nSecond sentence here."
        val newline = content.indexOf('\n')

        assertEquals("Second sentence here.", ReaderSpeechRules.currentExcerpt(content, newline))
        assertEquals(
            "Second sentence here.",
            ReaderSpeechRules.currentExcerpt(content, newline + 1),
        )
        assertEquals("", ReaderSpeechRules.currentExcerpt("Done.\n\n", 6))
        assertEquals(
            "Second sentence here.",
            ReaderSpeechRules.currentExcerpt(content, content.length + 5),
        )
    }

    @Test
    fun `speech supports initialize play pause resume and stop`() {
        var state = ReaderSpeechState()
        state = ReaderSpeechRules.reduce(state, ReaderSpeechEvent.Initialized, 1_000)
        assertEquals(ReaderSpeechStatus.Ready, state.status)

        state = ReaderSpeechRules.reduce(state, ReaderSpeechEvent.Play, 1_000)
        state = ReaderSpeechRules.reduce(state, ReaderSpeechEvent.RangeStarted(420), 1_000)
        assertEquals(ReaderSpeechState(ReaderSpeechStatus.Speaking, 420), state)

        state = ReaderSpeechRules.reduce(state, ReaderSpeechEvent.Pause, 1_000)
        assertEquals(ReaderSpeechState(ReaderSpeechStatus.Paused, 420), state)
        state = ReaderSpeechRules.reduce(state, ReaderSpeechEvent.Play, 1_000)
        assertEquals(ReaderSpeechState(ReaderSpeechStatus.Speaking, 420), state)

        state = ReaderSpeechRules.reduce(state, ReaderSpeechEvent.Stop, 1_000)
        assertEquals(ReaderSpeechState(ReaderSpeechStatus.Ready, 0), state)
    }

    @Test
    fun `close is terminal and failed initialization is unavailable`() {
        val unavailable = ReaderSpeechRules.reduce(
            ReaderSpeechState(),
            ReaderSpeechEvent.InitializationFailed,
            10,
        )
        val closed = ReaderSpeechRules.reduce(unavailable, ReaderSpeechEvent.Close, 10)

        assertEquals(ReaderSpeechStatus.Unavailable, unavailable.status)
        assertEquals(ReaderSpeechStatus.Closed, closed.status)
        assertEquals(closed, ReaderSpeechRules.reduce(closed, ReaderSpeechEvent.Play, 10))
        assertEquals(closed, ReaderSpeechRules.reduce(closed, ReaderSpeechEvent.Completed, 10))
        assertEquals(closed, ReaderSpeechRules.reduce(closed, ReaderSpeechEvent.Initialized, 10))
        assertEquals(
            closed,
            ReaderSpeechRules.reduce(closed, ReaderSpeechEvent.InitializationFailed, 10),
        )
    }
}
