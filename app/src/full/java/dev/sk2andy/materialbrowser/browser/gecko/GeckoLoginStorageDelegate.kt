package dev.sk2andy.materialbrowser.browser.gecko

import androidx.annotation.UiThread
import dev.sk2andy.materialbrowser.browser.credentials.CredentialPromptRules
import dev.sk2andy.materialbrowser.browser.credentials.vault.CredentialVaultSession
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVault
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginDraft
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginHint
import java.util.concurrent.Executor
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.GeckoResult

/** How a GeckoView login entry becomes a vault draft; pure, so it is tested without the engine. */
internal object GeckoLoginStorageRules {
    /**
     * What a locked vault reports as the password: Gecko needs one in every entry, and the real one
     * leaves the vault only for the account the user picks.
     */
    const val LOCKED_PASSWORD = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"

    fun draft(
        origin: String?,
        formActionOrigin: String?,
        httpRealm: String?,
        username: String?,
        password: String?,
    ): VaultLoginDraft? {
        val canonicalOrigin = CredentialPromptRules.canonicalHttpsOrigin(origin) ?: return null
        // A form that posts to plain http or nowhere readable keeps only the page origin.
        val canonicalAction = formActionOrigin?.takeIf(String::isNotBlank)?.let(CredentialPromptRules::canonicalHttpsOrigin)
        // A locked vault's placeholder coming back is never a password to keep.
        if (password == LOCKED_PASSWORD) return null
        return VaultLoginDraft(
            origin = canonicalOrigin,
            formActionOrigin = canonicalAction,
            httpRealm = httpRealm?.takeIf(String::isNotEmpty),
            username = username.orEmpty(),
            password = password ?: return null,
        ).takeIf(CredentialVaultRules::accepts)
    }
}

/**
 * GeckoView's login storage, backed by the browser's own [CredentialVault].
 *
 * - Fetch answers Gecko's registrable-domain query, so the page can offer its saved accounts and the
 *   save prompt can tell «update» from «new». While the vault is locked the answer comes from the
 *   login index: the right accounts with a placeholder instead of the password. Filling still goes
 *   through the login prompt, which offers only the page's exact origin and opens the vault for the
 *   picked account; Gecko's own silent autofill stays off ([GeckoRuntimeSettingsFactory]), so the
 *   placeholder is never typed into a page.
 * - Fetching every login is refused: nothing in the engine needs the whole vault.
 * - Saves arrive only after the user confirmed the save prompt, which opens the vault first and
 *   which private tabs never show.
 * - Writes run on [io]; reads use the open vault in memory or the index.
 */
internal class GeckoLoginStorageDelegate(
    private val vault: CredentialVault,
    private val io: Executor,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Whether the vault is open now; an expired session locks first ([CredentialVaultSession.isOpen]). */
    private val isOpen: () -> Boolean = { CredentialVaultSession.isOpen(vault) },
) : Autocomplete.StorageDelegate {
    @UiThread
    override fun onLoginFetch(domain: String): GeckoResult<Array<Autocomplete.LoginEntry>> {
        val entries = if (isOpen()) {
            CredentialVaultRules.loginsUnderDomain(vault.allLogins(), domain).map(::entry)
        } else {
            CredentialVaultRules.hintsUnderDomain(vault.loginHints(), domain).map(::lockedEntry)
        }
        return GeckoResult.fromValue(entries.toTypedArray())
    }

    @UiThread
    override fun onLoginFetch(): GeckoResult<Array<Autocomplete.LoginEntry>> = GeckoResult.fromValue(emptyArray())

    @UiThread
    override fun onLoginSave(login: Autocomplete.LoginEntry) {
        val draft = GeckoLoginStorageRules.draft(
            origin = login.origin,
            formActionOrigin = login.formActionOrigin,
            httpRealm = login.httpRealm,
            username = login.username,
            password = login.password,
        ) ?: return
        val now = clock()
        io.execute { vault.save(draft, now) }
    }

    @UiThread
    override fun onLoginUsed(login: Autocomplete.LoginEntry, usedFields: Int) {
        val id = login.guid ?: return
        if (!isOpen()) return
        // A login in use keeps the vault open for the next page of the same sign-in.
        CredentialVaultSession.touch(vault)
        val now = clock()
        io.execute { vault.markUsed(id, now) }
    }

    private fun entry(login: VaultLogin): Autocomplete.LoginEntry = Autocomplete.LoginEntry.Builder()
        .guid(login.id)
        .origin(login.origin)
        .formActionOrigin(login.formActionOrigin)
        .httpRealm(login.httpRealm)
        .username(login.username)
        .password(login.password)
        .build()

    private fun lockedEntry(hint: VaultLoginHint): Autocomplete.LoginEntry = Autocomplete.LoginEntry.Builder()
        .guid(hint.id)
        .origin(hint.origin)
        .formActionOrigin(hint.formActionOrigin)
        .httpRealm(hint.httpRealm)
        .username(hint.username)
        .password(GeckoLoginStorageRules.LOCKED_PASSWORD)
        .build()
}
