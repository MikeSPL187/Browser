package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.WeakPasswordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5PasswordHealthRulesTest {
    private val strong = "kV7#qe2Lm!Tz9pWf"

    @Test
    fun findingsFollowTheReportWithHttpInItsOwnSection() {
        val report = PasswordHealthRules.report(
            listOf(
                login("leak", "https://a.example", "Tr0ub4dor&3-horse!"),
                login("same1", "https://b.example", strong),
                login("same2", "https://c.example", strong),
                login("http", "http://d.example", "sunshine"),
                login("weak", "https://e.example", "12345678"),
            ),
            breaches = mapOf("leak" to 3),
        )
        val findings = V5PasswordHealthRules.findings(report) { it.origin.substringAfter("://") }

        assertEquals(
            listOf(
                "leak" to V5PasswordFindingKind.Breached,
                "same1" to V5PasswordFindingKind.Reused,
                "same2" to V5PasswordFindingKind.Reused,
                "http" to V5PasswordFindingKind.Insecure,
                "http" to V5PasswordFindingKind.Weak,
                "weak" to V5PasswordFindingKind.Weak,
            ),
            findings.map { it.loginId to it.kind },
        )
        assertEquals(3, findings.first().count)
        assertEquals("d.example", findings.first { it.kind == V5PasswordFindingKind.Insecure }.site)
        assertEquals(WeakPasswordKind.DigitsOnly, findings.last().weakKind)
    }

    @Test
    fun aLoginInTwoSectionsNeedsAttentionOnce() {
        val findings = listOf(
            finding("http", V5PasswordFindingKind.Insecure),
            finding("http", V5PasswordFindingKind.Weak),
            finding("weak", V5PasswordFindingKind.Weak),
        )
        assertEquals(2, V5PasswordHealthRules.attention(findings))
        assertEquals(0, V5PasswordHealthRules.attention(emptyList()))
    }

    @Test
    fun sectionsGoByUrgencyAndSkipEmptyOnes() {
        val findings = listOf(
            finding("w", V5PasswordFindingKind.Weak),
            finding("r", V5PasswordFindingKind.Reused),
        )
        assertEquals(
            listOf(V5PasswordFindingKind.Reused, V5PasswordFindingKind.Weak),
            V5PasswordHealthRules.sections(findings).map { it.first },
        )
    }

    @Test
    fun onlyHttpHasNoChangeAction() {
        assertFalse(V5PasswordHealthRules.canChange(finding("h", V5PasswordFindingKind.Insecure)))
        assertTrue(V5PasswordHealthRules.canChange(finding("w", V5PasswordFindingKind.Weak)))
        assertTrue(V5PasswordHealthRules.canChange(finding("b", V5PasswordFindingKind.Breached)))
    }

    @Test
    fun progressShowsAsWholePercent() {
        assertEquals(40, V5PasswordHealthRules.percent(0.4f))
        assertEquals(100, V5PasswordHealthRules.percent(1.5f))
        assertEquals(0, V5PasswordHealthRules.percent(-1f))
    }

    private fun finding(id: String, kind: V5PasswordFindingKind) = V5PasswordFinding(id, "$id.example", "anna", kind)

    private fun login(id: String, origin: String, password: String) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = null,
        username = "anna",
        password = password,
        createdAtMillis = 0,
        updatedAtMillis = 0,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )
}
