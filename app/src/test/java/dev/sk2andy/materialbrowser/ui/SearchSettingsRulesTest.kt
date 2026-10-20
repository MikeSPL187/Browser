package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.browser.suggestions.SearchSuggestionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSettingsRulesTest {
    @Test
    fun `a missing or unreadable SearXNG address is said under the field`() {
        assertEquals(R.string.toast_searxng_instance_required, SearchSettingsRules.searxngProblem(""))
        assertEquals(R.string.toast_searxng_instance_required, SearchSettingsRules.searxngProblem("   "))
        assertEquals(R.string.settings_searxng_instance_url_invalid, SearchSettingsRules.searxngProblem("search?q=1"))
        assertEquals(
            R.string.settings_searxng_instance_url_invalid,
            SearchSettingsRules.searxngProblem("https://search.example.org/?q=x"),
        )
        assertEquals(null, SearchSettingsRules.searxngProblem("https://search.example.org"))
    }

    @Test
    fun `every engine has its own description`() {
        val descriptions = SearchEngine.entries.map(SearchSettingsRules::description)

        assertEquals(SearchEngine.entries.size, descriptions.toSet().size)
    }

    @Test
    fun `neighbouring engines never share a tile color`() {
        SearchEngine.entries.indices.zipWithNext().forEach { (above, below) ->
            assertNotEquals(SearchSettingsRules.accent(above), SearchSettingsRules.accent(below))
        }
    }

    @Test
    fun `the tile shows the engine's first letter`() {
        assertEquals("D", SearchSettingsRules.letter(SearchEngine.DuckDuckGo))
        assertEquals("S", SearchSettingsRules.letter(SearchEngine.SearXNG))
    }

    @Test
    fun `the SearXNG server is asked for whenever SearXNG is used`() {
        assertTrue(
            SearchSettingsRules.showsSearxngServer(
                SearchEngine.SearXNG,
                SearchSuggestionProvider.None,
            ),
        )
        assertTrue(
            SearchSettingsRules.showsSearxngServer(
                SearchEngine.DuckDuckGo,
                SearchSuggestionProvider.SearXNG,
            ),
        )
        assertFalse(
            SearchSettingsRules.showsSearxngServer(
                SearchEngine.DuckDuckGo,
                SearchSuggestionProvider.DuckDuckGo,
            ),
        )
    }
}
