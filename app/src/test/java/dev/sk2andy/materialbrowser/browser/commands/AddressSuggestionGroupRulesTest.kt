package dev.sk2andy.materialbrowser.browser.commands

import dev.sk2andy.materialbrowser.data.AddressSuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressSuggestionGroupRulesTest {
    private val openTab = AddressSuggestionItem.Navigation(
        AddressSuggestion(url = "https://open.example/", title = "Open", openTabId = "tab"),
    )
    private val history = AddressSuggestionItem.Navigation(
        AddressSuggestion(url = "https://history.example/", title = "History"),
    )
    private val search = AddressSuggestionItem.Search("example search")
    private val secondSearch = AddressSuggestionItem.Search("example second")

    @Test
    fun `open tabs end up next to the field, below searches and the library`() {
        val ordered = AddressSuggestionGroupRules.displayOrder(
            listOf(openTab, history, search, secondSearch),
        )

        assertEquals(listOf(history, search, secondSearch, openTab), ordered)
    }

    @Test
    fun `a group keeps the composer's order inside it`() {
        val ordered = AddressSuggestionGroupRules.displayOrder(listOf(secondSearch, search))

        assertEquals(listOf(secondSearch, search), ordered)
    }

    @Test
    fun `dividers start every group but the first`() {
        val ordered = listOf(history, search, secondSearch, openTab)

        assertFalse(AddressSuggestionGroupRules.startsGroup(ordered, 0))
        assertTrue(AddressSuggestionGroupRules.startsGroup(ordered, 1))
        assertFalse(AddressSuggestionGroupRules.startsGroup(ordered, 2))
        assertTrue(AddressSuggestionGroupRules.startsGroup(ordered, 3))
        assertFalse(AddressSuggestionGroupRules.startsGroup(ordered, 4))
    }
}
