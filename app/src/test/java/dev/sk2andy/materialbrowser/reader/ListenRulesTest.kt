package dev.sk2andy.materialbrowser.reader

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
}
