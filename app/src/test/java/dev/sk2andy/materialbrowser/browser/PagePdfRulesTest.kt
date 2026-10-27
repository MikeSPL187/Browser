package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagePdfRulesTest {
    @Test
    fun `the page title names the file`() {
        assertEquals("Northern lights in March.pdf", PagePdfRules.fileName("Northern lights in March"))
    }

    @Test
    fun `characters a file name cannot hold become single spaces`() {
        assertEquals("News a b c.pdf", PagePdfRules.fileName("News: a/b\\c"))
        assertEquals("Tab title.pdf", PagePdfRules.fileName("  Tab\n\ttitle  "))
    }

    @Test
    fun `an empty or dotted title falls back to page`() {
        assertEquals("page.pdf", PagePdfRules.fileName(""))
        assertEquals("page.pdf", PagePdfRules.fileName(" ... "))
    }

    @Test
    fun `a long title is cut before the extension`() {
        val name = PagePdfRules.fileName("x".repeat(300))
        assertTrue(name.endsWith(".pdf"))
        assertEquals(80 + ".pdf".length, name.length)
    }
}
