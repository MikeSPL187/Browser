package dev.sk2andy.materialbrowser.shared.credentials

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHealthRulesTest {
    private val logins = listOf(
        login("1", "https://forum.example.org", "Tr0ub4dor&3-horse!"),
        login("2", "https://tasks.example.com", "Same-Strong-Pass-42!"),
        login("3", "https://mail.example.com", "Same-Strong-Pass-42!"),
        login("4", "https://cinema.example.ru", "sunshine"),
        login("5", "https://bank.example.com", "kV7#qe2Lm!Tz9pWf"),
        login("6", "https://tasks.example.com", "Same-Strong-Pass-42!"),
    )

    @Test
    fun eachLoginShowsOnceInItsMostUrgentSection() {
        val report = PasswordHealthRules.report(logins, breaches = mapOf("1" to 12, "2" to 3, "5" to 0))

        assertEquals(listOf("1", "2"), report.breached.map { it.login.id })
        assertEquals(listOf("3" to 1, "6" to 1), report.reused.map { it.login.id to it.otherSites })
        assertEquals(listOf("4"), report.weak.map { it.login.id })
        assertEquals(WeakPasswordKind.LettersOnly, report.weak.single().kind)
        assertEquals(1, report.fine)
        assertEquals(16, report.finePercent)
    }

    @Test
    fun changingThePasswordOpensTheWellKnownPageOnlyWhereTheSiteHasOne() {
        val origin = "https://shop.example.com"

        assertEquals("$origin/.well-known/change-password", PasswordHealthRules.changePasswordTarget(origin, 200, 404))
        assertEquals("$origin/", PasswordHealthRules.changePasswordTarget(origin, 404, 404))
        assertEquals("$origin/", PasswordHealthRules.changePasswordTarget(origin, null, null))
        // A site that answers every path with a page proves nothing by answering this one.
        assertEquals("$origin/", PasswordHealthRules.changePasswordTarget(origin, 200, 200))
    }

    @Test
    fun loginsWithoutAPasswordRepeatNothingAndAreShortNotLettersOnly() {
        val report = PasswordHealthRules.report(
            listOf(login("a", "https://a.example", ""), login("b", "https://b.example", ""), login("c", "https://c.example", "")),
            breaches = emptyMap(),
        )

        assertTrue(report.reused.isEmpty())
        assertEquals(List(3) { WeakPasswordKind.Short }, report.weak.map { it.kind })
        assertEquals(List(3) { 0 }, report.weak.map { it.length })
    }

    @Test
    fun aSiteAndItsWwwAreOneSiteForRepeats() {
        val report = PasswordHealthRules.report(
            listOf(
                login("bare", "https://example.com", "Same-Strong-Pass-42!"),
                login("www", "https://WWW.example.com/", "Same-Strong-Pass-42!"),
            ),
            breaches = emptyMap(),
        )

        assertTrue(report.reused.isEmpty())
    }

    @Test
    fun theV5CountIncludesPlainHttpLoginsOnce() {
        val report = PasswordHealthRules.report(
            listOf(
                login("http-weak", "http://shop.example.com", "sunshine"),
                login("http-strong", "http://news.example.com", "kV7#qe2Lm!Tz9pWf"),
                login("fine", "https://bank.example.com", "Tr0ub4dor&3-horse!"),
            ),
            breaches = emptyMap(),
        )

        assertEquals(1, report.needsAttention)
        assertEquals(2, report.attentionWithInsecure)
        assertEquals(setOf("http-weak"), report.attentionIds(withInsecure = false))
    }

    @Test
    fun passwordsSentOverPlainHttpAreListedApartFromTheOtherSections() {
        val report = PasswordHealthRules.report(
            listOf(
                login("site", "http://shop.example.com", "sunshine"),
                login("form", "https://news.example.com", "kV7#qe2Lm!Tz9pWf", formActionOrigin = "http://news.example.com"),
                login("safe", "https://bank.example.com", "Tr0ub4dor&3-horse!", formActionOrigin = "https://bank.example.com"),
                login("router", "http://127.0.0.1:8080", "Same-Strong-Pass-42!"),
            ),
            breaches = emptyMap(),
        )

        assertEquals(listOf("site" to false, "form" to true), report.insecure.map { it.login.id to it.formOnly })
        assertEquals(listOf("site"), report.weak.map { it.login.id })
        assertEquals(1, report.needsAttention)
    }

    @Test
    fun onlyPlainHttpToAnotherMachineIsInsecure() {
        assertTrue(PasswordHealthRules.isPlainHttp("http://example.com"))
        assertTrue(PasswordHealthRules.isPlainHttp("HTTP://Example.com:8080/login"))
        assertTrue(PasswordHealthRules.isPlainHttp("http://192.168.0.1"))
        assertTrue(PasswordHealthRules.isPlainHttp("http://127.example.com"))
        assertFalse(PasswordHealthRules.isPlainHttp("https://example.com"))
        assertFalse(PasswordHealthRules.isPlainHttp("http://localhost:3000"))
        assertFalse(PasswordHealthRules.isPlainHttp("http://app.localhost"))
        assertFalse(PasswordHealthRules.isPlainHttp("http://127.0.0.1"))
        assertFalse(PasswordHealthRules.isPlainHttp("http://[::1]:8080"))
        assertFalse(PasswordHealthRules.isPlainHttp("android://hash@com.example.app/"))
        assertFalse(PasswordHealthRules.isPlainHttp("http://"))
    }

    @Test
    fun noLoginsIsAllFine() {
        assertEquals(100, PasswordHealthRules.report(emptyList(), emptyMap()).finePercent)
    }

    @Test
    fun theRangeAnswerIsReadByTheHashSuffix() {
        val body = "0018A45C4D1DEF81644B54AB7F969B88D65:3\r\n1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493\r\n00D4F6E8FA6EECAD2A3AA415EEC418D38EC:0\r\n"
        assertEquals(3_861_493, PasswordHealthRules.timesSeen(body, "1e4c9b93f3f0682250b6cf8331b7ee68fd8"))
        assertEquals(0, PasswordHealthRules.timesSeen(body, "00D4F6E8FA6EECAD2A3AA415EEC418D38EC"))
        assertEquals(0, PasswordHealthRules.timesSeen(body, "FFFFF"))
        assertEquals("https://a.example/.well-known/change-password", PasswordHealthRules.changePasswordUrl("https://a.example"))
    }

    private fun login(id: String, origin: String, password: String, formActionOrigin: String? = null) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = formActionOrigin,
        httpRealm = null,
        username = "anna",
        password = password,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )
}
