package dev.sk2andy.materialbrowser.browser.credentials

import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import org.junit.Assert.assertEquals
import org.junit.Test

class LeakAnswersTest {
    private val leaked = login("leaked", "https://a.example", "password")
    private val clean = login("clean", "https://b.example", "kV7#qe2Lm!Tz9pWf")

    @Test
    fun `a check that fails later keeps the leak an earlier one found`() {
        val first = LeakAnswers.record(emptyMap(), listOf(leaked, clean), LeakCheckResult(mapOf("leaked" to 7), emptySet()))

        val second = LeakAnswers.record(first, listOf(leaked, clean), LeakCheckResult(emptyMap(), setOf("leaked", "clean")))

        assertEquals(mapOf("leaked" to 7), LeakAnswers.breaches(listOf(leaked, clean), second))
        assertEquals(0, LeakAnswers.unchecked(listOf(leaked, clean), second))
    }

    @Test
    fun `a password changed after the check is unchecked, not still leaked`() {
        val answers = LeakAnswers.record(emptyMap(), listOf(leaked, clean), LeakCheckResult(mapOf("leaked" to 7), emptySet()))
        val changed = leaked.copy(password = "Tr0ub4dor&3-horse!")

        assertEquals(emptyMap<String, Int>(), LeakAnswers.breaches(listOf(changed, clean), answers))
        assertEquals(1, LeakAnswers.unchecked(listOf(changed, clean), answers))
    }

    @Test
    fun `a login added later with an already leaked password shows the leak`() {
        val answers = LeakAnswers.record(emptyMap(), listOf(leaked), LeakCheckResult(mapOf("leaked" to 7), emptySet()))
        val added = login("added", "https://c.example", "password")

        assertEquals(mapOf("leaked" to 7, "added" to 7), LeakAnswers.breaches(listOf(leaked, added), answers))
    }

    @Test
    fun `an empty password is neither leaked nor waiting for a check`() {
        val empty = login("empty", "https://d.example", "")

        assertEquals(emptyMap<String, Int>(), LeakAnswers.breaches(listOf(empty), mapOf("" to 3)))
        assertEquals(0, LeakAnswers.unchecked(listOf(empty), emptyMap()))
    }

    private fun login(id: String, origin: String, password: String) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = null,
        username = "anna",
        password = password,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )
}
