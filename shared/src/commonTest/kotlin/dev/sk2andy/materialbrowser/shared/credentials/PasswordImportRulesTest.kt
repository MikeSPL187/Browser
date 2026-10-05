package dev.sk2andy.materialbrowser.shared.credentials

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class PasswordImportRulesTest {
    /** A stand-in for the app's check: https sites only, reduced to the origin. */
    private val origin: (String) -> String? = { site ->
        val withScheme = if ("://" in site) site else "https://$site"
        withScheme.takeIf { it.startsWith("https://") }
            ?.removePrefix("https://")
            ?.substringBefore('/')
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() && ',' !in it }
            ?.let { "https://$it" }
    }

    @Test
    fun chromeCsvBecomesLogins() {
        val csv = "﻿name,url,username,password,note\r\n" +
            "example.com,https://example.com/login,anna,\"pa,ss\"\"word\",\r\n" +
            "app,android://hash@com.example.app/,anna,secret,\r\n" +
            "old,http://plain.example.org/,anna,secret,\r\n" +
            "empty,https://empty.example.com/,anna,,\r\n"

        val read = assertIs<PasswordImportParse.Read>(PasswordImportRules.parse(csv, origin))

        assertEquals(1, read.logins.size)
        assertEquals(3, read.skipped)
        val draft = read.logins.single().draft
        assertEquals("https://example.com", draft.origin)
        assertEquals("anna", draft.username)
        assertEquals("pa,ss\"word", draft.password)
    }

    @Test
    fun firefoxCsvKeepsRealmAndFormAction() {
        val csv = """
            "url","username","password","httpRealm","formActionOrigin","guid","timeCreated","timeLastUsed","timePasswordChanged"
            "https://example.com","anna","one","","https://example.com","{1}","1","1","1"
            "https://router.example.net","admin","two","Router","","{2}","1","1","1"
        """.trimIndent()

        val logins = assertIs<PasswordImportParse.Read>(PasswordImportRules.parse(csv, origin)).logins

        assertEquals("https://example.com", logins[0].draft.formActionOrigin)
        assertNull(logins[0].draft.httpRealm)
        assertEquals("Router", logins[1].draft.httpRealm)
    }

    @Test
    fun bitwardenAndProtonCsvSkipOtherItemsAndBringTwoFactorKeys() {
        val bitwarden = "folder,favorite,type,name,notes,fields,reprompt,login_uri,login_username,login_password,login_totp\n" +
            ",,login,Mail,,,0,\"https://mail.example.com,https://example.com\",anna,one,JBSWY3DPEHPK3PXP\n" +
            ",,note,Secret note,text,,0,,,,\n"
        val read = assertIs<PasswordImportParse.Read>(PasswordImportRules.parse(bitwarden, origin))
        assertEquals(1, read.skipped)
        assertEquals("https://mail.example.com", read.logins.single().draft.origin)
        assertEquals(TotpRules.canonical(TotpRules.parse("JBSWY3DPEHPK3PXP")!!), read.logins.single().totp)

        val proton = "type,name,url,email,username,password,note,totp,createTime,modifyTime,vault\n" +
            "login,Shop,https://shop.example.com,anna@example.com,,two,,steam://ABC,1,1,Personal\n"
        val login = assertIs<PasswordImportParse.Read>(PasswordImportRules.parse(proton, origin)).logins.single()
        assertEquals("anna@example.com", login.draft.username)
        assertNull(login.totp)
    }

    @Test
    fun onePasswordCsvIsFoundByItsHeader() {
        val csv = "Title,Url,Username,Password,OTPAuth,Favorite,Archived,Tags,Notes\n" +
            "Forum,forum.example.org,snowfox,three,,false,false,,\n"
        val login = assertIs<PasswordImportParse.Read>(PasswordImportRules.parse(csv, origin)).logins.single()
        assertEquals("https://forum.example.org", login.draft.origin)
    }

    @Test
    fun bitwardenJsonReadsLoginsAndRefusesAnEncryptedExport() {
        val export = """
            {"encrypted": false, "folders": [], "items": [
              {"type": 1, "name": "Bank", "login": {"uris": [{"match": null, "uri": "https://bank.example.com/"}],
               "username": "anna", "password": "four", "totp": "otpauth://totp/Bank:anna?secret=JBSWY3DPEHPK3PXP"}},
              {"type": 2, "name": "Note", "notes": "text"},
              {"type": 1, "name": "No site", "login": {"uris": [], "username": "x", "password": "y"}}
            ]}
        """.trimIndent()
        val read = assertIs<PasswordImportParse.Read>(PasswordImportRules.parse(export, origin))
        assertEquals(listOf("https://bank.example.com"), read.logins.map { it.draft.origin })
        assertEquals(2, read.skipped)
        assertEquals("otpauth://totp/?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30", read.logins.single().totp)

        assertEquals(PasswordImportParse.Encrypted, PasswordImportRules.parse("""{"encrypted": true, "data": "x"}""", origin))
        assertEquals(PasswordImportParse.NotAnExport, PasswordImportRules.parse("""{"bookmarks": []}""", origin))
    }

    @Test
    fun otherFilesAreNotExports() {
        assertEquals(PasswordImportParse.NotAnExport, PasswordImportRules.parse("<!DOCTYPE NETSCAPE-Bookmark-file-1>", origin))
        assertEquals(PasswordImportParse.NotAnExport, PasswordImportRules.parse("url,username\nhttps://a.com,anna\n", origin))
        assertEquals(PasswordImportParse.NotAnExport, PasswordImportRules.parse("url,password\n\"https://a.com,open", origin))
        assertEquals(PasswordImportParse.NotAnExport, PasswordImportRules.parse("", origin))
    }

    @Test
    fun csvRowsFollowRfc4180() {
        assertEquals(
            listOf(listOf("a", "b,c", "line\nbreak"), listOf("", "\"", "")),
            PasswordImportRules.csvRows("a,\"b,c\",\"line\nbreak\"\r\n,\"\"\"\",\n"),
        )
    }

    @Test
    fun importingKeepsSavedPasswordsAndAddsTheRest() {
        val saved = VaultLogin("1", "https://example.com", null, null, "anna", "saved", 0, 0, null, 0)
        val key = TotpRules.canonical(TotpRules.parse("JBSWY3DPEHPK3PXP")!!)
        val imported = listOf(
            ImportedLogin(VaultLoginDraft("https://example.com", null, null, "anna", "other"), totp = key),
            ImportedLogin(VaultLoginDraft("https://new.example.com", null, null, "anna", "new")),
            ImportedLogin(VaultLoginDraft("https://new.example.com", null, null, "anna", "again")),
            ImportedLogin(VaultLoginDraft("http://plain.example.com", null, null, "anna", "x")),
        )
        var next = 0
        val (logins, summary) = CredentialVaultRules.import(listOf(saved), imported, nowMillis = 5) { "n${next++}" }

        assertEquals(listOf("n0"), summary.addedIds)
        assertEquals(2, summary.duplicates)
        assertEquals(1, summary.rejected)
        assertEquals(1, summary.withTotp)
        assertEquals("saved", logins.first { it.id == "1" }.password)
        assertEquals(key, logins.first { it.id == "1" }.totp)
        assertEquals("new", logins.first { it.id == "n0" }.password)
        assertFalse("again" in imported[2].toString())
    }
}
