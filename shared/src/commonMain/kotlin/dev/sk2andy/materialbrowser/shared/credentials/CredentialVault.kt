package dev.sk2andy.materialbrowser.shared.credentials

/**
 * One saved sign-in. [origin] is the exact page origin it belongs to (`https://host[:port]`), so a
 * login for `accounts.example.com` is never offered on `example.com` or on any other subdomain.
 *
 * The password stays out of logs: [toString] redacts it and the user name.
 */
data class VaultLogin(
    val id: String,
    val origin: String,
    val formActionOrigin: String?,
    val httpRealm: String?,
    val username: String,
    val password: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val lastUsedAtMillis: Long?,
    val timesUsed: Int,
) {
    override fun toString(): String =
        "VaultLogin(id=$id, origin=$origin, username=<redacted>, password=<redacted>)"
}

/** A sign-in a page asks to save: what the engine saw, before the vault gives it an identity. */
data class VaultLoginDraft(
    val origin: String,
    val formActionOrigin: String?,
    val httpRealm: String?,
    val username: String,
    val password: String,
) {
    override fun toString(): String =
        "VaultLoginDraft(origin=$origin, username=<redacted>, password=<redacted>)"
}

sealed interface VaultSaveResult {
    data class Added(val login: VaultLogin) : VaultSaveResult

    data class Updated(val login: VaultLogin) : VaultSaveResult

    /** The same user name and password are already saved for this origin. */
    data class Unchanged(val login: VaultLogin) : VaultSaveResult

    /** Saving needs the vault open; nothing was written. */
    data object Locked : VaultSaveResult

    /** The draft breaks a rule in [CredentialVaultRules] or the vault is full. */
    data object Rejected : VaultSaveResult

    /** The vault could not be written; the saved logins are as they were. */
    data object Failed : VaultSaveResult
}

/**
 * The browser's own password vault, independent of any engine: GeckoView and WebView adapters
 * talk to this port, never to the storage behind it.
 *
 * Every read returns nothing and every change is refused while the vault is locked. Opening it is
 * the platform's job (device authentication or the recovery phrase), not this port's.
 */
interface CredentialVault {
    val isUnlocked: Boolean

    /** Logins saved for exactly [origin], most recently used first; empty while locked. */
    fun loginsFor(origin: String): List<VaultLogin>

    /** Every saved login, by origin; empty while locked. */
    fun allLogins(): List<VaultLogin>

    fun save(draft: VaultLoginDraft, nowMillis: Long): VaultSaveResult

    /** Records that [id] filled a form; false if it is unknown, the vault is locked or the write failed. */
    fun markUsed(id: String, nowMillis: Long): Boolean

    fun delete(id: String): Boolean

    /** Forgets the open vault's key and logins until it is opened again. */
    fun lock()
}
