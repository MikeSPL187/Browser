package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V5WebDavRulesTest {
    private val ready = V5WebDavConnectState(
        address = "https://cloud.example.org/remote.php/dav",
        login = "anna",
        password = "secret",
        phrase = "long enough phrase",
        phraseRepeat = "long enough phrase",
    )

    @Test
    fun connectNeedsHttpsLoginPasswordAndMatchingLongPhrase() {
        assertTrue(V5WebDavRules.canConnect(ready))
        assertFalse(V5WebDavRules.canConnect(ready.copy(address = "http://cloud.example.org")))
        assertFalse(V5WebDavRules.canConnect(ready.copy(address = "https://")))
        assertFalse(V5WebDavRules.canConnect(ready.copy(login = " ")))
        assertFalse(V5WebDavRules.canConnect(ready.copy(password = "")))
        assertFalse(V5WebDavRules.canConnect(ready.copy(phrase = "short", phraseRepeat = "short")))
        assertFalse(V5WebDavRules.canConnect(ready.copy(phraseRepeat = "long enough phrasE")))
        assertFalse(V5WebDavRules.canConnect(ready.copy(connecting = true)))
    }

    @Test
    fun anHttpAddressIsSaidBeforeAskingTheServer() {
        assertEquals(V5WebDavError.NotHttps, V5WebDavRules.addressProblem(" HTTP://cloud.example.org"))
        assertNull(V5WebDavRules.addressProblem("https://cloud.example.org"))
        assertNull(V5WebDavRules.addressProblem(""))
    }

    @Test
    fun anAddressTheSyncWouldRefuseCannotBeSentAndSaysWhy() {
        listOf(
            "https://anna@cloud.example.org",
            "https://cloud.example.org/dav?x=1",
            "https://cloud.example.org/#files",
            "https://cloud.example.org/my files",
            "https://cloud.example.org:99999",
            "https://cloud\\example.org",
        ).forEach { address ->
            assertFalse(address, V5WebDavRules.canConnect(ready.copy(address = address)))
            assertEquals(address, V5WebDavError.InvalidAddress, V5WebDavRules.addressProblem(address))
        }
        assertNull("only the scheme typed so far", V5WebDavRules.addressProblem("https://"))
        assertNull(V5WebDavRules.addressProblem("cloud"))
        assertTrue(V5WebDavRules.canConnect(ready.copy(address = "https://cloud.example.org:8443/dav")))
    }

    @Test
    fun aPhraseOfTooFewDifferentCharactersIsRefused() {
        assertTrue(V5WebDavRules.phraseTooSimple("aaaaaaaaaa"))
        assertTrue(V5WebDavRules.phraseTooSimple("abababababab"))
        assertFalse(V5WebDavRules.phraseTooSimple("short"))
        assertFalse(V5WebDavRules.phraseTooSimple("лиса-кедр-зима"))
        assertFalse(V5WebDavRules.canConnect(ready.copy(phrase = "aaaaaaaaaa", phraseRepeat = "aaaaaaaaaa")))
    }

    @Test
    fun phrasesDifferOnlyOnceTheRepeatIsTyped() {
        assertFalse(V5WebDavRules.phrasesDiffer(ready.copy(phraseRepeat = "")))
        assertTrue(V5WebDavRules.phrasesDiffer(ready.copy(phraseRepeat = "other")))
        assertFalse(V5WebDavRules.phrasesDiffer(ready))
    }

    @Test
    fun everyErrorHasItsOwnLine() {
        val lines = V5WebDavError.entries.map(V5WebDavRules::errorText)
        assertEquals(lines.size, lines.toSet().size)
    }

    @Test
    fun `an address without https is told to start with it`() {
        assertEquals(V5WebDavError.MissingScheme, V5WebDavRules.addressProblem("cloud.example.com"))
        assertNull(V5WebDavRules.addressProblem("   "))
    }

    @Test
    fun `runs, keyboard rows and common passwords are too simple`() {
        listOf("1234567890", "qwertyuiop", "password12", "abcdefghij", "0987654321", "йцукенгшщз").forEach { phrase ->
            assertTrue(phrase, V5WebDavRules.phraseTooSimple(phrase))
        }
        listOf("лес река гора небо", "Tr0ub4dor&3x", "correct horse battery").forEach { phrase ->
            assertFalse(phrase, V5WebDavRules.phraseTooSimple(phrase))
        }
    }
}
