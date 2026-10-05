package dev.sk2andy.materialbrowser.browser.safety

import dev.sk2andy.materialbrowser.blocking.SortedHostIndex
import java.util.concurrent.CompletableFuture
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreatHostListTest {
    private val index = SortedHostIndex.from(
        "# Header\nevil.example\nlogin.bank-help.top\nxn--pple-43d.com\n".toByteArray(),
    )
    private val list = ThreatHostList(CompletableFuture.completedFuture(index))

    @Test
    fun `a listed host and its subdomains are found`() {
        assertTrue(list.contains("evil.example"))
        assertTrue(list.contains("cdn.evil.example"))
        assertTrue(list.contains("EVIL.example."))
    }

    @Test
    fun `parents and neighbours of a listed subdomain are not`() {
        assertTrue(list.contains("login.bank-help.top"))
        assertFalse(list.contains("bank-help.top"))
        assertFalse(list.contains("example"))
        assertFalse(list.contains("good.example"))
    }

    @Test
    fun `an international name is looked up in punycode`() {
        assertTrue(list.contains("аpple.com"))
        assertFalse(list.contains("apple.com"))
    }

    @Test
    fun `nothing is listed while the list is still loading`() {
        val loading = ThreatHostList(CompletableFuture())
        assertFalse(loading.contains("evil.example"))
    }
}
