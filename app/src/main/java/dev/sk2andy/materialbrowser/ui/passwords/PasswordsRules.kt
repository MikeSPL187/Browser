package dev.sk2andy.materialbrowser.ui.passwords

import dev.sk2andy.materialbrowser.browser.credentials.CredentialPromptRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import java.security.SecureRandom
import java.util.Locale

/** The open vault closes after [timeoutMillis] without anyone touching the passwords. */
internal class VaultAutoLock(private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS) {
    private var lastUseMillis: Long? = null

    fun touch(nowMillis: Long) {
        lastUseMillis = nowMillis
    }

    fun reset() {
        lastUseMillis = null
    }

    fun isExpired(nowMillis: Long): Boolean {
        val last = lastUseMillis ?: return true
        return nowMillis - last >= timeoutMillis || nowMillis < last
    }

    /** How long until the vault locks if nobody touches it, never below zero. */
    fun remainingMillis(nowMillis: Long): Long =
        lastUseMillis?.let { (it + timeoutMillis - nowMillis).coerceAtLeast(0) } ?: 0

    companion object {
        /** Owner's decision (2026-10-05): five minutes. */
        const val DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1_000L
    }
}

internal object PasswordsRules {
    const val CONFIRM_WORD_COUNT = 3

    /** Three different word positions (0-based, in order) the user types back to prove the phrase is written down. */
    fun confirmationPositions(random: SecureRandom, wordCount: Int = 12): List<Int> =
        generateSequence { random.nextInt(wordCount) }.distinct().take(CONFIRM_WORD_COUNT).sorted().toList()

    fun confirms(phrase: String, positions: List<Int>, answers: List<String>): Boolean {
        val words = phrase.split(' ')
        return positions.size == answers.size && positions.zip(answers).all { (position, answer) ->
            words.getOrNull(position) == answer.trim().lowercase(Locale.ROOT)
        }
    }

    /**
     * The origin for a login typed in by hand: «example.com», «https://example.com/login» and
     * «Example.COM:8443» all work; a plain-http site does not, because the vault fills only https.
     */
    fun manualOrigin(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("http://", ignoreCase = true)) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return CredentialPromptRules.canonicalHttpsOrigin(withScheme)
    }

    /** «accounts.example.com» for `https://accounts.example.com`, with the port when there is one. */
    fun displaySite(origin: String): String = origin.removePrefix("https://")

    /** Logins whose site or user name contains [query], ignoring case; all of them for a blank query. */
    fun filter(logins: List<VaultLogin>, query: String): List<VaultLogin> {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return logins
        return logins.filter { login ->
            displaySite(login.origin).contains(needle) || login.username.lowercase(Locale.ROOT).contains(needle)
        }
    }
}
