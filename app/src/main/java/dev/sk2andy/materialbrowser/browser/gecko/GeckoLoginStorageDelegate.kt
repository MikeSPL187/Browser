package dev.sk2andy.materialbrowser.browser.gecko

import androidx.annotation.UiThread
import dev.sk2andy.materialbrowser.browser.credentials.CredentialPromptRules
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVault
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginDraft
import java.util.concurrent.Executor
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.GeckoResult

/** How a GeckoView login entry becomes a vault draft; pure, so it is tested without the engine. */
internal object GeckoLoginStorageRules {
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
 * - Fetch answers Gecko's registrable-domain query, so the page learns a login exists and the
 *   save prompt can tell «update» from «new». Filling still goes through the login prompt, which
 *   offers only the page's exact origin; Gecko's own silent autofill stays off while the vault is
 *   in use ([GeckoRuntimeSettingsFactory]).
 * - Fetching every login is refused: nothing in the engine needs the whole vault.
 * - Saves arrive only after the user confirmed Gecko's save prompt, which private tabs never show.
 * - Writes run on [io]; reads use the open vault in memory. A locked vault answers nothing.
 */
internal class GeckoLoginStorageDelegate(
    private val vault: CredentialVault,
    private val io: Executor,
    private val clock: () -> Long = System::currentTimeMillis,
) : Autocomplete.StorageDelegate {
    @UiThread
    override fun onLoginFetch(domain: String): GeckoResult<Array<Autocomplete.LoginEntry>> =
        GeckoResult.fromValue(
            CredentialVaultRules.loginsUnderDomain(vault.allLogins(), domain).map(::entry).toTypedArray(),
        )

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
}
