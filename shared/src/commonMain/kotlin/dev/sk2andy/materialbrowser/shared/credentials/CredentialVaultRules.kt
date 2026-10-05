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
