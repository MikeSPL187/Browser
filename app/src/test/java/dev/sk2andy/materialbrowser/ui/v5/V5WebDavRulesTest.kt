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
}
