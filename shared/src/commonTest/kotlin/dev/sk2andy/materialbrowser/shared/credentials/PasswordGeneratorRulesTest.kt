package dev.sk2andy.materialbrowser.shared.credentials

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordGeneratorRulesTest {
    private val random = Random(42)
    private val nextInt: (Int) -> Int = { bound -> random.nextInt(bound) }

    @Test
    fun aPasswordHasTheLengthAndEveryGroup() {
        repeat(200) {
            val password = PasswordGeneratorRules.password(PasswordGeneratorOptions(length = 12), nextInt)
            assertEquals(12, password.length)
            assertTrue(password.any(Char::isLowerCase))
            assertTrue(password.any(Char::isUpperCase))
            assertTrue(password.any(Char::isDigit))
            assertTrue(password.any { !it.isLetterOrDigit() })
            assertFalse(password.any { it in "lI1O0o" }, password)
        }
    }

    @Test
    fun lettersOnlyAndAmbiguousCharactersFollowTheSwitches() {
        val lettersOnly = PasswordGeneratorOptions(length = 30, digitsAndSymbols = false, avoidAmbiguous = false)
        repeat(50) {
            assertTrue(PasswordGeneratorRules.password(lettersOnly, nextInt).all(Char::isLetter))
        }
        assertEquals(2, PasswordGeneratorRules.groups(lettersOnly).size)
        assertTrue("O" in PasswordGeneratorRules.groups(lettersOnly)[1])
    }

    @Test
    fun lengthAndWordsStayInRange() {
        assertEquals(PasswordGeneratorRules.MIN_LENGTH, PasswordGeneratorRules.password(PasswordGeneratorOptions(length = 2), nextInt).length)
        assertEquals(PasswordGeneratorRules.MAX_LENGTH, PasswordGeneratorRules.password(PasswordGeneratorOptions(length = 500), nextInt).length)
        val phrase = PasswordGeneratorRules.phrase(listOf("acid", "acorn", "acre"), count = 99, nextInt)
        assertEquals(PasswordGeneratorRules.MAX_WORDS, phrase.split(PasswordGeneratorRules.WORD_SEPARATOR).size)
    }

    @Test
    fun strengthFollowsTheEntropy() {
        val defaults = PasswordGeneratorOptions()
        assertEquals(PasswordStrength.VeryStrong, PasswordGeneratorRules.strength(PasswordGeneratorRules.entropyBits(defaults, 1_296)))
        val phrase = PasswordGeneratorOptions(mode = PasswordGeneratorMode.Phrase, words = 6)
        assertEquals(PasswordStrength.Strong, PasswordGeneratorRules.strength(PasswordGeneratorRules.entropyBits(phrase, 1_296)))
        assertEquals(PasswordStrength.Weak, PasswordGeneratorRules.strength(PasswordGeneratorRules.estimatedBits("password")))
        assertEquals(PasswordStrength.Weak, PasswordGeneratorRules.strength(PasswordGeneratorRules.estimatedBits("aaaaaaaaaaaa")))
        assertTrue(PasswordGeneratorRules.estimatedBits("vK7#qe2Lm!Tz9pWf") >= 80.0)
    }
}
