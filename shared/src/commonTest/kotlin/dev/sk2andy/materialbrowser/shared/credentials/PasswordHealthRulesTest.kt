package dev.sk2andy.materialbrowser.shared.credentials

import kotlin.test.Test
import kotlin.test.assertEquals

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

    private fun login(id: String, origin: String, password: String) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = null,
        username = "anna",
        password = password,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )
}
