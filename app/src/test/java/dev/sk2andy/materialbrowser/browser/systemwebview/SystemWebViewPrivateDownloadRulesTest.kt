package dev.sk2andy.materialbrowser.browser.systemwebview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemWebViewPrivateDownloadRulesTest {
    @Test
    fun `a redirect resolves against the current address`() {
        assertEquals(
            "https://cdn.example.test/file.zip",
            SystemWebViewPrivateDownloadRules.nextHop(
                "https://example.test/get?id=1",
                "https://cdn.example.test/file.zip",
            ),
        )
        assertEquals(
            "https://example.test/files/file.zip",
            SystemWebViewPrivateDownloadRules.nextHop("https://example.test/get/1", "/files/file.zip"),
        )
    }

    @Test
    fun `a redirect never downgrades, carries credentials or leaves the web`() {
        assertNull(SystemWebViewPrivateDownloadRules.nextHop("https://example.test/a", "http://example.test/a"))
        assertNull(SystemWebViewPrivateDownloadRules.nextHop("https://example.test/a", "https://u:p@example.test/a"))
        assertNull(SystemWebViewPrivateDownloadRules.nextHop("https://example.test/a", "intent://x#Intent;end"))
        assertNull(SystemWebViewPrivateDownloadRules.nextHop("https://example.test/a", null))
        assertEquals(
            "https://example.test/a",
            SystemWebViewPrivateDownloadRules.nextHop("http://example.test/a", "https://example.test/a"),
        )
    }
}
