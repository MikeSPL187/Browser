package dev.sk2andy.materialbrowser.shared.credentials

import kotlin.math.ln

/** What the generator makes (board W-Generator): a random password or a phrase of words. */
enum class PasswordGeneratorMode { Password, Phrase }

data class PasswordGeneratorOptions(
    val mode: PasswordGeneratorMode = PasswordGeneratorMode.Password,
    val length: Int = PasswordGeneratorRules.DEFAULT_LENGTH,
    val digitsAndSymbols: Boolean = true,
    /** Leaves out l, I, 1, O, 0 and o, which are easy to misread when a password is typed by hand. */
    val avoidAmbiguous: Boolean = true,
    val words: Int = PasswordGeneratorRules.DEFAULT_WORDS,
) {
    fun normalized(): PasswordGeneratorOptions = copy(
        length = length.coerceIn(PasswordGeneratorRules.MIN_LENGTH, PasswordGeneratorRules.MAX_LENGTH),
        words = words.coerceIn(PasswordGeneratorRules.MIN_WORDS, PasswordGeneratorRules.MAX_WORDS),
    )
}

/** How long guessing would take, from the entropy in bits. */
enum class PasswordStrength { Weak, Fair, Strong, VeryStrong }

/**
 * The password generator's rules. Randomness comes in from outside ([nextInt] returns a uniform
 * value in `0 until bound`, from the system CSPRNG in the app), so the rules are tested without it.
 */
object PasswordGeneratorRules {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 64
    const val DEFAULT_LENGTH = 20
    const val MIN_WORDS = 4
    const val MAX_WORDS = 10
    const val DEFAULT_WORDS = 6
    const val WORD_SEPARATOR = "-"

    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"

    /** Symbols most sites accept; no quotes, backslash or spaces, which some forms mangle. */
    private const val SYMBOLS = "!#$%&*+-=?@^_"
    private const val AMBIGUOUS = "lI1O0o"

    private const val FAIR_BITS = 40.0
    private const val STRONG_BITS = 60.0
    private const val VERY_STRONG_BITS = 80.0

    /** The character groups a password draws from; each group appears at least once. */
    fun groups(options: PasswordGeneratorOptions): List<String> {
        val groups = buildList {
            add(LOWER)
            add(UPPER)
            if (options.digitsAndSymbols) {
                add(DIGITS)
                add(SYMBOLS)
            }
        }
        return if (options.avoidAmbiguous) groups.map { group -> group.filterNot { it in AMBIGUOUS } } else groups
    }

    fun password(options: PasswordGeneratorOptions, nextInt: (Int) -> Int): String {
        val length = options.normalized().length
        val groups = groups(options)
        val all = groups.joinToString("")
        val characters = CharArray(length) { index ->
            // The first characters take one from each group, the rest from all; then all are shuffled.
            val source = groups.getOrNull(index) ?: all
            source[nextInt(source.length)]
        }
        for (index in characters.lastIndex downTo 1) {
            val other = nextInt(index + 1)
            val kept = characters[index]
            characters[index] = characters[other]
            characters[other] = kept
        }
        return characters.concatToString()
    }

    /** [count] words from [wordlist], joined with [WORD_SEPARATOR]. */
    fun phrase(wordlist: List<String>, count: Int, nextInt: (Int) -> Int): String {
        require(wordlist.isNotEmpty())
        val words = count.coerceIn(MIN_WORDS, MAX_WORDS)
        return List(words) { wordlist[nextInt(wordlist.size)] }.joinToString(WORD_SEPARATOR)
    }

    /** Bits of entropy of a generated value with [options]; [wordlistSize] counts for phrases. */
    fun entropyBits(options: PasswordGeneratorOptions, wordlistSize: Int): Double {
        val normalized = options.normalized()
        return when (normalized.mode) {
            PasswordGeneratorMode.Password -> normalized.length * log2(groups(normalized).sumOf { it.length }.toDouble())
            PasswordGeneratorMode.Phrase -> normalized.words * log2(wordlistSize.toDouble())
        }
    }

    /**
     * A rough guess for a password a person chose: length times the size of the groups it uses.
     * It overrates patterns and words, so it is only ever used to call a password weak, never safe.
     */
    fun estimatedBits(password: String): Double {
        if (password.isEmpty()) return 0.0
        var pool = 0
        if (password.any { it in 'a'..'z' }) pool += LOWER.length
        if (password.any { it in 'A'..'Z' }) pool += UPPER.length
        if (password.any { it in '0'..'9' }) pool += DIGITS.length
        if (password.any { !it.isLetterOrDigit() }) pool += SYMBOLS.length
        if (password.any { it.isLetter() && it !in 'a'..'z' && it !in 'A'..'Z' }) pool += OTHER_LETTERS
        val distinct = password.toSet().size
        return minOf(password.length, distinct * 2) * log2(pool.coerceAtLeast(2).toDouble())
    }

    fun strength(bits: Double): PasswordStrength = when {
        bits >= VERY_STRONG_BITS -> PasswordStrength.VeryStrong
        bits >= STRONG_BITS -> PasswordStrength.Strong
        bits >= FAIR_BITS -> PasswordStrength.Fair
        else -> PasswordStrength.Weak
    }

    private const val OTHER_LETTERS = 33

    private fun log2(value: Double): Double = ln(value) / ln(2.0)
}
