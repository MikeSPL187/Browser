package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CertificateProblemRulesTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    private fun problem(
        names: List<String> = listOf("mail.example"),
        notBefore: Long? = now - 30 * day,
        notAfter: Long? = now + 30 * day,
        selfSigned: Boolean = false,
    ) = CertificateProblemRules.problem("mail.example", names, notBefore, notAfter, selfSigned, now)

    @Test
    fun `the term comes before the name and the issuer`() {
        assertEquals(CertificateProblem.Expired, problem(notAfter = now - day, names = listOf("other.example"), selfSigned = true))
        assertEquals(CertificateProblem.NotYetValid, problem(notBefore = now + day, selfSigned = true))
        assertEquals(CertificateProblem.WrongSite, problem(names = listOf("other.example"), selfSigned = true))
        assertEquals(CertificateProblem.SelfSigned, problem(selfSigned = true))
        assertEquals(CertificateProblem.Untrusted, problem())
        // A certificate without names or dates says nothing more than that it is untrusted.
        assertEquals(CertificateProblem.Untrusted, problem(names = emptyList(), notBefore = null, notAfter = null))
        assertEquals(CertificateProblem.Untrusted, CertificateProblemRules.problem(null, "mail.example", now))
    }

    @Test
    fun `a wildcard stands for one leftmost label only`() {
        assertTrue(CertificateProblemRules.matches("*.example.com", "mail.example.com"))
        assertTrue(CertificateProblemRules.matches("Mail.Example.com.", "mail.example.com"))
        assertFalse(CertificateProblemRules.matches("*.example.com", "example.com"))
        assertFalse(CertificateProblemRules.matches("*.example.com", "a.b.example.com"))
        // A wildcard over a whole public suffix never matches.
        assertFalse(CertificateProblemRules.matches("*.com", "example.com"))
        assertTrue(CertificateProblemRules.matches("пример.рф", "xn--e1afmkfd.xn--p1ai"))
    }
}
