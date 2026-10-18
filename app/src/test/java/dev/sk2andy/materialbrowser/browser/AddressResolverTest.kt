package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressResolverTest {
    @Test
    fun `keeps complete web urls`() {
        assertEquals("https://example.com/path?q=1", AddressResolver.resolve("https://example.com/path?q=1"))
        assertEquals("http://localhost:8080", AddressResolver.resolve("http://localhost:8080"))
    }

    @Test
    fun `adds https to host-like inputs`() {
        assertEquals("https://example.com/news", AddressResolver.resolve("example.com/news"))
        assertEquals("https://example.com?q=1", AddressResolver.resolve("example.com?q=1"))
        assertEquals("https://example.com#top", AddressResolver.resolve("example.com#top"))
        assertEquals("https://localhost:8080", AddressResolver.resolve("localhost:8080"))
        assertEquals("https://127.0.0.1:3000/path", AddressResolver.resolve("127.0.0.1:3000/path"))
    }

    @Test
    fun `words with a dot that are not domains are searches`() {
        // File names, version numbers and names written with a dot are not addresses.
        listOf("file.txt", "node.js", "setup.exe", "image.jpg", "notes.pdf", "Dr.Who", "python3.12").forEach { input ->
            assertTrue(input, AddressResolver.isSearchQuery(input))
            // Letters, digits and dots stay as they are in a query.
            assertEquals(input, "https://www.google.com/search?q=$input", AddressResolver.resolve(input))
        }
    }

    @Test
    fun `real top-level domains still open as sites`() {
        assertEquals("https://example.com", AddressResolver.resolve("example.com"))
        assertEquals("https://ya.ru", AddressResolver.resolve("ya.ru"))
        assertEquals("https://example.dev/path", AddressResolver.resolve("example.dev/path"))
        assertEquals("https://example.app?x=1", AddressResolver.resolve("example.app?x=1"))
        assertEquals("https://EXAMPLE.COM", AddressResolver.resolve("EXAMPLE.COM"))
        assertEquals("https://readme.md", AddressResolver.resolve("readme.md"))
    }

    @Test
    fun `names of private networks open as sites`() {
        assertEquals("https://router.local", AddressResolver.resolve("router.local"))
        assertEquals("https://homeassistant.local:8123", AddressResolver.resolve("homeassistant.local:8123"))
        assertEquals("https://nas.lan/admin", AddressResolver.resolve("nas.lan/admin"))
    }

    @Test
    fun `normalizes international host names`() {
        assertEquals("https://xn--mnchen-3ya.de", AddressResolver.resolve("münchen.de"))
    }

    @Test
    fun `opens sites under international top-level domains`() {
        assertEquals("https://xn--c1aapkosapc.xn--p1ai", AddressResolver.resolve("госуслуги.рф"))
        assertEquals("https://xn--e1afmkfd.xn--80asehdb/news", AddressResolver.resolve("пример.онлайн/news"))
        assertFalse(AddressResolver.isSearchQuery("xn--c1aapkosapc.xn--p1ai"))
    }

    @Test
    fun `a search suggestion stays a search whatever it looks like`() {
        val forced = { input: String -> AddressResolver.resolve(input, SearchEngine.Google, forceSearch = true) }
        assertEquals("https://www.google.com/search?q=example.com", forced(" example.com "))
        assertTrue(forced("intent://x#Intent;end").startsWith("https://www.google.com/search?q=intent"))
        assertTrue(forced("https://evil.example").startsWith("https://www.google.com/search?q="))
        assertEquals(BLANK_URL, forced("   "))
    }

    @Test
    fun `uses google for plain text and forbidden schemes`() {
        assertEquals(
            "https://www.google.com/search?q=material%20browser",
            AddressResolver.resolve("material browser"),
        )
        assertTrue(AddressResolver.resolve("javascript://alert(1)").startsWith("https://www.google.com/search?q="))
        assertTrue(AddressResolver.resolve("file:///data/local/tmp/x").startsWith("https://www.google.com/search?q="))
    }

    @Test
    fun `uses selected search engine for plain text and forbidden schemes`() {
        assertEquals(
            "https://duckduckgo.com/?q=privacy%20browser",
            AddressResolver.resolve("privacy browser", SearchEngine.DuckDuckGo),
        )
        assertTrue(
            AddressResolver.resolve("javascript://alert(1)", SearchEngine.Brave)
                .startsWith("https://search.brave.com/search?q="),
        )
    }

    @Test
    fun `selected search engine does not change direct address handling`() {
        assertEquals(
            "https://example.com/news",
            AddressResolver.resolve("example.com/news", SearchEngine.Perplexity),
        )
    }

    @Test
    fun `uses configured searxng instance for plain text`() {
        assertEquals(
            "http://localhost:8080/searxng/search?q=privacy%20browser",
            AddressResolver.resolve(
                input = "privacy browser",
                searchEngine = SearchEngine.SearXNG,
                searxngInstanceUrl = "http://localhost:8080/searxng",
            ),
        )
    }

    @Test
    fun `ai mode changes only search query resolution`() {
        assertEquals(
            "https://www.google.com/ai?q=why%20is%20the%20sky%20blue",
            AddressResolver.resolve(
                input = "why is the sky blue",
                searchEngine = SearchEngine.Google,
                searchMode = SearchMode.Ai,
            ),
        )
        assertEquals(
            "https://example.com/news",
            AddressResolver.resolve(
                input = "example.com/news",
                searchEngine = SearchEngine.Google,
                searchMode = SearchMode.Ai,
            ),
        )
    }

    @Test
    fun `search query classification matches resolution policy`() {
        assertTrue(AddressResolver.isSearchQuery("why is the sky blue"))
        assertTrue(AddressResolver.isSearchQuery("javascript://alert(1)"))
        assertFalse(AddressResolver.isSearchQuery("example.com/news"))
        assertFalse(AddressResolver.isSearchQuery("https://example.com"))
        assertFalse(AddressResolver.isSearchQuery("  "))
    }

    @Test
    fun `empty input resolves to blank tab`() {
        assertEquals(BLANK_URL, AddressResolver.resolve("  "))
    }

    @Test
    fun `displays host when tracking query contains uri-invalid characters`() {
        assertEquals(
            "centerparcs.de",
            AddressResolver.displayText(
                "https://www.centerparcs.de/?gclsrc=aw.ds&adlgid=mlg|center%20parcs|793940555",
            ),
        )
    }

    @Test
    fun `display text keeps invalid values unchanged`() {
        assertEquals("not a url", AddressResolver.displayText("not a url"))
    }
}
