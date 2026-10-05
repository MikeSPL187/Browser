package dev.sk2andy.materialbrowser.shared.credentials

/** The vault's logins after a change, and what the change did. */
data class VaultChange(
    val logins: List<VaultLogin>,
    val result: VaultSaveResult,
)

/**
 * What the vault accepts and how it matches. Pure rules, so every engine adapter and every
 * storage behaves the same and the tests cover them without a device.
 */
object CredentialVaultRules {
    const val MAX_LOGINS = 10_000
    const val MAX_USERNAME_LENGTH = 1_024
    const val MAX_PASSWORD_LENGTH = 4_096
    const val MAX_REALM_LENGTH = 512
    const val MAX_ID_LENGTH = 128
    private const val MAX_HOST_LENGTH = 253
    private const val MAX_LABEL_LENGTH = 63
    private const val HTTPS_PREFIX = "https://"
    private const val DEFAULT_HTTPS_PORT = 443
    private const val MAX_PORT = 65_535

    /**
     * Whether [origin] is in the one form the vault stores: `https://` + lower-case ASCII host
     * (IDN already in punycode) + an explicit port only when it is not 443. No path, no user info.
     * Adapters canonicalize what the engine reports before asking; anything else is refused, so a
     * login can never be keyed to a look-alike spelling of a site.
     */
    fun isCanonicalOrigin(origin: String): Boolean {
        if (!origin.startsWith(HTTPS_PREFIX)) return false
        val authority = origin.substring(HTTPS_PREFIX.length)
        val host = authority.substringBefore(':')
        val port = authority.substringAfter(':', missingDelimiterValue = "")
        if (authority.contains(':') && !isExplicitPort(port)) return false
        return isHost(host)
    }

    fun accepts(draft: VaultLoginDraft): Boolean =
        isCanonicalOrigin(draft.origin) &&
            draft.formActionOrigin?.let(::isCanonicalOrigin) != false &&
            draft.httpRealm?.let { realm -> realm.length <= MAX_REALM_LENGTH && realm.isPlainText() } != false &&
            draft.username.length <= MAX_USERNAME_LENGTH && draft.username.isPlainText() &&
            draft.password.isNotEmpty() && draft.password.length <= MAX_PASSWORD_LENGTH && '\u0000' !in draft.password

    /** Logins for exactly [origin]; a subdomain or another port is another site. */
    fun loginsFor(logins: List<VaultLogin>, origin: String): List<VaultLogin> =
        logins.filter { login -> login.origin == origin }
            .sortedWith(
                compareByDescending<VaultLogin> { it.lastUsedAtMillis ?: Long.MIN_VALUE }
                    .thenByDescending(VaultLogin::updatedAtMillis),
            )

    /**
     * Logins whose host is [domain] or one of its subdomains. Gecko asks by registrable domain
     * («example.com») so it can tell a page that a login exists; offering one to fill still goes
     * through [loginsFor] with the page's exact origin.
     */
    fun loginsUnderDomain(logins: List<VaultLogin>, domain: String): List<VaultLogin> {
        val wanted = domain.trim().trimEnd('.').lowercase()
        if (!isHost(wanted)) return emptyList()
        return logins.filter { login -> hostOf(login.origin).let { host -> host == wanted || host.endsWith(".$wanted") } }
    }

    /** The login index of [logins]: what may stay readable while the vault is locked. */
    fun hints(logins: List<VaultLogin>): List<VaultLoginHint> =
        sortedForList(logins).map { login ->
            VaultLoginHint(
                id = login.id,
                origin = login.origin,
                formActionOrigin = login.formActionOrigin,
                httpRealm = login.httpRealm,
                username = login.username,
                lastUsedAtMillis = login.lastUsedAtMillis,
            )
        }

    /** Whether [hint] could stand for a real login: the same checks a saved login passes. */
    fun accepts(hint: VaultLoginHint): Boolean =
        hint.id.isNotBlank() && hint.id.length <= MAX_ID_LENGTH && hint.id.isPlainText() &&
            accepts(VaultLoginDraft(hint.origin, hint.formActionOrigin, hint.httpRealm, hint.username, password = "-"))

    /** Hints for exactly [origin], most recently used first; the same rule as [loginsFor]. */
    fun hintsFor(hints: List<VaultLoginHint>, origin: String): List<VaultLoginHint> =
        hints.filter { hint -> hint.origin == origin }
            .sortedByDescending { it.lastUsedAtMillis ?: Long.MIN_VALUE }

    /** Hints whose host is [domain] or one of its subdomains; the same rule as [loginsUnderDomain]. */
    fun hintsUnderDomain(hints: List<VaultLoginHint>, domain: String): List<VaultLoginHint> {
        val wanted = domain.trim().trimEnd('.').lowercase()
        if (!isHost(wanted)) return emptyList()
        return hints.filter { hint -> hostOf(hint.origin).let { host -> host == wanted || host.endsWith(".$wanted") } }
    }

    fun sortedForList(logins: List<VaultLogin>): List<VaultLogin> =
        logins.sortedWith(compareBy(VaultLogin::origin, VaultLogin::username, VaultLogin::id))

    /**
     * Adds [draft] or updates the login it replaces: same origin, same HTTP realm, same user name.
     * A new password for a known user replaces the old one; the same password changes nothing.
     */
    fun save(logins: List<VaultLogin>, draft: VaultLoginDraft, nowMillis: Long, newId: () -> String): VaultChange {
        if (!accepts(draft)) return VaultChange(logins, VaultSaveResult.Rejected)
        val existing = logins.firstOrNull { login ->
            login.origin == draft.origin && login.httpRealm == draft.httpRealm && login.username == draft.username
        }
        if (existing == null) {
            if (logins.size >= MAX_LOGINS) return VaultChange(logins, VaultSaveResult.Rejected)
            val added = VaultLogin(
                id = newId(),
                origin = draft.origin,
                formActionOrigin = draft.formActionOrigin,
                httpRealm = draft.httpRealm,
                username = draft.username,
                password = draft.password,
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
                lastUsedAtMillis = null,
                timesUsed = 0,
            )
            return VaultChange(logins + added, VaultSaveResult.Added(added))
        }
        if (existing.password == draft.password) return VaultChange(logins, VaultSaveResult.Unchanged(existing))
        val updated = existing.copy(
            password = draft.password,
            formActionOrigin = draft.formActionOrigin ?: existing.formActionOrigin,
            updatedAtMillis = nowMillis,
        )
        return VaultChange(logins.map { if (it.id == existing.id) updated else it }, VaultSaveResult.Updated(updated))
    }

    /**
     * [logins] with login [id] replaced by [draft]: the user edited it. Refused when the draft breaks
     * a rule, the login is gone, or another login already has the same origin, realm and user name.
     */
    fun update(logins: List<VaultLogin>, id: String, draft: VaultLoginDraft, nowMillis: Long): VaultChange {
        val existing = logins.firstOrNull { it.id == id }
        if (existing == null || !accepts(draft)) return VaultChange(logins, VaultSaveResult.Rejected)
        val clash = logins.any { login ->
            login.id != id && login.origin == draft.origin &&
                login.httpRealm == draft.httpRealm && login.username == draft.username
        }
        if (clash) return VaultChange(logins, VaultSaveResult.Rejected)
        val updated = existing.copy(
            origin = draft.origin,
            // The form's action belonged to the old site; a hand edit to another site drops it.
            formActionOrigin = existing.formActionOrigin.takeIf { draft.origin == existing.origin },
            httpRealm = draft.httpRealm,
            username = draft.username,
            password = draft.password,
        )
        if (updated == existing) return VaultChange(logins, VaultSaveResult.Unchanged(existing))
        val stamped = updated.copy(updatedAtMillis = nowMillis)
        return VaultChange(logins.map { if (it.id == id) stamped else it }, VaultSaveResult.Updated(stamped))
    }

    /** [logins] with [id] marked as just used, or null if there is no such login. */
    fun markUsed(logins: List<VaultLogin>, id: String, nowMillis: Long): List<VaultLogin>? {
        if (logins.none { it.id == id }) return null
        return logins.map { login ->
            if (login.id != id) login
            else login.copy(
                lastUsedAtMillis = nowMillis,
                timesUsed = if (login.timesUsed == Int.MAX_VALUE) Int.MAX_VALUE else login.timesUsed + 1,
            )
        }
    }

    /** [logins] without [id], or null if there is no such login. */
    fun delete(logins: List<VaultLogin>, id: String): List<VaultLogin>? =
        logins.filterNot { it.id == id }.takeIf { it.size != logins.size }

    private fun hostOf(origin: String): String = origin.removePrefix(HTTPS_PREFIX).substringBefore(':')

    private fun isExplicitPort(port: String): Boolean {
        if (port.isEmpty() || port.length > 5 || port.startsWith('0') || port.any { it !in '0'..'9' }) return false
        val value = port.toInt()
        return value in 1..MAX_PORT && value != DEFAULT_HTTPS_PORT
    }

    private fun isHost(host: String): Boolean {
        if (host.isEmpty() || host.length > MAX_HOST_LENGTH) return false
        return host.split('.').all { label ->
            label.isNotEmpty() && label.length <= MAX_LABEL_LENGTH &&
                !label.startsWith('-') && !label.endsWith('-') &&
                label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
        }
    }

    private fun String.isPlainText(): Boolean = none { char -> char < ' ' || char == '\u007f' }
}
