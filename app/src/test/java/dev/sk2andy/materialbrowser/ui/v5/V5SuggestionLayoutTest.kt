package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.browser.commands.BrowserCommand
import dev.sk2andy.materialbrowser.browser.commands.BrowserCommandKind
import dev.sk2andy.materialbrowser.browser.commands.CommandSuggestion
import dev.sk2andy.materialbrowser.data.AddressSuggestion
import dev.sk2andy.materialbrowser.data.AddressSuggestionSource
import dev.sk2andy.materialbrowser.recall.RecallMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V5SuggestionLayoutTest {
    private val history = AddressSuggestionItem.Navigation(
        AddressSuggestion("https://polar-notes.ru/noch", "Полярная ночь"),
    )
    private val favorite = AddressSuggestionItem.Navigation(
        AddressSuggestion("https://polka-knig.ru", "Полка", source = AddressSuggestionSource.Favorite),
    )
    private val openTab = AddressSuggestionItem.Navigation(
        AddressSuggestion("https://polar-notes.ru", "Полярные заметки", openTabId = "tab-1"),
    )
    private val recall = AddressSuggestionItem.Recall(
        RecallMatch("personal", "https://example.com", "Example", "полярное", 1L, 1.0),
    )
    private val search = AddressSuggestionItem.Search("полярное сияние")
    private val command = AddressSuggestionItem.Command(
        CommandSuggestion(BrowserCommand("reload", BrowserCommandKind.Reload), "Reload", "Reloads"),
    )

    @Test
    fun `an open tab is the best match, then the groups go up from the field`() {
        val layout = V5SuggestionLayoutRules.layout(
            listOf(command, recall, history, favorite, search, openTab),
            query = "пол",
        )
        assertEquals(openTab, layout.best)
        assertEquals(
            listOf(
                V5SuggestionGroup.Favorites,
                V5SuggestionGroup.History,
                V5SuggestionGroup.PageMemory,
                V5SuggestionGroup.Search,
                V5SuggestionGroup.Commands,
            ),
            layout.sections.map { it.group },
        )
        assertTrue(layout.sections.none { openTab in it.items })
    }

    @Test
    fun `without an open tab a library page, then page memory is best, never a search`() {
        assertEquals(history, V5SuggestionLayoutRules.best(listOf(search, history, favorite), "пол"))
        assertEquals(recall, V5SuggestionLayoutRules.best(listOf(search, recall), "пол"))
        assertNull(V5SuggestionLayoutRules.best(listOf(search, command), "пол"))
    }

    @Test
    fun `nothing typed means no best match`() {
        val layout = V5SuggestionLayoutRules.layout(listOf(openTab, history), query = " ")
        assertNull(layout.best)
        assertEquals(listOf(V5SuggestionGroup.OpenTabs, V5SuggestionGroup.History), layout.sections.map { it.group })
    }

    @Test
    fun `groups keep the order the suggestions came in`() {
        val other = AddressSuggestionItem.Navigation(AddressSuggestion("https://a.ru", "A"))
        val layout = V5SuggestionLayoutRules.layout(listOf(history, other, openTab), query = "a")
        assertEquals(listOf(history, other), layout.sections.single().items)
        assertTrue(V5SuggestionLayoutRules.layout(emptyList(), "a").isEmpty)
    }

    @Test
    fun `the typed text is found regardless of case`() {
        assertEquals(0..2, V5SuggestionLayoutRules.match("Полярные заметки", "пол"))
        assertEquals(9..14, V5SuggestionLayoutRules.match("Полярные заметки", " ЗАМЕТК "))
        assertNull(V5SuggestionLayoutRules.match("Полярные заметки", "сияние"))
        assertNull(V5SuggestionLayoutRules.match("Полярные заметки", ""))
    }

    @Test
    fun `arrow keys walk from the field upwards and back`() {
        val layout = V5SuggestionLayoutRules.layout(listOf(history, search, openTab), query = "пол")
        assertEquals(listOf(openTab, history, search), layout.fromField)
        assertEquals(openTab, V5SuggestionLayoutRules.step(layout, null, 1))
        assertEquals(history, V5SuggestionLayoutRules.step(layout, openTab.stableId, 1))
        assertEquals(openTab, V5SuggestionLayoutRules.step(layout, history.stableId, -1))
        assertNull(V5SuggestionLayoutRules.step(layout, openTab.stableId, -1))
        assertNull(V5SuggestionLayoutRules.step(layout, search.stableId, 1))
        assertNull(V5SuggestionLayoutRules.step(layout, null, -1))
    }

    @Test
    fun `arrow keys over an empty list go nowhere`() {
        assertNull(V5SuggestionLayoutRules.step(V5SuggestionLayout.Empty, null, 1))
        assertNull(V5SuggestionLayoutRules.step(V5SuggestionLayout.Empty, "gone", -1))
    }

    @Test
    fun `a highlight that left the list starts the walk again`() {
        val layout = V5SuggestionLayoutRules.layout(listOf(history, search), query = "пол")
        assertEquals(history, V5SuggestionLayoutRules.step(layout, "navigation:gone", 1))
    }

    @Test
    fun `a repeated key keeps only its first row, so the list never sees it twice`() {
        val again = AddressSuggestionItem.Recall(
            RecallMatch("personal", "https://example.com", "Example again", "полярное", 2L, 0.5),
        )
        val layout = V5SuggestionLayoutRules.layout(listOf(history, recall, again), query = "пол")
        val keys = (listOfNotNull(layout.best) + layout.sections.flatMap { it.items }).map { it.stableId }
        assertEquals(keys.distinct(), keys)
        assertTrue(layout.sections.flatMap { it.items }.contains(recall))
    }

    @Test
    fun `over a page the typed text can be found on it, farthest from the field`() {
        val layout = V5SuggestionLayoutRules.layout(listOf(history, search), query = " пол ", pageUrl = "https://polar-notes.ru/noch")
        assertEquals("пол", layout.findOnPage)
    }

    @Test
    fun `find on page alone still makes a list`() {
        val layout = V5SuggestionLayoutRules.layout(emptyList(), query = "пол", pageUrl = "https://polar-notes.ru/noch")
        assertEquals(false, layout.isEmpty)
    }

    @Test
    fun `nothing to find without a page, without text, in the page's own address or in a command`() {
        val page = "https://polar-notes.ru/noch"
        assertNull(V5SuggestionLayoutRules.findOnPage("пол", pageUrl = null))
        assertNull(V5SuggestionLayoutRules.findOnPage("  ", pageUrl = page))
        assertNull(V5SuggestionLayoutRules.findOnPage(page, pageUrl = page))
        assertNull(V5SuggestionLayoutRules.findOnPage("> reload", pageUrl = page))
        assertNull(V5SuggestionLayoutRules.layout(listOf(history), query = "пол").findOnPage)
    }
}
