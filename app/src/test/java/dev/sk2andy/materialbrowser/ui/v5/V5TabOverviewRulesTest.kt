package dev.sk2andy.materialbrowser.ui.v5

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
}
