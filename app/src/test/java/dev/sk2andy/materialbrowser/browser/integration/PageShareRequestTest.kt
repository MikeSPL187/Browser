package dev.sk2andy.materialbrowser.browser.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageShareRequestTest {
    @Test
    fun createsRequestForNormalizedWebUrl() {
        val request = PageShareRequest.create(
            url = " https://example.com/article ",
            title = " Example article ",
        )

        assertEquals("https://example.com/article", request?.url)
        assertEquals("Example article", request?.title)
    }

    @Test
    fun acceptsBlankTitleAndRejectsNonWebUrl() {
        val request = PageShareRequest.create(
            url = "https://example.com",
            title = "   ",
        )

        assertEquals("", request?.title)
        assertNull(PageShareRequest.create(url = "about:blank", title = "New tab"))
    }

    @Test
    fun webShareKeepsTextBeforeTheLink() {
        val request = PageShareRequest.createWebShare(
            url = "https://example.com/article",
            title = "Article",
            text = " Read this ",
        )

        assertEquals("Read this\nhttps://example.com/article", request?.sharedText)
        assertEquals("Article", request?.title)
    }

    @Test
    fun webShareAcceptsTextOnlyAndTitleOnly() {
        assertEquals(
            "Just text",
            PageShareRequest.createWebShare(url = null, title = null, text = "Just text")?.sharedText,
        )
        val titleOnly = PageShareRequest.createWebShare(url = null, title = "Title", text = null)
        assertEquals("Title", titleOnly?.sharedText)
        assertNull(titleOnly?.url)
    }

    @Test
    fun webShareRejectsUnsafeLinksAndEmptyShares() {
        assertNull(PageShareRequest.createWebShare(url = "javascript:alert(1)", title = null, text = "x"))
        assertNull(PageShareRequest.createWebShare(url = null, title = " ", text = ""))
    }
}
