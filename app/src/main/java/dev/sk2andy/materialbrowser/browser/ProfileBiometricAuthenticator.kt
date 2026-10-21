package dev.sk2andy.materialbrowser.browser

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import dev.sk2andy.materialbrowser.R

enum class ProfileAuthenticationPurpose {
    Unlock,
    Configure,
    Export,
    UnlockPrivateTabs,
    ConfigurePrivateTabsLock,
}

internal class ProfileBiometricAuthenticator(
    private val activity: AppCompatActivity,
) {
    val isAvailable: Boolean
        get() = authenticators() != null

    /**
     * A strong biometric when the phone has one enrolled; otherwise the phone's own PIN, pattern or
     * password (H-01): a profile locked while a fingerprint existed must still open after the
     * fingerprints are removed, the same way the phone itself opens. Null with neither.
     */
    private fun authenticators(): Int? {
        val manager = BiometricManager.from(activity)
        return listOf(BIOMETRIC, DEVICE_CREDENTIAL).firstOrNull { authenticators ->
            manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    private var pendingResult: ((Boolean) -> Unit)? = null

    fun authenticate(
        purpose: ProfileAuthenticationPurpose,
        onResult: (Boolean) -> Unit,
    ) {
        if (!isAvailable || pendingResult != null) {
            onResult(false)
            return
        }
        pendingResult = onResult
        // A prompt that throws (activity finishing, vendor failure) never calls back; without
        // this the pending result would refuse every later unlock until the process restarts.
        runCatching { showPrompt(purpose) }.onFailure { complete(success = false) }
    }

    private fun showPrompt(purpose: ProfileAuthenticationPurpose) {
        val authenticators = authenticators() ?: return complete(success = false)
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    complete(success = true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    complete(success = false)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(purpose.titleResource))
            .setSubtitle(activity.getString(purpose.subtitleResource))
            .setAllowedAuthenticators(authenticators)
        // The phone's own credential screen has its own way back; a cancel button is for biometrics only.
        if (authenticators == BIOMETRIC) info.setNegativeButtonText(activity.getString(R.string.action_cancel))
        prompt.authenticate(info.build())
    }

    private fun complete(success: Boolean) {
        val result = pendingResult ?: return
        pendingResult = null
        result(success)
    }

    private val ProfileAuthenticationPurpose.titleResource: Int
        get() = when (this) {
            ProfileAuthenticationPurpose.Unlock,
            ProfileAuthenticationPurpose.Configure,
            ProfileAuthenticationPurpose.Export,
            -> R.string.profile_biometric_prompt_title
            ProfileAuthenticationPurpose.UnlockPrivateTabs,
            ProfileAuthenticationPurpose.ConfigurePrivateTabsLock,
            -> R.string.private_tabs_lock_prompt_title
        }

    private val ProfileAuthenticationPurpose.subtitleResource: Int
        get() = when (this) {
            ProfileAuthenticationPurpose.Unlock -> R.string.profile_biometric_prompt_unlock
            ProfileAuthenticationPurpose.Configure -> R.string.profile_biometric_prompt_configure
            ProfileAuthenticationPurpose.Export -> R.string.profile_biometric_prompt_export
            ProfileAuthenticationPurpose.UnlockPrivateTabs -> R.string.private_tabs_lock_prompt_unlock
            ProfileAuthenticationPurpose.ConfigurePrivateTabsLock ->
                R.string.private_tabs_lock_prompt_configure
        }

    private companion object {
        const val BIOMETRIC = BiometricManager.Authenticators.BIOMETRIC_STRONG
        const val DEVICE_CREDENTIAL = BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
