package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeckoDownloadRetryRulesTest {
    @Test
    fun `web addresses are fetched again`() {
        assertEquals(DownloadRetry.Available, GeckoDownloadRetryRules.offer("https://example.com/a.pdf", null))
        assertEquals(DownloadRetry.Available, GeckoDownloadRetryRules.offer("HTTP://example.com/a.pdf", null))
    }

    @Test
    fun `a blob is gone with its page, which is offered instead`() {
        assertEquals(
            DownloadRetry.Unavailable(DownloadRetryUnavailable.Blob, "https://example.com/editor"),
            GeckoDownloadRetryRules.offer("blob:https://example.com/7f3c", "https://example.com/editor"),
        )
    }

    @Test
    fun `other addresses are not fetched again`() {
        assertEquals(
            DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered),
            GeckoDownloadRetryRules.offer("data:text/plain,hello", "https://example.com/"),
        )
    }

    @Test
    fun `a retry saves only the same file`() {
        val expected = GeckoDownloadTransferMetadata("report.pdf", "application/pdf", "https://example.com/r")
        assertTrue(GeckoDownloadRetryRules.isSameFile(expected, expected.copy(sourceUrl = "https://cdn.example/r")))
        assertFalse(
            "a POST answered by GET with a page",
            GeckoDownloadRetryRules.isSameFile(expected, expected.copy(fileName = "report.html", mimeType = "text/html")),
        )
        assertFalse(GeckoDownloadRetryRules.isSameFile(expected, expected.copy(fileName = "other.pdf")))
    }

    @Test
    fun `a closed private tab names no page`() {
        assertEquals(
            DownloadRetry.Unavailable(DownloadRetryUnavailable.PrivateClosed, pageUrl = null),
            GeckoDownloadRetryRules.privateClosed,
        )
    }
}
