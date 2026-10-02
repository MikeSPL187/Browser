package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TabCardDescriptionRulesTest {
    @Test
    fun `site is the web host without www, and nothing for other pages`() {
        assertEquals("example.com", TabCardDescriptionRules.site("https://www.Example.com/path?q=1"))
        assertEquals("news.site.org", TabCardDescriptionRules.site(" http://news.site.org "))
        assertNull(TabCardDescriptionRules.site(""))
        assertNull(TabCardDescriptionRules.site("about:blank"))
        assertNull(TabCardDescriptionRules.site("file:///sdcard/page.html"))
        assertNull(TabCardDescriptionRules.site("not a url"))
    }

    @Test
    fun `a card reads its title and then its site, without saying the site twice`() {
        assertEquals(
            listOf("Weather in Tallinn", "weather.com"),
            TabCardDescriptionRules.parts("Weather in Tallinn", "https://weather.com/today"),
        )
        assertEquals(
            listOf("example.com"),
            TabCardDescriptionRules.parts("example.com", "https://www.example.com/"),
        )
        assertEquals(listOf("New tab"), TabCardDescriptionRules.parts(" New tab ", ""))
        assertEquals(listOf("site.org"), TabCardDescriptionRules.parts("", "https://site.org/"))
    }

    @Test
    fun `parts join with commas, as TalkBack pauses on them`() {
        assertEquals(
            "Mail, mail.example.com, current tab",
            TabCardDescriptionRules.join(listOf("Mail", "mail.example.com", "current tab")),
        )
    }
}
