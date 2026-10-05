package dev.sk2andy.materialbrowser.shared.credentials

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialVaultRulesTest {
    @Test
    fun onlyTheCanonicalHttpsOriginIsAccepted() {
        assertTrue(CredentialVaultRules.isCanonicalOrigin("https://example.com"))
        assertTrue(CredentialVaultRules.isCanonicalOrigin("https://accounts.xn--80ak6aa92e.com:8443"))
        assertFalse(CredentialVaultRules.isCanonicalOrigin("http://example.com"))
        assertFalse(CredentialVaultRules.isCanonicalOrigin("https://Example.com"))
        assertFalse(CredentialVaultRules.isCanonicalOrigin("https://example.com/"))
        assertFalse(CredentialVaultRules.isCanonicalOrigin("https://example.com:443"))
        assertFalse(CredentialVaultRules.isCanonicalOrigin("https://user@example.com"))
        assertFalse(CredentialVaultRules.isCanonicalOrigin("https://пример.рф"))
        assertFalse(CredentialVaultRules.isCanonicalOrigin("https://-bad.example.com"))
        assertFalse(CredentialVaultRules.isCanonicalOrigin("https://example.com:0"))
    }

    @Test
    fun aLoginIsOfferedOnlyToItsExactOrigin() {
        val logins = listOf(
            login("1", "https://example.com", lastUsed = 10),
            login("2", "https://accounts.example.com"),
            login("3", "https://example.com", lastUsed = 20),
            login("4", "https://example.com:8443"),
        )

        assertEquals(listOf("3", "1"), CredentialVaultRules.loginsFor(logins, "https://example.com").map(VaultLogin::id))
        assertEquals(listOf("2"), CredentialVaultRules.loginsFor(logins, "https://accounts.example.com").map(VaultLogin::id))
    }

    @Test
    fun geckosDomainQueryFindsTheDomainAndItsSubdomainsOnly() {
        val logins = listOf(
            login("1", "https://example.com"),
            login("2", "https://accounts.example.com:8443"),
            login("3", "https://notexample.com"),
            login("4", "https://example.com.evil.net"),
        )

        assertEquals(listOf("1", "2"), CredentialVaultRules.loginsUnderDomain(logins, "Example.com.").map(VaultLogin::id))
        assertEquals(emptyList(), CredentialVaultRules.loginsUnderDomain(logins, "com/../x"))
    }

    @Test
    fun savingAddsUpdatesOrLeavesAlone() {
        val draft = draft(password = "first")
        val added = CredentialVaultRules.save(emptyList(), draft, nowMillis = 1) { "new" }
        val login = assertIs<VaultSaveResult.Added>(added.result).login
        assertEquals("new", login.id)

        val same = CredentialVaultRules.save(added.logins, draft, nowMillis = 2) { error("no new id") }
        assertIs<VaultSaveResult.Unchanged>(same.result)
        assertEquals(added.logins, same.logins)

        val changed = CredentialVaultRules.save(added.logins, draft(password = "second"), nowMillis = 3) { error("no new id") }
        val updated = assertIs<VaultSaveResult.Updated>(changed.result).login
        assertEquals("second", updated.password)
        assertEquals(1, updated.createdAtMillis)
        assertEquals(3, updated.updatedAtMillis)
        assertEquals(1, changed.logins.size)

        val otherUser = CredentialVaultRules.save(changed.logins, draft(username = "bob"), nowMillis = 4) { "bob" }
        assertIs<VaultSaveResult.Added>(otherUser.result)
        assertEquals(2, otherUser.logins.size)
    }

    @Test
    fun aHandEditChangesTheLoginButNeverDuplicatesAnother() {
        val alice = login("1", "https://example.com").copy(formActionOrigin = "https://example.com")
        val bob = login("2", "https://example.com").copy(username = "bob")
        val logins = listOf(alice, bob)

        val renamed = CredentialVaultRules.update(logins, "1", draft(username = "carol"), nowMillis = 9)
        val updated = assertIs<VaultSaveResult.Updated>(renamed.result).login
        assertEquals("carol", updated.username)
        assertEquals("https://example.com", updated.formActionOrigin)
        assertEquals(9, updated.updatedAtMillis)

        val moved = CredentialVaultRules.update(logins, "1", draft(origin = "https://other.example.com"), 9)
        assertNull(assertIs<VaultSaveResult.Updated>(moved.result).login.formActionOrigin)

        assertEquals(VaultSaveResult.Rejected, CredentialVaultRules.update(logins, "1", draft(username = "bob"), 9).result)
        assertEquals(VaultSaveResult.Rejected, CredentialVaultRules.update(logins, "9", draft(), 9).result)
        assertIs<VaultSaveResult.Unchanged>(CredentialVaultRules.update(logins, "1", draft(), 9).result)
    }

    @Test
    fun unsafeDraftsAreRejected() {
        fun rejected(draft: VaultLoginDraft) =
            CredentialVaultRules.save(emptyList(), draft, 1) { "x" }.result == VaultSaveResult.Rejected

        assertTrue(rejected(draft(origin = "http://example.com")))
        assertTrue(rejected(draft(password = "")))
        assertTrue(rejected(draft(password = "a\u0000b")))
        assertTrue(rejected(draft(username = "tab\there")))
        assertTrue(rejected(draft(password = "p".repeat(CredentialVaultRules.MAX_PASSWORD_LENGTH + 1))))
        assertTrue(rejected(draft().copy(formActionOrigin = "https://evil.example.com/path")))
        assertFalse(rejected(draft(username = "")))
    }

    @Test
    fun usingAndDeletingNeedAKnownLogin() {
        val logins = listOf(login("1", "https://example.com"))

        val used = CredentialVaultRules.markUsed(logins, "1", nowMillis = 50)
        assertEquals(50, used?.single()?.lastUsedAtMillis)
        assertEquals(1, used?.single()?.timesUsed)
        assertNull(CredentialVaultRules.markUsed(logins, "missing", 50))
        assertEquals(emptyList(), CredentialVaultRules.delete(logins, "1"))
        assertNull(CredentialVaultRules.delete(logins, "missing"))
    }

    @Test
    fun signingInWithAGeneratedPasswordNamesItsLogin() {
        val generated = CredentialVaultRules.save(emptyList(), draft(username = "", password = "Gen3rated!"), 1) { "g" }
        val logins = generated.logins

        val named = CredentialVaultRules.save(logins, draft(username = "anna", password = "Gen3rated!"), 2) { "new" }
        assertIs<VaultSaveResult.Updated>(named.result)
        assertEquals(listOf("g" to "anna"), named.logins.map { it.id to it.username })

        val other = CredentialVaultRules.save(logins, draft(username = "anna", password = "different"), 2) { "new" }
        assertIs<VaultSaveResult.Added>(other.result)
        assertEquals(2, other.logins.size)
    }

    @Test
    fun hintsCarryNoPasswordAndMatchLikeLogins() {
        val logins = listOf(
            login("1", "https://example.com", lastUsed = 10),
            login("2", "https://accounts.example.com"),
            login("3", "https://example.com", lastUsed = 20),
            login("4", "https://other.org"),
        )
        val hints = CredentialVaultRules.hints(logins)

        assertFalse("hunter2" in hints.joinToString { it.username + it.origin + it.id })
        assertEquals(listOf("3", "1"), CredentialVaultRules.hintsFor(hints, "https://example.com").map { it.id })
        assertEquals(
            setOf("1", "2", "3"),
            CredentialVaultRules.hintsUnderDomain(hints, "example.com").map { it.id }.toSet(),
        )
        assertEquals(emptyList(), CredentialVaultRules.hintsUnderDomain(hints, "not a host"))
        assertTrue(hints.all(CredentialVaultRules::accepts))
        assertFalse(CredentialVaultRules.accepts(hints.first().copy(id = " ")))
        assertFalse(CredentialVaultRules.accepts(hints.first().copy(origin = "https://Example.com")))
        assertFalse("alice" in hints.first().toString())
    }

    @Test
    fun aTwoFactorKeyIsKeptOnlyInItsCanonicalForm() {
        val logins = listOf(login("1", "https://example.com"))
        val key = TotpRules.canonical(TotpRules.parse("JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP")!!)

        val withKey = CredentialVaultRules.setTotp(logins, "1", key, nowMillis = 7)
        assertEquals(key, withKey?.single()?.totp)
        assertEquals(7, withKey?.single()?.updatedAtMillis)
        assertNull(CredentialVaultRules.setTotp(logins, "1", "otpauth://totp/Site:anna?secret=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP", 7))
        assertNull(CredentialVaultRules.setTotp(logins, "missing", key, 7))
        assertNull(CredentialVaultRules.setTotp(withKey!!, "1", null, 8)?.single()?.totp)

        val edited = CredentialVaultRules.update(withKey, "1", draft(password = "changed"), 9)
        assertEquals(key, edited.logins.single().totp)
        assertFalse("JBSWY3DP" in withKey.single().toString())
    }

    @Test
    fun theLoginNeverPrintsItsSecrets() {
        val text = login("1", "https://example.com").toString() + draft().toString()
        assertFalse("hunter2" in text)
        assertFalse("alice" in text)
    }

    private fun draft(
        origin: String = "https://example.com",
        username: String = "alice",
        password: String = "hunter2",
    ) = VaultLoginDraft(origin, formActionOrigin = null, httpRealm = null, username = username, password = password)

    private fun login(id: String, origin: String, lastUsed: Long? = null) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = null,
        username = "alice",
        password = "hunter2",
        createdAtMillis = 1,
        updatedAtMillis = 1,
        lastUsedAtMillis = lastUsed,
        timesUsed = 0,
    )
}
