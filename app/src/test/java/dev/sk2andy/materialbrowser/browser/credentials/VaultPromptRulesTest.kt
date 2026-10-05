package dev.sk2andy.materialbrowser.browser.credentials

import dev.sk2andy.materialbrowser.browser.credentials.VaultPromptRules.SaveQuestion
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class VaultPromptRulesTest {
    private val hints = listOf(
        hint("1", ORIGIN, "alice", lastUsed = 10),
        hint("2", ORIGIN, "bob", lastUsed = 30),
        hint("3", "https://accounts.example.com", "carol"),
        hint("4", ORIGIN, "realm-user", realm = "Staff"),
    )

    @Test
    fun `only accounts of exactly this site are offered, last used first`() {
        val accounts = VaultPromptRules.accounts(hints, ORIGIN, allowedUserIds = emptySet())

        assertEquals(listOf("bob", "alice"), accounts.map(VaultLoginAccount::username))
        assertEquals(
            listOf("alice"),
            VaultPromptRules.accounts(hints, ORIGIN, setOf("alice", "carol")).map(VaultLoginAccount::username),
        )
        assertEquals(emptyList<VaultLoginAccount>(), VaultPromptRules.accounts(hints, "https://example.org", emptySet()))
    }

    @Test
    fun `a locked vault asks to update a known user and to save a new one`() {
        assertEquals(SaveQuestion.Update, VaultPromptRules.saveQuestion(hints, null, ORIGIN, login("alice", "x")))
        assertEquals(SaveQuestion.Save, VaultPromptRules.saveQuestion(hints, null, ORIGIN, login("dave", "x")))
        assertEquals(
            SaveQuestion.Save,
            VaultPromptRules.saveQuestion(hints, null, "https://accounts.example.com", login("alice", "x")),
        )
    }

    @Test
    fun `an open vault asks nothing for the same password`() {
        val open = listOf(saved("alice", "hunter2"))

        assertEquals(SaveQuestion.None, VaultPromptRules.saveQuestion(hints, open, ORIGIN, login("alice", "hunter2")))
        assertEquals(SaveQuestion.Update, VaultPromptRules.saveQuestion(hints, open, ORIGIN, login("alice", "new")))
        assertEquals(SaveQuestion.Save, VaultPromptRules.saveQuestion(hints, open, ORIGIN, login("bob", "x")))
    }

    @Test
    fun `the picked login comes from the open vault, never from a realm login`() {
        val logins = listOf(saved("alice", "hunter2"), saved("bob", "pw", realm = "Staff"))

        assertEquals("hunter2", VaultPromptRules.login(logins, "alice")?.password)
        assertNull(VaultPromptRules.login(logins, "bob"))
    }

    @Test
    fun `the site reads as the Passwords screen shows it and an account prints no name`() {
        assertEquals("mail.example.com", VaultPromptRules.displaySite("https://mail.example.com"))
        assertEquals("xn--80ak6aa92e.com:8443", VaultPromptRules.displaySite("https://xn--80ak6aa92e.com:8443"))
        assertFalse("alice" in VaultLoginAccount("alice", null).toString())
        assertFalse("alice" in VaultLoginAnswer.Pick("alice").toString())
    }

    private fun login(username: String, password: String) = CredentialLogin(username, password)

    private fun hint(id: String, origin: String, username: String, lastUsed: Long? = null, realm: String? = null) =
        VaultLoginHint(id, origin, null, realm, username, lastUsed)

    private fun saved(username: String, password: String, realm: String? = null) = VaultLogin(
        id = "id-$username",
        origin = ORIGIN,
        formActionOrigin = null,
        httpRealm = realm,
        username = username,
        password = password,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )

    private companion object {
        const val ORIGIN = "https://example.com"
    }
}
