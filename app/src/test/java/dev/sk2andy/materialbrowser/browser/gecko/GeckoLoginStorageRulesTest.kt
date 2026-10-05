package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.browser.credentials.CredentialPromptRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeckoLoginStorageRulesTest {
    @Test
    fun `gecko's origins become the one spelling the vault keys logins by`() {
        assertEquals("https://example.com", CredentialPromptRules.canonicalHttpsOrigin("https://Example.COM"))
        assertEquals("https://example.com", CredentialPromptRules.canonicalHttpsOrigin("https://example.com:443"))
        assertEquals("https://example.com:8443", CredentialPromptRules.canonicalHttpsOrigin("https://example.com:8443"))
        // Gecko reports IDN hosts in punycode; the vault keeps them in lower case.
        assertEquals("https://xn--e1afmkfd.xn--p1ai", CredentialPromptRules.canonicalHttpsOrigin("https://XN--E1AFMKFD.xn--p1ai"))
        assertNull(CredentialPromptRules.canonicalHttpsOrigin("https://пример.рф"))
        assertNull(CredentialPromptRules.canonicalHttpsOrigin("http://example.com"))
    }

    @Test
    fun `a saved login keeps its page origin and drops what it cannot trust`() {
        val draft = GeckoLoginStorageRules.draft(
            origin = "https://Accounts.Example.com",
            formActionOrigin = "http://example.com",
            httpRealm = "",
            username = null,
            password = "hunter2",
        )

        assertEquals("https://accounts.example.com", draft?.origin)
        assertNull("a form posting over http keeps no action origin", draft?.formActionOrigin)
        assertNull(draft?.httpRealm)
        assertEquals("", draft?.username)
        assertEquals("https://example.com", GeckoLoginStorageRules.draft("https://example.com", "https://example.com", null, "a", "p")?.formActionOrigin)
    }

    @Test
    fun `logins from plain http pages or without a password are not saved`() {
        assertNull(GeckoLoginStorageRules.draft("http://example.com", null, null, "alice", "hunter2"))
        assertNull(GeckoLoginStorageRules.draft("https://example.com", null, null, "alice", null))
        assertNull(GeckoLoginStorageRules.draft("https://example.com", null, null, "alice", ""))
        assertNull(GeckoLoginStorageRules.draft(null, null, null, "alice", "hunter2"))
    }
}
