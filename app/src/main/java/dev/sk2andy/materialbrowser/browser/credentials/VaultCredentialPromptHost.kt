package dev.sk2andy.materialbrowser.browser.credentials

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.vault.AndroidCredentialVault
import dev.sk2andy.materialbrowser.browser.credentials.vault.CredentialVaultSession
import dev.sk2andy.materialbrowser.browser.credentials.vault.KeystoreVaultKeyWrapper
import dev.sk2andy.materialbrowser.browser.credentials.vault.LocalCredentialVault
import dev.sk2andy.materialbrowser.browser.credentials.vault.VaultOpenResult
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginHint
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the Vola host decides before it shows anything; pure, so it is tested without a device. */
internal object VaultPromptRules {
    /** What to ask after a sign-in: nothing, «Save password?» or «Update password?». */
    enum class SaveQuestion { None, Save, Update }

    /**
     * [openLogins] are the vault's logins for [origin] when it is open, null when it is locked. A
     * locked vault cannot compare passwords, so a known user name asks to update; the vault then
     * leaves an unchanged password as it is.
     */
    fun saveQuestion(
        hints: List<VaultLoginHint>,
        openLogins: List<VaultLogin>?,
        origin: String,
        login: CredentialLogin,
    ): SaveQuestion {
        if (openLogins != null) {
            val saved = openLogins.firstOrNull { it.httpRealm == null && it.username == login.username }
            return when {
                saved == null -> SaveQuestion.Save
                saved.password == login.password -> SaveQuestion.None
                else -> SaveQuestion.Update
            }
        }
        val known = CredentialVaultRules.hintsFor(hints, origin)
            .any { it.httpRealm == null && it.username == login.username }
        return if (known) SaveQuestion.Update else SaveQuestion.Save
    }

    /** Accounts to offer on exactly [origin], most recently used first, one per user name. */
    fun accounts(
        hints: List<VaultLoginHint>,
        origin: String,
        allowedUserIds: Set<String>,
    ): List<VaultLoginAccount> = CredentialVaultRules.hintsFor(hints, origin)
        .filter { hint -> hint.httpRealm == null }
        .filter { hint -> allowedUserIds.isEmpty() || hint.username in allowedUserIds }
        .distinctBy(VaultLoginHint::username)
        .map { hint -> VaultLoginAccount(hint.username, hint.lastUsedAtMillis) }

    /** The open vault's login for [username] on [origin]; null when there is none. */
    fun login(logins: List<VaultLogin>, username: String): VaultLogin? =
        logins.firstOrNull { it.httpRealm == null && it.username == username }

    /**
     * The site as the Passwords screen shows it: the ASCII host (an international name stays in
     * punycode), so a look-alike spelling cannot pass for the real site.
     */
    fun displaySite(origin: String): String = origin.removePrefix("https://")
}

/**
 * Logins from Vola's own vault, shown in Vola's own sheets. Until the vault is set up on this phone
 * every request goes to [fallback], the system Credential Manager, so nothing changes for people
 * who never turned Passwords on. Identity (FedCM) prompts always go to [fallback].
 *
 * - Saving asks «Save password?» or «Update password?» and opens the vault with the fingerprint or
 *   screen lock before Gecko writes the login.
 * - Signing in lists the accounts for exactly this site from the login index at once; the
 *   fingerprint is asked only when an account is picked, and only that login's password leaves the
 *   vault.
 * - Nothing here keeps a password: it is handed to the engine and forgotten.
 */
internal class VaultCredentialPromptHost(
    private val activity: FragmentActivity,
    private val vault: LocalCredentialVault,
    private val fallback: CredentialPromptHost?,
) : CredentialPromptHost {
    private val windowId = System.identityHashCode(activity)
    private var pendingRequestId: Long? = null
    private var closed = false

    override fun saveLogin(prompt: CredentialLoginSavePrompt, onComplete: (Boolean) -> Unit) {
        if (!vault.exists) {
            fallback?.saveLogin(prompt, onComplete) ?: onComplete(false)
            return
        }
        val origin = CredentialPromptRules.canonicalHttpsOrigin(prompt.identity.origin)
        if (closed || origin == null) {
            onComplete(false)
            return
        }
        val question = VaultPromptRules.saveQuestion(
            hints = vault.loginHints(),
            openLogins = vault.loginsFor(origin).takeIf { vault.isUnlocked },
            origin = origin,
            login = prompt.login,
        )
        if (question == VaultPromptRules.SaveQuestion.None) {
            onComplete(false)
            return
        }
        val done = once(onComplete)
        val request = VaultLoginRequest.Save(
            id = VaultLoginPrompts.nextRequestId(),
            windowId = windowId,
            site = VaultPromptRules.displaySite(origin),
            username = prompt.login.username,
            update = question == VaultPromptRules.SaveQuestion.Update,
        )
        show(request) { answer ->
            if (answer == VaultLoginAnswer.Save) {
                openVault(onOpened = { done(true) }, onFailed = { done(false) })
            } else {
                done(false)
            }
        }
    }

    override fun selectLogin(prompt: CredentialLoginSelectPrompt, onComplete: (CredentialLogin?) -> Unit) {
        if (!vault.exists) {
            fallback?.selectLogin(prompt, onComplete) ?: onComplete(null)
            return
        }
        val origin = CredentialPromptRules.canonicalHttpsOrigin(prompt.identity.origin)
        val accounts = origin?.let { VaultPromptRules.accounts(vault.loginHints(), it, prompt.allowedUserIds) }
        if (closed || origin == null || accounts.isNullOrEmpty()) {
            onComplete(null)
            return
        }
        val done = once(onComplete)
        val request = VaultLoginRequest.Select(
            id = VaultLoginPrompts.nextRequestId(),
            windowId = windowId,
            site = VaultPromptRules.displaySite(origin),
            accounts = accounts,
        )
        show(request) { answer ->
            if (answer is VaultLoginAnswer.Pick) {
                openVault(
                    onOpened = { done(fill(origin, answer.username)) },
                    onFailed = { done(null) },
                )
            } else {
                done(null)
            }
        }
    }

    override fun selectIdentityProvider(prompt: IdentityCredentialProviderPrompt, onComplete: (Int?) -> Unit) {
        fallback?.selectIdentityProvider(prompt, onComplete) ?: onComplete(null)
    }

    override fun selectIdentityAccount(prompt: IdentityCredentialAccountPrompt, onComplete: (Int?) -> Unit) {
        fallback?.selectIdentityAccount(prompt, onComplete) ?: onComplete(null)
    }

    override fun confirmIdentityPrivacyPolicy(prompt: IdentityCredentialPrivacyPrompt, onComplete: (Boolean) -> Unit) {
        fallback?.confirmIdentityPrivacyPolicy(prompt, onComplete) ?: onComplete(false)
    }

    override fun close() {
        if (closed) return
        closed = true
        pendingRequestId?.let { id -> VaultLoginPrompts.answer(id, VaultLoginAnswer.Dismiss) }
        pendingRequestId = null
        fallback?.close()
    }

    private fun show(request: VaultLoginRequest, onAnswer: (VaultLoginAnswer) -> Unit) {
        pendingRequestId = request.id
        VaultLoginPrompts.show(request) { answer ->
            if (pendingRequestId == request.id) pendingRequestId = null
            onAnswer(if (closed) VaultLoginAnswer.Dismiss else answer)
        }
    }

    /** The picked login, marked as used; null when it is no longer in the vault. */
    private fun fill(origin: String, username: String): CredentialLogin? {
        val login = VaultPromptRules.login(vault.loginsFor(origin), username) ?: return null
        val now = System.currentTimeMillis()
        activity.lifecycleScope.launch(Dispatchers.IO) { vault.markUsed(login.id, now) }
        return CredentialPromptRules.login(login.username, login.password)
    }

    /** Opens the vault with the fingerprint or screen lock, unless it is open already. */
    private fun openVault(onOpened: () -> Unit, onFailed: () -> Unit) {
        if (closed) {
            onFailed()
            return
        }
        if (vault.isUnlocked) {
            CredentialVaultSession.touch(vault)
            onOpened()
            return
        }
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onFailed()
            return
        }
        val finished = AtomicBoolean(false)
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    activity.lifecycleScope.launch {
                        val opened = withContext(Dispatchers.IO) {
                            vault.unlock(KeystoreVaultKeyWrapper()) == VaultOpenResult.Opened
                        }
                        if (!finished.compareAndSet(false, true)) return@launch
                        if (opened && !closed) {
                            CredentialVaultSession.touch(vault)
                            onOpened()
                        } else {
                            onFailed()
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (finished.compareAndSet(false, true)) onFailed()
                }
            },
        )
        runCatching {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.passwords_prompt_title))
                    .setSubtitle(activity.getString(R.string.passwords_prompt_subtitle))
                    .setAllowedAuthenticators(authenticators)
                    .build(),
            )
        }.onFailure { if (finished.compareAndSet(false, true)) onFailed() }
    }

    private fun <T> once(onComplete: (T) -> Unit): (T) -> Unit {
        val completed = AtomicBoolean(false)
        return { value -> if (completed.compareAndSet(false, true)) onComplete(value) }
    }

    companion object {
        /**
         * The host for a browser window: Vola's own over the system one. Without a window that can
         * show a fingerprint prompt, the system host alone.
         */
        fun create(context: Context): CredentialPromptHost? {
            val fallback = AndroidCredentialPromptHost.create(context)
            val activity = AndroidCredentialPromptHost.activityContext(context) as? FragmentActivity
                ?: return fallback
            return VaultCredentialPromptHost(activity, AndroidCredentialVault.get(activity), fallback)
        }
    }
}
