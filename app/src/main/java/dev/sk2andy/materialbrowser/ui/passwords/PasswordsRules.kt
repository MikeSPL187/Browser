package dev.sk2andy.materialbrowser.ui.passwords

import androidx.biometric.BiometricPrompt
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.CredentialPromptRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import java.net.IDN
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
    /**
     * What the screen says after the system prompt closed with [errorCode]; null when the user
     * closed it or nothing needs saying. The prompt itself is gone by then, so a lockout must not
     * leave «Open» doing nothing in silence.
     */
    fun authenticationError(errorCode: Int): Int? = when (errorCode) {
        BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> R.string.passwords_auth_lockout
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL, BiometricPrompt.ERROR_NO_BIOMETRICS -> R.string.passwords_no_screen_lock
        BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
        BiometricPrompt.ERROR_VENDOR, BiometricPrompt.ERROR_NO_SPACE -> R.string.passwords_failed
        else -> null
    }

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
     * The origin for a login typed in by hand: «example.com», «https://example.com/login»,
     * «Example.COM:8443» and «пример.рф» (kept as punycode) all work; a plain-http site does not,
     * because the vault fills only https.
     */
    fun manualOrigin(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("http://", ignoreCase = true)) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return CredentialPromptRules.canonicalHttpsOrigin(withAsciiHost(withScheme))
    }

    /** [url] with an international host name in punycode, which is what the origin check reads. */
    private fun withAsciiHost(url: String): String {
        val start = url.indexOf("://") + SCHEME_SEPARATOR.length
        val end = url.indexOfAny(charArrayOf('/', '?', '#'), start).takeIf { it >= 0 } ?: url.length
        val authority = url.substring(start, end)
        if (authority.all { it.code < ASCII_LIMIT }) return url
        val host = authority.substringAfterLast('@').substringBefore(':')
        val ascii = runCatching { IDN.toASCII(host) }.getOrNull() ?: return url
        return url.substring(0, start) + authority.replace(host, ascii) + url.substring(end)
    }

    /** «accounts.example.com» for `https://accounts.example.com`, with the port when there is one. */
    fun displaySite(origin: String): String = origin.removePrefix("https://")

    /**
     * [logins] under their site's first letter (canvas «Passwords»), letters in order and logins in
     * their own order within one; a site that starts with no letter goes under «#» at the end.
     */
    fun byLetter(logins: List<VaultLogin>): List<Pair<String, List<VaultLogin>>> =
        logins.groupBy { login ->
            displaySite(login.origin).removePrefix("http://").removePrefix("www.")
                .firstOrNull()?.takeIf(Char::isLetter)?.uppercase() ?: OTHER_LETTER
        }.toList().sortedWith(compareBy({ it.first == OTHER_LETTER }, { it.first }))

    private const val OTHER_LETTER = "#"

    /**
     * Logins whose site or user name contains [query], ignoring case; all of them for a blank query.
     * A site shown in punycode is also found by its own spelling («пример» finds `xn--e1afmkfd.xn--p1ai`).
     */
    fun filter(logins: List<VaultLogin>, query: String): List<VaultLogin> {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return logins
        return logins.filter { login ->
            val site = displaySite(login.origin)
            site.contains(needle) ||
                (PUNYCODE in site && unicodeSite(site).contains(needle)) ||
                login.username.lowercase(Locale.ROOT).contains(needle)
        }
    }

    private fun unicodeSite(site: String): String = runCatching { IDN.toUnicode(site).lowercase(Locale.ROOT) }.getOrDefault(site)

    private const val SCHEME_SEPARATOR = "://"
    private const val ASCII_LIMIT = 128
    private const val PUNYCODE = "xn--"
}
