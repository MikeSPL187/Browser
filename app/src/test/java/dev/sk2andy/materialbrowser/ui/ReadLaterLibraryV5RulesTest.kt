package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.reader.ReaderDocument
import dev.sk2andy.materialbrowser.reader.ReaderSnapshot
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryRules
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadLaterLibraryV5RulesTest {
    private fun snapshot(id: String, title: String, url: String, savedAt: Long, site: String = "") =
        ReaderSnapshot(id, ReaderDocument(title, url, site, emptyList()), progress = 0f, savedAtMillis = savedAt)

    private val aurora = snapshot("a", "Northern lights", "https://www.polar-notes.ru/aurora", savedAt = 1, site = "Polar notes")
    private val train = snapshot("b", "Train to Murmansk", "https://rzd.example/train", savedAt = 2)

    @Test
    fun `the newest saved article comes first`() {
        assertEquals(listOf("b", "a"), ReadLaterLibraryV5Rules.shown(listOf(aurora, train), "").map { it.id })
    }

    @Test
    fun `search finds an article by its title, site or address, in any case`() {
        assertEquals(listOf("a"), ReadLaterLibraryV5Rules.shown(listOf(aurora, train), "LIGHTS").map { it.id })
        assertEquals(listOf("a"), ReadLaterLibraryV5Rules.shown(listOf(aurora, train), "polar notes").map { it.id })
        assertEquals(listOf("b"), ReadLaterLibraryV5Rules.shown(listOf(aurora, train), " rzd ").map { it.id })
        assertEquals(emptyList<String>(), ReadLaterLibraryV5Rules.shown(listOf(aurora, train), "tundra").map { it.id })
    }

    @Test
    fun `the share read shows only between the first line and the end`() {
        assertNull(ReadLaterLibraryV5Rules.readPercent(0f))
        assertEquals(40, ReadLaterLibraryV5Rules.readPercent(0.4f))
        assertNull(ReadLaterLibraryV5Rules.readPercent(1f))
        assertNull(ReadLaterLibraryV5Rules.readPercent(2f))
    }

    @Test
    fun `the host drops www and falls back to the address`() {
        assertEquals("polar-notes.ru", ReadLaterLibraryV5Rules.host("https://www.polar-notes.ru/aurora"))
        assertEquals("not a url", ReadLaterLibraryV5Rules.host("not a url"))
    }

    @Test
    fun `the control offers history, favorites and read later; downloads stand alone`() {
        assertEquals(
            listOf(V5LibrarySegment.History, V5LibrarySegment.Favorites, V5LibrarySegment.ReadLater),
            V5LibraryRules.segments,
        )
    }
}
