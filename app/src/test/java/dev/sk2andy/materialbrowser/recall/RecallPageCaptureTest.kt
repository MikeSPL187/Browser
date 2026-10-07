package dev.sk2andy.materialbrowser.recall

import dev.sk2andy.materialbrowser.browser.BrowserTab
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecallPageCaptureTest {
    private val tab = BrowserTab(id = "t1", lastAccessedAt = 0, profileId = "personal")
    private val identity = RecallExtractionIdentity("t1", "personal", "https://example.com/page", 3)

    @Test
    fun `a regular tab on a web page is remembered`() {
        assertEquals(identity, RecallPageCapture.identity(tab, "https://example.com/page#top", 3, allowed = true))
    }

    @Test
    fun `private tabs, Recall off, other pages and unknown navigations are not`() {
        assertNull(RecallPageCapture.identity(tab.copy(isIncognito = true), "https://example.com/page", 3, true))
        assertNull(RecallPageCapture.identity(tab, "https://example.com/page", 3, allowed = false))
        assertNull(RecallPageCapture.identity(tab, "about:blank", 3, true))
        assertNull(RecallPageCapture.identity(tab, "https://example.com/page", null, true))
        assertNull(RecallPageCapture.identity(null, "https://example.com/page", 3, true))
    }

    @Test
    fun `the article blocks become the document text`() {
        val document = RecallPageCapture.document(reader(blocks = listOf("Heading", "First paragraph.")), identity, 7)
        assertEquals("https://example.com/page", document?.url)
        assertEquals("Example", document?.title)
        assertEquals("Heading First paragraph.", document?.text)
        assertEquals(7L, document?.visitedAt)
    }

    @Test
    fun `a page without an article falls back to its visible text`() {
        val document = RecallPageCapture.document(reader(blocks = emptyList(), visibleText = "Visible words"), identity, 7)
        assertEquals("Visible words", document?.text)
    }

    @Test
    fun `another page, an error or no text is dropped`() {
        assertNull(RecallPageCapture.document(reader(url = "https://example.com/other", blocks = listOf("x y")), identity, 7))
        assertNull(RecallPageCapture.document("""{"error":"missing-root"}""", identity, 7))
        assertNull(RecallPageCapture.document(reader(blocks = emptyList()), identity, 7))
        assertNull(RecallPageCapture.document("not json", identity, 7))
        assertNull(RecallPageCapture.document(null, identity, 7))
    }

    private fun reader(
        url: String = "https://example.com/page#top",
        blocks: List<String>,
        visibleText: String = "",
    ): String = JSONObject()
        .put("title", "Example")
        .put("sourceUrl", url)
        .put("visibleText", visibleText)
        .put("blocks", JSONArray(blocks.map { text -> JSONObject().put("kind", "paragraph").put("text", text) }))
        .toString()
}
