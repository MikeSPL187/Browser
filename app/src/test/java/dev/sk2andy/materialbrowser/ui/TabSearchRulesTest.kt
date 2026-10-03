package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabSearchRulesTest {
    @Test
    fun `every word must appear in the title or the address, in any case`() {
        val title = "Lake Baikal in winter"
        val url = "https://travel.example.org/"
        assertTrue(TabSearchRules.matches(title, url, "baikal"))
        assertTrue(TabSearchRules.matches(title, url, "WINTER travel"))
        assertTrue(
            TabSearchRules.matches("Спринт 42", "https://tasks.example.com/", "спринт tasks"),
        )
        assertFalse(TabSearchRules.matches(title, url, "baikal summer"))
    }

    @Test
    fun `a blank query keeps every tab in order`() {
        val tabs = listOf("b" to "https://b.org", "a" to "https://a.org")
        assertEquals(tabs, TabSearchRules.filter(tabs, null, { it.first }, { it.second }))
        assertEquals(tabs, TabSearchRules.filter(tabs, "  ", { it.first }, { it.second }))
        assertTrue(TabSearchRules.matches("Anything", "https://x.org", ""))
    }

    @Test
    fun `filtering keeps the overview order`() {
        val tabs = listOf(
            "Mail" to "https://mail.example.com",
            "Docs" to "https://docs.example.com",
            "Maps" to "https://maps.example.org",
        )
        assertEquals(
            listOf(tabs[0], tabs[1]),
            TabSearchRules.filter(tabs, "example.com", { it.first }, { it.second }),
        )
    }
}
