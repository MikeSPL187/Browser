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
    fun `a dragged tile takes the place it is dropped on`() {
        val order = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), V5NewTabRules.moved(order, "a", 2))
        assertEquals(listOf("d", "a", "b", "c"), V5NewTabRules.moved(order, "d", 0))
        assertEquals(listOf("b", "c", "d", "a"), V5NewTabRules.moved(order, "a", 99))
        assertEquals(order, V5NewTabRules.moved(order, "x", 1))
        assertEquals(order, V5NewTabRules.moved(order, "b", 1))
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

    @Test
    fun `a nonsense column count still gives rows`() {
        assertEquals(listOf(1, 1), V5NewTabRules.rows(count = 1, canAdd = true, columns = 0).map { it.size })
    }

    @Test
    fun `moving in an empty or single grid changes nothing`() {
        assertEquals(emptyList<String>(), V5NewTabRules.moved(emptyList(), "a", 0))
        assertEquals(listOf("a"), V5NewTabRules.moved(listOf("a"), "a", 5))
        assertEquals(listOf("b", "a"), V5NewTabRules.moved(listOf("a", "b"), "b", -3))
    }

    @Test
    fun `a title with no letter or an empty one gets the dot`() {
        assertEquals("•", V5NewTabRules.letter(""))
        assertEquals("•", V5NewTabRules.letter("   "))
    }

    @Test
    fun `weekdays cross a year boundary and speak the locale`() {
        // Epoch day 18628 is Friday, 1 January 2021; the two days before are Wednesday and Thursday.
        assertEquals(
            listOf("Ср", "Чт", "Пт"),
            V5NewTabRules.weekdays(lastEpochDay = 18628, days = 3, locale = Locale.forLanguageTag("ru")),
        )
        assertEquals(emptyList<String>(), V5NewTabRules.weekdays(lastEpochDay = 0, days = 0, locale = Locale.ENGLISH))
    }

    @Test
    fun `the pinned note explains an empty or a full grid, never while editing`() {
        assertEquals(V5NewTabRules.PinnedNote.Empty, V5NewTabRules.pinnedNote(pinnedCount = 0, canAdd = true, editing = false))
        assertEquals(V5NewTabRules.PinnedNote.Full, V5NewTabRules.pinnedNote(pinnedCount = 16, canAdd = false, editing = false))
        assertEquals(null, V5NewTabRules.pinnedNote(pinnedCount = 5, canAdd = true, editing = false))
        assertEquals(null, V5NewTabRules.pinnedNote(pinnedCount = 16, canAdd = false, editing = true))
        // A preview without an editor cannot add either, but is not full.
        assertEquals(null, V5NewTabRules.pinnedNote(pinnedCount = 3, canAdd = false, editing = false))
    }
}
