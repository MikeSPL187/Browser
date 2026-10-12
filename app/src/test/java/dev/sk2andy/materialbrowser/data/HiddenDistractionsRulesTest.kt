package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenDistractionsRulesTest {
    @Test
    fun `a site is the host without www, in lower case`() {
        assertEquals("news.example", HiddenDistractionsRules.siteOf("WWW.News.Example."))
        assertEquals("m.news.example", HiddenDistractionsRules.siteOf("m.news.example"))
        assertNull(HiddenDistractionsRules.siteOf(""))
        assertNull(HiddenDistractionsRules.siteOf("evil host"))
        assertNull(HiddenDistractionsRules.siteOf(null))
    }

    @Test
    fun `only the selectors the page script writes are kept`() {
        assertTrue(HiddenDistractionsRules.isValidSelector("#promo"))
        assertTrue(HiddenDistractionsRules.isValidSelector("#main > aside.sidebar > div.card:nth-of-type(2)"))
        assertFalse(HiddenDistractionsRules.isValidSelector("div{color:red}"))
        assertFalse(HiddenDistractionsRules.isValidSelector("a[href]"))
        assertFalse(HiddenDistractionsRules.isValidSelector("div;"))
        assertFalse(HiddenDistractionsRules.isValidSelector(" "))
        assertFalse(HiddenDistractionsRules.isValidSelector("d".repeat(HiddenDistractionsRules.MAX_SELECTOR_LENGTH + 1)))
    }

    @Test
    fun `hiding the same block twice keeps one, newest last`() {
        var state = HiddenDistractions()
        state = HiddenDistractionsRules.hide(state, "www.news.example", "#a")
        state = HiddenDistractionsRules.hide(state, "news.example", "#b")
        state = HiddenDistractionsRules.hide(state, "news.example", "#a")

        assertEquals(listOf("#b", "#a"), state.selectorsFor("news.example"))
        assertEquals(listOf("#b", "#a"), state.selectorsFor("www.news.example"))
    }

    @Test
    fun `a site keeps its newest blocks and the least recently changed site goes first`() {
        var state = HiddenDistractions()
        repeat(HiddenDistractionsRules.MAX_SELECTORS_PER_SITE + 2) { index ->
            state = HiddenDistractionsRules.hide(state, "news.example", "#b$index")
        }
        assertEquals(HiddenDistractionsRules.MAX_SELECTORS_PER_SITE, state.selectorsFor("news.example").size)
        assertEquals("#b2", state.selectorsFor("news.example").first())

        repeat(HiddenDistractionsRules.MAX_SITES) { index ->
            state = HiddenDistractionsRules.hide(state, "site$index.example", "#ad")
        }
        assertEquals(HiddenDistractionsRules.MAX_SITES, state.sites.size)
        assertTrue(state.selectorsFor("news.example").isEmpty())
        assertEquals("site199.example", state.sites.keys.last())
    }

    @Test
    fun `refused input leaves the list as it was`() {
        val state = HiddenDistractionsRules.hide(HiddenDistractions(), "news.example", "#a")

        assertSame(state, HiddenDistractionsRules.hide(state, "news.example", "a[href]"))
        assertSame(state, HiddenDistractionsRules.hide(state, null, "#b"))
        assertSame(state, HiddenDistractionsRules.showAll(state, "other.example"))
    }

    @Test
    fun `show all forgets one site only`() {
        var state = HiddenDistractionsRules.hide(HiddenDistractions(), "news.example", "#a")
        state = HiddenDistractionsRules.hide(state, "blog.example", "#b")

        state = HiddenDistractionsRules.showAll(state, "www.news.example")

        assertEquals(mapOf("blog.example" to listOf("#b")), state.sites)
    }

    @Test
    fun `round trip keeps sites, blocks and their order`() {
        var state = HiddenDistractionsRules.hide(HiddenDistractions(), "news.example", "#a")
        state = HiddenDistractionsRules.hide(state, "blog.example", "#b")
        state = HiddenDistractionsRules.hide(state, "news.example", "div.promo")

        val decoded = HiddenDistractionsCodec.decode(HiddenDistractionsCodec.encode(state))

        assertEquals(state, decoded)
        assertEquals(listOf("blog.example", "news.example"), decoded.sites.keys.toList())
    }

    @Test
    fun `unreadable stored lists are skipped`() {
        assertEquals(HiddenDistractions(), HiddenDistractionsCodec.decode(null))
        assertEquals(HiddenDistractions(), HiddenDistractionsCodec.decode("not json"))
        assertEquals(
            mapOf("news.example" to listOf("#a")),
            HiddenDistractionsCodec.decode(
                """{"sites":[{"h":"news.example","s":["#a","x{}"]},{"h":"bad host","s":["#b"]},{"s":["#c"]},7]}""",
            ).sites,
        )
    }
}
