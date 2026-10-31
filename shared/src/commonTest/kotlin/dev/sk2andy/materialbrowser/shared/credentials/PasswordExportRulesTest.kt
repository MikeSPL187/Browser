package dev.sk2andy.materialbrowser.shared.credentials

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PasswordExportRulesTest {
    /** The export writes origins already; the app's check would keep them as they are. */
    private val origin: (String) -> String? = { site -> site.takeIf { it.startsWith("https://") } }

    @Test
    fun exportedFileComesBackAsTheSameLogins() {
        val totp = TotpRules.canonical(TotpRules.parse("JBSWY3DPEHPK3PXP")!!)
        val logins = listOf(
            login("1", "https://example.com", "anna", "pa,ss\"word\nnext line", totp = totp),
            login("2", "https://example.com", "bob", " spaced ", realm = "Admin area"),
            login("3", "https://shop.example.org", "", "only-a-password", formAction = "https://pay.example.org"),
        )

        val csv = PasswordExportRules.csv(logins) { it.origin.removePrefix("https://") }
        assertTrue(csv.startsWith("\"name\",\"url\",\"username\",\"password\",\"note\""))

        val read = assertIs<PasswordImportParse.Read>(PasswordImportRules.parse(csv, origin))
        assertEquals(0, read.skipped)
        assertEquals(
            logins.map { VaultLoginDraft(it.origin, it.formActionOrigin, it.httpRealm, it.username, it.password) },
            read.logins.map(ImportedLogin::draft),
        )
        assertEquals(listOf(totp, null, null), read.logins.map(ImportedLogin::totp))
    }

    @Test
    fun noLoginsIsJustTheHeader() {
        assertEquals(
            "\"name\",\"url\",\"username\",\"password\",\"note\",\"totp\",\"httpRealm\",\"formActionOrigin\"\n",
            PasswordExportRules.csv(emptyList()) { "" },
        )
    }

    private fun login(
        id: String,
        origin: String,
        username: String,
        password: String,
        totp: String? = null,
        realm: String? = null,
        formAction: String? = null,
    ) = VaultLogin(id, origin, formAction, realm, username, password, 0, 0, null, 0, totp)
}
