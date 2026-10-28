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
    fun `segments split tabs and private tabs, locked ones counted but not shown`() {
        assertEquals(V5OverviewCounts(regular = 2, private = 2), V5TabOverviewRules.counts(tabs))
        assertEquals(1, V5TabOverviewRules.lockedPrivate(tabs, hidden))
        assertEquals(0, V5TabOverviewRules.lockedPrivate(tabs) { false })
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

    @Test
    fun `a fling against the drag, no travel or no width never closes a card`() {
        assertFalse(V5TabOverviewRules.closes(offsetPx = 50f, velocityPxPerSecond = -5000f, widthPx = 400f, density = 2f))
        assertFalse(V5TabOverviewRules.closes(offsetPx = -50f, velocityPxPerSecond = 5000f, widthPx = 400f, density = 2f))
        assertFalse(V5TabOverviewRules.closes(offsetPx = 0f, velocityPxPerSecond = 5000f, widthPx = 400f, density = 2f))
        assertFalse(V5TabOverviewRules.closes(offsetPx = 300f, velocityPxPerSecond = 5000f, widthPx = 0f, density = 2f))
        assertTrue(V5TabOverviewRules.closes(offsetPx = -161f, velocityPxPerSecond = 0f, widthPx = 400f, density = 2f))
    }

    @Test
    fun `a card swiped further than its width fades no more than the limit`() {
        val limit = V5TabOverviewRules.swipeAlpha(offsetPx = 400f, widthPx = 400f)
        assertEquals(limit, V5TabOverviewRules.swipeAlpha(offsetPx = -4000f, widthPx = 400f), 0f)
        assertEquals(1f, V5TabOverviewRules.swipeAlpha(offsetPx = 100f, widthPx = 0f), 0f)
    }

    @Test
    fun `a blank search keeps every card and words may be split by tabs or new lines`() {
        assertEquals(listOf(polar, recipe), V5TabOverviewRules.visibleTabs(tabs, false, "   ", hidden))
        assertEquals(listOf(recipe), V5TabOverviewRules.visibleTabs(tabs, false, "recipes\tpie\n", hidden))
    }

    private fun group(id: String, vararg tabIds: String, collapsed: Boolean = false) =
        V5OverviewGroup(id, id, tabIds.toList(), collapsed, V5Tile.Blue)

    @Test
    fun `a group whose tabs are all out of the segment or the search is not shown`() {
        val items = V5OverviewGroupRules.items(listOf("1", "2"), listOf(group("g", "8", "9")), searching = false)
        assertEquals(listOf(V5OverviewItem.Card("1", null), V5OverviewItem.Card("2", null)), items)
    }

    @Test
    fun `a collapsed group is one row with at most three previews in tab order`() {
        val g = group("g", "4", "2", "3", "1", collapsed = true)
        val items = V5OverviewGroupRules.items(listOf("1", "2", "3", "4", "5"), listOf(g), searching = false)
        val row = items[0] as V5OverviewItem.CollapsedGroup
        assertEquals(4, row.count)
        assertEquals(listOf("1", "2", "3"), row.previewIds)
        assertEquals(V5OverviewItem.UngroupedHeader, items[1])
        assertEquals(V5OverviewItem.Card("5", null), items[2])
        assertEquals(3, items.size)
    }

    @Test
    fun `every item of the grid has its own key`() {
        val groups = listOf(group("a", "1", "2"), group("b", "3", collapsed = true))
        val items = V5OverviewGroupRules.items(listOf("1", "2", "3", "4"), groups, searching = false)
        assertEquals(items.map { it.key }.distinct(), items.map { it.key })
    }

    @Test
    fun `the grid opens at a collapsed group's row, a loose card, or the start`() {
        val groups = listOf(group("a", "1", collapsed = true))
        val items = V5OverviewGroupRules.items(listOf("1", "2"), groups, searching = false)
        assertEquals(0, V5OverviewGroupRules.indexOf(items, "1"))
        assertEquals(2, V5OverviewGroupRules.indexOf(items, "2"))
        assertEquals(0, V5OverviewGroupRules.indexOf(items, "zzz"))
        assertEquals(0, V5OverviewGroupRules.indexOf(items, null))
        assertEquals(0, V5OverviewGroupRules.indexOf(emptyList(), "1"))
    }

    @Test
    fun `close all counts and closes only the cards in front, never pinned ones`() {
        fun card(id: String, closable: Boolean = true) =
            V5TabCard(id = id, title = id, selected = false, preview = null, favicon = null, closable = closable)
        // The private tab of the other segment and a locked one are not cards here at all.
        val cards = listOf(card("a"), card("pinned", closable = false), card("b"))
        assertEquals(listOf("a", "b"), V5TabOverviewRules.closeAllIds(cards))
        assertEquals(emptyList<String>(), V5TabOverviewRules.closeAllIds(listOf(card("pinned", closable = false))))
        assertEquals(emptyList<String>(), V5TabOverviewRules.closeAllIds(emptyList()))
    }

    @Test
    fun `plus adds a private tab only where the profile can have one`() {
        assertTrue(V5TabOverviewRules.canAddTab(privateSegment = false, canOpenPrivate = false))
        assertTrue(V5TabOverviewRules.canAddTab(privateSegment = true, canOpenPrivate = true))
        assertFalse(V5TabOverviewRules.canAddTab(privateSegment = true, canOpenPrivate = false))
    }

    @Test
    fun `an empty grid says why, a search first`() {
        assertEquals(V5OverviewEmpty.NoMatches, V5TabOverviewRules.empty("news", privateSegment = true, canOpenPrivate = false))
        assertEquals(V5OverviewEmpty.NoPrivateTabs, V5TabOverviewRules.empty(" ", privateSegment = true, canOpenPrivate = true))
        assertEquals(V5OverviewEmpty.PrivateUnavailable, V5TabOverviewRules.empty("", privateSegment = true, canOpenPrivate = false))
        assertEquals(V5OverviewEmpty.NoTabs, V5TabOverviewRules.empty("", privateSegment = false, canOpenPrivate = true))
    }

    @Test
    fun `an open group is framed from its header to its last card`() {
        val items = V5OverviewGroupRules.items(listOf("1", "2", "3", "4", "5"), listOf(dinner, trip), searching = false)
        val frames = V5OverviewGroupRules.frames(items)
        assertEquals(listOf(V5GroupFrame(dinner, "group:g1", "3")), frames)
    }

    @Test
    fun `nothing is framed while searching or without open groups`() {
        val searching = V5OverviewGroupRules.items(listOf("1", "3"), listOf(dinner), searching = true)
        assertEquals(emptyList<V5GroupFrame>(), V5OverviewGroupRules.frames(searching))
        val folded = V5OverviewGroupRules.items(listOf("4", "5"), listOf(trip), searching = false)
        assertEquals(emptyList<V5GroupFrame>(), V5OverviewGroupRules.frames(folded))
    }

    @Test
    fun `cards enter outwards from the page's card, from the top when there is none`() {
        assertEquals(0, V5TabOverviewRules.entranceOrder(index = 4, selectedIndex = 4))
        assertEquals(2, V5TabOverviewRules.entranceOrder(index = 2, selectedIndex = 4))
        assertEquals(3, V5TabOverviewRules.entranceOrder(index = 7, selectedIndex = 4))
        assertEquals(5, V5TabOverviewRules.entranceOrder(index = 5, selectedIndex = -1))
    }
}
