package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5TabOverviewRulesTest {
    private val polar = V5OverviewTab("1", "Полярные заметки", "https://polar-notes.ru", isPrivate = false)
    private val recipe = V5OverviewTab("2", "Пирог с брусникой", "https://recipes.ru/pie", isPrivate = false)
    private val secret = V5OverviewTab("3", "Подарок", "https://shop.ru/gift", isPrivate = true)
    private val locked = V5OverviewTab("4", "Банк", "https://bank.ru", isPrivate = true)
    private val tabs = listOf(polar, recipe, secret, locked)
    private val hidden: (V5OverviewTab) -> Boolean = { it.id == locked.id }

    @Test
    fun `segments split tabs and private tabs and skip locked ones`() {
        assertEquals(V5OverviewCounts(regular = 2, private = 1), V5TabOverviewRules.counts(tabs, hidden))
        assertEquals(listOf(polar, recipe), V5TabOverviewRules.visibleTabs(tabs, privateSegment = false, "", hidden))
        assertEquals(listOf(secret), V5TabOverviewRules.visibleTabs(tabs, privateSegment = true, "", hidden))
    }

    @Test
    fun `search narrows the segment by title or address words`() {
        assertEquals(listOf(polar), V5TabOverviewRules.visibleTabs(tabs, false, "полярные", hidden))
        assertEquals(listOf(recipe), V5TabOverviewRules.visibleTabs(tabs, false, "recipes pie", hidden))
        assertEquals(emptyList<V5OverviewTab>(), V5TabOverviewRules.visibleTabs(tabs, false, "банк", hidden))
    }

    @Test
    fun `the overview opens on the selected tab's segment while it has tabs`() {
        assertTrue(V5TabOverviewRules.initialSegmentIsPrivate(true, V5OverviewCounts(2, 1)))
        assertFalse(V5TabOverviewRules.initialSegmentIsPrivate(true, V5OverviewCounts(2, 0)))
        assertFalse(V5TabOverviewRules.initialSegmentIsPrivate(false, V5OverviewCounts(2, 1)))
    }

    @Test
    fun `a card closes past 40 percent of its width or on a fling the same way`() {
        val width = 400f
        val density = 2f
        assertTrue(V5TabOverviewRules.closes(-170f, 0f, width, density))
        assertFalse(V5TabOverviewRules.closes(150f, 0f, width, density))
        assertTrue(V5TabOverviewRules.closes(60f, 2100f, width, density))
        assertFalse(V5TabOverviewRules.closes(60f, -2100f, width, density))
        assertFalse(V5TabOverviewRules.closes(0f, 5000f, width, density))
    }

    @Test
    fun `a swiped card fades with its travel`() {
        assertEquals(1f, V5TabOverviewRules.swipeAlpha(0f, 400f), 0.001f)
        assertEquals(0.7f, V5TabOverviewRules.swipeAlpha(-200f, 400f), 0.001f)
        assertEquals(0.4f, V5TabOverviewRules.swipeAlpha(900f, 400f), 0.001f)
    }

    private val dinner = V5OverviewGroup("g1", "Ужин", listOf("3", "1"), collapsed = false, tile = V5Tile.Red)
    private val trip = V5OverviewGroup("g2", "Поездка", listOf("4", "5"), collapsed = true, tile = V5Tile.Blue)

    @Test
    fun `groups come first with their cards in tab order, then the rest under No group`() {
        val items = V5OverviewGroupRules.items(listOf("1", "2", "3", "4", "5"), listOf(dinner, trip), searching = false)
        assertEquals(
            listOf(
                V5OverviewItem.GroupHeader(dinner, 2),
                V5OverviewItem.Card("1", dinner),
                V5OverviewItem.Card("3", dinner),
                V5OverviewItem.CollapsedGroup(trip, 2, listOf("4", "5")),
                V5OverviewItem.UngroupedHeader,
                V5OverviewItem.Card("2", null),
            ),
            items,
        )
    }

    @Test
    fun `without visible groups or while searching the grid stays flat`() {
        val flat = listOf(V5OverviewItem.Card("2", null), V5OverviewItem.Card("6", null))
        assertEquals(flat, V5OverviewGroupRules.items(listOf("2", "6"), listOf(dinner, trip), searching = false))
        assertEquals(
            listOf(V5OverviewItem.Card("1", null), V5OverviewItem.Card("3", null)),
            V5OverviewGroupRules.items(listOf("1", "3"), listOf(dinner), searching = true),
        )
    }

    @Test
    fun `the grid opens at the selected card's group`() {
        val items = V5OverviewGroupRules.items(listOf("1", "2", "3", "4", "5"), listOf(dinner, trip), searching = false)
        assertEquals(0, V5OverviewGroupRules.indexOf(items, "3"))
        assertEquals(3, V5OverviewGroupRules.indexOf(items, "5"))
        assertEquals(5, V5OverviewGroupRules.indexOf(items, "2"))
        assertEquals(0, V5OverviewGroupRules.indexOf(items, "missing"))
    }
}
