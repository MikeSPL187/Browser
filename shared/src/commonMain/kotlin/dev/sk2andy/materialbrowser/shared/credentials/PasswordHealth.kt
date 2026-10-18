package dev.sk2andy.materialbrowser.shared.credentials

/** Why a weak password is weak, for the line under it (board W-PasswordHealth). */
enum class WeakPasswordKind { LettersOnly, DigitsOnly, Short, Other }

data class BreachedLogin(val login: VaultLogin, val timesSeen: Int)

data class ReusedLogin(val login: VaultLogin, val otherSites: Int)

data class WeakLogin(val login: VaultLogin, val length: Int, val kind: WeakPasswordKind)

/**
 * A login whose password goes over plain HTTP: the site itself ([formOnly] false) or only the form
 * it is sent with. Changing the password does not help; only the site can fix it.
 */
data class InsecureLogin(val login: VaultLogin, val formOnly: Boolean)

/**
 * What the password check found. A login shows in one section only, the most urgent one: leaks,
 * then repeats, then weak passwords. [insecure] stands apart, since a new password does not fix
 * it: every login sent over plain HTTP, whatever its other sections. [needsAttention] leaves it
 * out for the screen without a section for it; [attentionWithInsecure] counts it, as the v5 check
 * does, so its entry and its page say the same number.
 */
data class PasswordHealthReport(
    val total: Int,
    val breached: List<BreachedLogin>,
    val reused: List<ReusedLogin>,
    val weak: List<WeakLogin>,
    val insecure: List<InsecureLogin> = emptyList(),
) {
    val needsAttention: Int get() = breached.size + reused.size + weak.size

    /** Logins needing attention, each once; plain-HTTP ones too when [withInsecure]. */
    fun attentionIds(withInsecure: Boolean): Set<String> = buildSet {
        breached.mapTo(this) { it.login.id }
        reused.mapTo(this) { it.login.id }
        weak.mapTo(this) { it.login.id }
        if (withInsecure) insecure.mapTo(this) { it.login.id }
    }

    val attentionWithInsecure: Int get() = attentionIds(withInsecure = true).size
    val fine: Int get() = total - needsAttention

    /** Share of logins that are fine, in whole percent; 100 with no logins. */
    val finePercent: Int get() = if (total == 0) 100 else fine * 100 / total
}

/**
 * The password check (Q21b): repeats and weak passwords are found on the phone; leaks come from an
 * optional Pwned Passwords check, which sends only the first five characters of a SHA-1 hash.
 */
object PasswordHealthRules {
    const val HASH_PREFIX_LENGTH = 5
    private const val MIN_LENGTH = 12
    private const val CHANGE_PASSWORD_PATH = "/.well-known/change-password"
    private const val HTTP_SCHEME = "http://"

    /** [breaches] maps a login id to how often its password was seen in leaks; 0 or absent is clean. */
    fun report(logins: List<VaultLogin>, breaches: Map<String, Int>): PasswordHealthReport {
        val breached = logins.mapNotNull { login ->
            breaches[login.id]?.takeIf { it > 0 }?.let { BreachedLogin(login, it) }
        }
        val flagged = breached.mapTo(mutableSetOf()) { it.login.id }
        // A login without a password repeats nothing; example.com and www.example.com are one site.
        val sitesByPassword = logins.filter { it.password.isNotEmpty() }
            .groupBy(VaultLogin::password)
            .mapValues { (_, same) -> same.mapTo(mutableSetOf()) { siteKey(it.origin) }.size }
        val reused = logins.mapNotNull { login ->
            val sites = sitesByPassword[login.password] ?: 0
            if (login.id in flagged || sites < 2) null else ReusedLogin(login, otherSites = sites - 1)
        }
        flagged += reused.map { it.login.id }
        val weak = logins.mapNotNull { login ->
            if (login.id in flagged || !isWeak(login.password)) null
            else WeakLogin(login, login.password.length, weakKind(login.password))
        }
        val insecure = logins.mapNotNull { login ->
            when {
                isPlainHttp(login.origin) -> InsecureLogin(login, formOnly = false)
                login.formActionOrigin?.let(::isPlainHttp) == true -> InsecureLogin(login, formOnly = true)
                else -> null
            }
        }
        return PasswordHealthReport(logins.size, breached, reused, weak, insecure)
    }

    /**
     * Whether [origin] sends what it gets over plain HTTP to another machine. The phone's own
     * loopback addresses (a local server or router setup page on the device) do not leave it.
     */
    fun isPlainHttp(origin: String): Boolean {
        val trimmed = origin.trim()
        if (!trimmed.startsWith(HTTP_SCHEME, ignoreCase = true)) return false
        val authority = trimmed.substring(HTTP_SCHEME.length).substringBefore('/').substringAfterLast('@')
        val host = if (authority.startsWith('[')) {
            authority.substringBefore(']') + "]"
        } else {
            authority.substringBefore(':')
        }.lowercase().trimEnd('.')
        return host.isNotEmpty() && !isLoopback(host)
    }

    private fun isLoopback(host: String): Boolean =
        host == "localhost" || host.endsWith(".localhost") || host == "[::1]" ||
            host.split('.').let { parts -> parts.size == 4 && parts[0] == "127" && parts.all { it.toIntOrNull() in 0..255 } }

    fun isWeak(password: String): Boolean =
        PasswordGeneratorRules.strength(PasswordGeneratorRules.estimatedBits(password)) < PasswordStrength.Strong

    fun weakKind(password: String): WeakPasswordKind = when {
        password.isEmpty() -> WeakPasswordKind.Short
        password.all(Char::isLetter) -> WeakPasswordKind.LettersOnly
        password.all(Char::isDigit) -> WeakPasswordKind.DigitsOnly
        password.length < MIN_LENGTH -> WeakPasswordKind.Short
        else -> WeakPasswordKind.Other
    }

    /**
     * How often [hashSuffix] (the SHA-1 hex after its first five characters) appears in a Pwned
     * Passwords range answer: lines of `SUFFIX:COUNT`. Padding lines carry a count of 0.
     */
    fun timesSeen(rangeBody: String, hashSuffix: String): Int {
        val wanted = hashSuffix.uppercase()
        return rangeBody.lineSequence()
            .map(String::trim)
            .firstOrNull { line -> line.substringBefore(':').uppercase() == wanted }
            ?.substringAfter(':')
            ?.toIntOrNull()
            ?: 0
    }

    /** [origin] as one site for repeats: lower case, without a leading `www.`. */
    fun siteKey(origin: String): String {
        val trimmed = origin.trim().trimEnd('/').lowercase()
        val scheme = trimmed.substringBefore("://", missingDelimiterValue = "")
        val rest = trimmed.substringAfter("://")
        return scheme + "://" + rest.removePrefix("www.")
    }

    /** The site's own «change password» page (W3C well-known URL). */
    fun changePasswordUrl(origin: String): String = origin.trimEnd('/') + CHANGE_PASSWORD_PATH
}
