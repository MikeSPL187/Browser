package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserTab
import org.junit.Assert.assertEquals
import org.junit.Test

class NewTabContinueRulesTest {
    @Test
    fun `a page with a title shows it`() {
        assertEquals("F-Droid", NewTabContinueRules.title(tab(title = "F-Droid", url = "https://f-droid.org/")))
    }

    @Test
    fun `a page without a title shows its site instead of New tab`() {
        assertEquals("f-droid.org", NewTabContinueRules.title(tab(title = "", url = "https://f-droid.org/packages/")))
        assertEquals("example.com", NewTabContinueRules.title(tab(title = "  ", url = "https://www.example.com/a")))
    }

    private fun tab(title: String, url: String) = BrowserTab("a", 0L, title = title, url = url)
}
