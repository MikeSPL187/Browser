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
        get() = BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

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
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(purpose.titleResource))
                .setSubtitle(activity.getString(purpose.subtitleResource))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .setNegativeButtonText(activity.getString(R.string.action_cancel))
                .build(),
        )
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
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG
    }
}
