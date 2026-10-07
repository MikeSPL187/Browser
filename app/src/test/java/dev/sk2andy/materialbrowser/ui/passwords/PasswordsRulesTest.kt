package dev.sk2andy.materialbrowser.ui.passwords

import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordsRulesTest {
    @Test
    fun `the vault locks five minutes after the last use and when the clock goes back`() {
        val lock = VaultAutoLock()
        assertTrue("never opened", lock.isExpired(0))
        lock.touch(1_000)
        assertFalse(lock.isExpired(1_000 + 4 * 60_000))
        assertEquals(60_000, lock.remainingMillis(1_000 + 4 * 60_000))
        assertTrue(lock.isExpired(1_000 + 5 * 60_000))
        assertTrue(lock.isExpired(500))
        lock.reset()
        assertTrue(lock.isExpired(1_001))
    }

    @Test
    fun `the phrase check asks three different words and accepts them in any case`() {
        val positions = PasswordsRules.confirmationPositions(SecureRandom())
        assertEquals(3, positions.distinct().size)
        assertEquals(positions.sorted(), positions)
        assertTrue(positions.all { it in 0..11 })

        val phrase = "acid acorn acre acts afar affix aged agent agile aging agony ahead"
        assertTrue(PasswordsRules.confirms(phrase, listOf(0, 5, 11), listOf(" ACID", "affix", "Ahead ")))
        assertFalse(PasswordsRules.confirms(phrase, listOf(0, 5, 11), listOf("acid", "affix", "agony")))
        assertFalse(PasswordsRules.confirms(phrase, listOf(0, 5, 11), listOf("acid", "affix")))
    }

    @Test
    fun `a site typed by hand becomes an https origin`() {
        assertEquals("https://example.com", PasswordsRules.manualOrigin("example.com"))
        assertEquals("https://accounts.example.com", PasswordsRules.manualOrigin(" https://Accounts.Example.com/login?x=1 "))
        assertEquals("https://example.com:8443", PasswordsRules.manualOrigin("example.com:8443"))
        assertNull(PasswordsRules.manualOrigin("http://example.com"))
        assertNull(PasswordsRules.manualOrigin(""))
        assertNull(PasswordsRules.manualOrigin("not a site"))
        // An international name is kept in punycode, the spelling the browser reports for the page.
        assertEquals("https://xn--e1afmkfd.xn--p1ai", PasswordsRules.manualOrigin("пример.рф"))
        assertEquals("https://xn--e1afmkfd.xn--p1ai", PasswordsRules.manualOrigin("https://Пример.рф/вход"))
    }

    @Test
    fun `search matches the site or the user name`() {
        val logins = listOf(login("1", "https://mail.example.com", "anna@example.com"), login("2", "https://bank.example.ru", "anna.k"))
        assertEquals(listOf("2"), PasswordsRules.filter(logins, "BANK").map(VaultLogin::id))
        assertEquals(listOf("1", "2"), PasswordsRules.filter(logins, "anna").map(VaultLogin::id))
        assertEquals(logins, PasswordsRules.filter(logins, "  "))
        val international = login("3", "https://xn--e1afmkfd.xn--p1ai", "boris")
        assertEquals(listOf("3"), PasswordsRules.filter(logins + international, "Пример").map(VaultLogin::id))
        assertEquals("bank.example.ru", PasswordsRules.displaySite("https://bank.example.ru"))
    }

    private fun login(id: String, origin: String, username: String) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = null,
        username = username,
        password = "pw",
        createdAtMillis = 1,
        updatedAtMillis = 1,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )
}
