package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class V5NewTabRulesTest {
    @Test
    fun `pinned sites fill rows of four and add comes last`() {
        val rows = V5NewTabRules.rows(count = 5, canAdd = true)
        assertEquals(listOf(4, 2), rows.map { it.size })
        assertEquals(V5NewTabRules.Cell.Site(4), rows[1][0])
        assertEquals(V5NewTabRules.Cell.Add, rows[1][1])
    }

    @Test
    fun `a full grid has no add cell and an empty one only add`() {
        assertEquals(listOf(4, 4), V5NewTabRules.rows(count = 8, canAdd = false).map { it.size })
        assertEquals(listOf(listOf(V5NewTabRules.Cell.Add)), V5NewTabRules.rows(count = 0, canAdd = true))
        assertEquals(emptyList<List<V5NewTabRules.Cell>>(), V5NewTabRules.rows(count = 0, canAdd = false))
    }

    @Test
    fun `a site keeps its letter tile and never gets gray`() {
        val tile = V5NewTabRules.letterTile("wikipedia.org")
        assertEquals(tile, V5NewTabRules.letterTile("wikipedia.org"))
        (0 until 200).forEach { assertNotEquals(V5Tile.Gray, V5NewTabRules.letterTile("site$it.example")) }
    }

    @Test
    fun `the letter is the first letter or digit`() {
        assertEquals("W", V5NewTabRules.letter("wikipedia.org"))
        assertEquals("Я", V5NewTabRules.letter("  яндекс"))
        assertEquals("3", V5NewTabRules.letter("3dnews.ru"))
        assertEquals("•", V5NewTabRules.letter("—"))
    }

    @Test
    fun `the week ends on the last day`() {
        // Epoch day 0 is Thursday, 1 January 1970.
        assertEquals(listOf("Tue", "Wed", "Thu"), V5NewTabRules.weekdays(lastEpochDay = 0, days = 3, locale = Locale.ENGLISH))
    }
}
