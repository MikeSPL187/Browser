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
    /** The two-factor key as a canonical `otpauth://` link ([TotpRules.canonical]); null without one. */
    val totp: String? = null,
) {
    override fun toString(): String =
        "VaultLogin(id=$id, origin=$origin, username=<redacted>, password=<redacted>, totp=${if (totp == null) "none" else "<redacted>"})"
}

/**
 * What the browser may know about a login while the vault is locked: its site and user name, never
 * its password. The login index keeps these under a device key that needs no fingerprint, so a page
 * can offer «Sign in as …» at once and ask for the fingerprint only when a login is picked.
 */
data class VaultLoginHint(
    val id: String,
    val origin: String,
    val formActionOrigin: String?,
    val httpRealm: String?,
    val username: String,
    val lastUsedAtMillis: Long?,
) {
    override fun toString(): String = "VaultLoginHint(id=$id, origin=$origin, username=<redacted>)"
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
 * A login the export has with another password than the vault: [loginId]'s saved password was kept,
 * and [password] is the export's, offered to the user as a replacement.
 */
class ImportConflict(val loginId: String, val password: String) {
    override fun equals(other: Any?): Boolean =
        other is ImportConflict && other.loginId == loginId && other.password == password

    override fun hashCode(): Int = 31 * loginId.hashCode() + password.hashCode()

    override fun toString(): String = "ImportConflict(loginId=$loginId, password=<redacted>)"
}

/** What an import did: the logins it added, and what it left alone. */
data class VaultImportSummary(
    val addedIds: List<String>,
    /** Already in the vault (same site, realm and user name); the saved password was kept. */
    val duplicates: Int,
    /** Refused by the vault's rules. */
    val rejected: Int,
    /** Logins that came with a two-factor key, new ones and ones that had none yet. */
    val withTotp: Int,
    /** The [duplicates] whose password in the export differs from the saved one, one per login. */
    val conflicts: List<ImportConflict> = emptyList(),
    /** New logins left out because the vault already holds [CredentialVaultRules.MAX_LOGINS]. */
    val full: Int = 0,
) {
    val added: Int get() = addedIds.size
}

sealed interface VaultImportResult {
    data class Imported(val summary: VaultImportSummary) : VaultImportResult

    data object Locked : VaultImportResult

    /** The vault could not be written; nothing was imported. */
    data object Failed : VaultImportResult
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

    /**
     * Site and user name of every saved login, open or locked (from the login index); never a
     * password. Empty when there is no vault.
     */
    fun loginHints(): List<VaultLoginHint>

    fun save(draft: VaultLoginDraft, nowMillis: Long): VaultSaveResult

    /** Edits login [id] by hand: its site, user name or password. Refused if it would duplicate another. */
    fun update(id: String, draft: VaultLoginDraft, nowMillis: Long): VaultSaveResult

    /**
     * Gives login [id] a two-factor key, or removes it with null. [totp] must be a canonical link
     * ([TotpRules.canonical]); false when it is not, the login is unknown, or the vault is locked.
     */
    fun setTotp(id: String, totp: String?, nowMillis: Long): Boolean

    /**
     * Adds [logins] from another manager's export in one write. A login the vault already has keeps
     * its password; it only gains a two-factor key when it had none.
     */
    fun importLogins(logins: List<ImportedLogin>, nowMillis: Long): VaultImportResult

    /**
     * Gives each login in [conflicts] the export's password, in one write: the user chose the
     * export's passwords after an import. False when the vault is locked or the write failed.
     */
    fun replacePasswords(conflicts: List<ImportConflict>, nowMillis: Long): Boolean

    /** Records that [id] filled a form; false if it is unknown, the vault is locked or the write failed. */
    fun markUsed(id: String, nowMillis: Long): Boolean

    fun delete(id: String): Boolean

    /** Forgets the open vault's key and logins until it is opened again. */
    fun lock()
}
