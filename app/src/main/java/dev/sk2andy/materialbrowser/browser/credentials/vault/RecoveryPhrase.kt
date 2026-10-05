package dev.sk2andy.materialbrowser.browser.credentials.vault

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

/**
 * EFF's short word list #1 (1,296 words, CC BY 3.0 US), as shipped in `assets`. Its SHA-256 is
 * pinned: a changed list would make every written-down recovery phrase useless, so a list that
 * does not match is refused rather than used.
 */
internal class RecoveryWordlist private constructor(val words: List<String>) {
    private val known = words.toHashSet()

    operator fun contains(word: String): Boolean = word in known

    companion object {
        const val ASSET = "eff_short_wordlist_1.txt"
        const val SIZE = 1_296
        private const val SHA256 = "36ecca49e4fa20ca84b176c32f2e9c82f98f446585190e75f9879a95c08247bf"

        fun from(bytes: ByteArray): RecoveryWordlist? {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            if (digest != SHA256) return null
            val words = bytes.toString(Charsets.UTF_8).lines().filter(String::isNotEmpty)
            return RecoveryWordlist(words).takeIf { words.size == SIZE && it.known.size == SIZE }
        }
    }
}

/**
 * The 12-word recovery phrase: 12 × log2(1296) ≈ 124 bits from the system CSPRNG. It is the way
 * back into the vault when the device key is gone (reset, new phone, lock screen removed).
 */
internal object RecoveryPhrase {
    const val WORD_COUNT = 12

    fun generate(wordlist: RecoveryWordlist, random: SecureRandom): String =
        List(WORD_COUNT) { wordlist.words[random.nextInt(wordlist.words.size)] }.joinToString(" ")

    /**
     * The phrase as typed, in its one canonical spelling (lower case, single spaces), or null if
     * it is not 12 words from the list. Hyphens stay: «yo-yo» is one word of the list.
     */
    fun normalize(input: String, wordlist: RecoveryWordlist): String? {
        val words = input.lowercase(Locale.ROOT).split(WHITESPACE).filter(String::isNotEmpty)
        if (words.size != WORD_COUNT || words.any { it !in wordlist }) return null
        return words.joinToString(" ")
    }

    private val WHITESPACE = Regex("[\\s,]+")
}
