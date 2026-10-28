package dev.sk2andy.materialbrowser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenRulesTest {
    private val article = ListenItem("Title", "https://example.com/a", "Text.", isPrivate = false)

    @Test
    fun `the mini player shows while there is something to pause, resume or explain`() {
        listOf(
            ReaderSpeechStatus.Initializing,
            ReaderSpeechStatus.Speaking,
            ReaderSpeechStatus.Paused,
            ReaderSpeechStatus.Unavailable,
        ).forEach { status -> assertTrue(status.name, ListenRules.showsPlayer(article, status)) }
        listOf(ReaderSpeechStatus.Ready, ReaderSpeechStatus.Closed)
            .forEach { status -> assertFalse(status.name, ListenRules.showsPlayer(article, status)) }
        assertFalse(ListenRules.showsPlayer(null, ReaderSpeechStatus.Speaking))
    }

    @Test
    fun `a private page's title stays out of a normal tab's bar`() {
        val private = article.copy(isPrivate = true)
        assertFalse(ListenRules.showsTitle(private, selectedTabPrivate = false))
        assertTrue(ListenRules.showsTitle(private, selectedTabPrivate = true))
        assertTrue(ListenRules.showsTitle(article, selectedTabPrivate = false))
        assertTrue(ListenRules.showsTitle(article, selectedTabPrivate = true))
    }

    @Test
    fun `the mini player shows while a page from the menu is prepared or failed`() {
        val preparing = article.copy(text = "")
        assertTrue(ListenRules.showsPlayer(preparing, ReaderSpeechStatus.Closed, ListenPreparation.Preparing))
        assertTrue(
            ListenRules.showsPlayer(
                preparing,
                ReaderSpeechStatus.Closed,
                ListenPreparation.Failed(ReaderExtractionFailure.EmptyArticle),
            ),
        )
        assertFalse(ListenRules.showsPlayer(null, ReaderSpeechStatus.Closed, ListenPreparation.Preparing))
    }

    @Test
    fun `the speed capsule steps up from the usual speed and comes back slower`() {
        assertEquals(1.2f, ListenRules.nextSpeed(1f))
        assertEquals(1.5f, ListenRules.nextSpeed(1.2f))
        assertEquals(2f, ListenRules.nextSpeed(1.5f))
        assertEquals(0.8f, ListenRules.nextSpeed(2f))
        assertEquals(1f, ListenRules.nextSpeed(0.8f))
        assertEquals(1f, ListenRules.nextSpeed(3f))
    }

    @Test
    fun `an article takes about its words at the spoken pace, shorter when faster`() {
        val minute = "word ".repeat(ListenRules.SPOKEN_WORDS_PER_MINUTE)
        assertEquals(60, ListenRules.durationSeconds(minute, 1f))
        assertEquals(30, ListenRules.durationSeconds(minute, 2f))
        assertEquals(50, ListenRules.durationSeconds(minute, 1.2f))
        assertEquals(0, ListenRules.durationSeconds(" \n ", 1f))
        assertEquals(1, ListenRules.durationSeconds("Word", 2f))
    }

    @Test
    fun `progress follows the voice through the text`() {
        assertEquals(0f, ListenRules.progress("", 10))
        assertEquals(0.5f, ListenRules.progress("abcd", 2))
        assertEquals(1f, ListenRules.progress("abcd", 9))
        assertEquals(0f, ListenRules.progress("abcd", -1))
    }

    @Test
    fun `the clock shows minutes and two-digit seconds`() {
        assertEquals("0:00", ListenRules.clock(0))
        assertEquals("3:12", ListenRules.clock(192))
        assertEquals("8:40", ListenRules.clock(520))
        assertEquals("61:01", ListenRules.clock(3661))
        assertEquals("0:00", ListenRules.clock(-5))
    }
}
