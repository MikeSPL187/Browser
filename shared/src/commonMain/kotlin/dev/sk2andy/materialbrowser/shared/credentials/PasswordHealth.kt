package dev.sk2andy.materialbrowser.shared.credentials

/** Why a weak password is weak, for the line under it (board W-PasswordHealth). */
enum class WeakPasswordKind { LettersOnly, DigitsOnly, Short, Other }

data class BreachedLogin(val login: VaultLogin, val timesSeen: Int)

/** [otherSiteKeys] names the other sites with the same password ([PasswordHealthRules.siteKey]), so the page can list them (МП-2). */
data class ReusedLogin(val login: VaultLogin, val otherSites: Int, val otherSiteKeys: List<String> = emptyList())

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
    private const val MISSING_PAGE_PATH = "/.well-known/resource-that-should-not-exist-whose-status-code-should-not-be-200"
    private val OK_STATUSES = 200..299
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
            .mapValues { (_, same) -> same.mapTo(linkedSetOf()) { siteKey(it.origin) } }
        val reused = logins.mapNotNull { login ->
            val sites = sitesByPassword[login.password].orEmpty()
            if (login.id in flagged || sites.size < 2) {
                null
            } else {
                val own = siteKey(login.origin)
                ReusedLogin(login, otherSites = sites.size - 1, otherSiteKeys = sites.filter { it != own })
            }
        }
        flagged += reused.map { it.login.id }
        // A login without a password has nothing to strengthen (МП-4); length counts characters as
        // a person does, an emoji as one, not UTF-16 units (МП-5).
        val weak = logins.mapNotNull { login ->
            if (login.id in flagged || login.password.isEmpty() || !isWeak(login.password)) null
            else WeakLogin(login, characterCount(login.password), weakKind(login.password))
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
        // As a browser reads the address: `\\`, `?` and `#` end the host too, so the name after `@`
        // in `http://evil.com\\@good.com` is not the host (П-1, МП-7).
        val authority = trimmed.substring(HTTP_SCHEME.length)
            .substringBefore('/').substringBefore('\\').substringBefore('?').substringBefore('#')
            .substringAfterLast('@')
        val host = if (authority.startsWith('[')) {
            authority.substringBefore(']') + "]"
        } else {
            authority.substringBefore(':')
        }.lowercase().trimEnd('.')
        return host.isNotEmpty() && !isLoopback(host)
    }

    /**
     * The phone itself: `localhost`, `127.x` in any of its short forms (`127.1`), `0.0.0.0` (which
     * reaches the phone), IPv6 `::1` written out or not, and IPv4 loopback mapped into IPv6 (МП-3).
     */
    private fun isLoopback(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".localhost")) return true
        if (host.startsWith('[')) {
            val address = host.removePrefix("[").removeSuffix("]")
            if (address.startsWith("::ffff:")) return isIpv4Loopback(address.removePrefix("::ffff:"))
            val groups = address.split(':').filter(String::isNotEmpty)
            return address.contains("::") && groups == listOf("1") ||
                groups.size == 8 && groups.dropLast(1).all { it.toIntOrNull(16) == 0 } && groups.last().toIntOrNull(16) == 1
        }
        return isIpv4Loopback(host)
    }

    private fun isIpv4Loopback(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size !in 1..4 || parts.any { it.toLongOrNull()?.let { value -> value >= 0 } != true }) return false
        return parts.size >= 2 && parts[0].toLong() == 127L || parts.size == 4 && parts.all { it.toLong() == 0L }
    }

    /** Characters as a person counts them: a pair of surrogates (an emoji) is one. */
    fun characterCount(text: String): Int = text.count { !it.isLowSurrogate() }

    fun isWeak(password: String): Boolean =
        PasswordGeneratorRules.strength(PasswordGeneratorRules.estimatedBits(password)) < PasswordStrength.Strong

    fun weakKind(password: String): WeakPasswordKind = when {
        password.isEmpty() -> WeakPasswordKind.Short
        password.all(Char::isLetter) -> WeakPasswordKind.LettersOnly
        password.all(Char::isDigit) -> WeakPasswordKind.DigitsOnly
        characterCount(password) < MIN_LENGTH -> WeakPasswordKind.Short
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

    /**
     * A path no site serves, asked next to [changePasswordUrl]: a site that answers it with a page
     * answers every path so, and its «change password» answer proves nothing.
     */
    fun missingPageUrl(origin: String): String = origin.trimEnd('/') + MISSING_PAGE_PATH

    /**
     * Where «Change password on the site» goes, after Chrome's check: the well-known page when the
     * site answers it ([changePasswordStatus], after redirects) and does not answer a missing page
     * the same way ([missingPageStatus]); otherwise the site's start page, never a 404.
     */
    fun changePasswordTarget(origin: String, changePasswordStatus: Int?, missingPageStatus: Int?): String {
        val supported = changePasswordStatus in OK_STATUSES && missingPageStatus !in OK_STATUSES
        return if (supported) changePasswordUrl(origin) else origin.trimEnd('/') + "/"
    }
}
