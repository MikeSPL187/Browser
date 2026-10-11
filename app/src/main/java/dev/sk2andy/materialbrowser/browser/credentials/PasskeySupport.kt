package dev.sk2andy.materialbrowser.browser.credentials

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Whether this device can sign in with passkeys in the Gecko engine, and why not. */
enum class PasskeyAvailability {
    Available,

    /** Android 12 and 13: Gecko would need Google Play services FIDO, which Vola does not ship. */
    NeedsAndroid14,

    /** Android 14+ without the system Credential Manager (some custom or stripped firmware). */
    NoCredentialManager,
}

/**
 * Passkeys option A (`docs/vola/research/passkeys.md`): Gecko signs in through Android's
 * Credential Manager and whatever provider the person installed (Bitwarden, KeePassDX, Google
 * Password Manager). That manager exists from Android 14, so older devices keep WebAuthn off.
 */
object PasskeySupport {
    /** Android 14, the first release whose framework Credential Manager GeckoView calls. */
    const val MIN_SDK = 34

    /** `PackageManager.FEATURE_CREDENTIALS`, spelled out so the check compiles below API 34. */
    const val CREDENTIALS_FEATURE = "android.software.credentials"

    fun availability(sdkInt: Int, hasCredentialManager: Boolean): PasskeyAvailability = when {
        sdkInt < MIN_SDK -> PasskeyAvailability.NeedsAndroid14
        !hasCredentialManager -> PasskeyAvailability.NoCredentialManager
        else -> PasskeyAvailability.Available
    }

    fun current(context: Context): PasskeyAvailability {
        val sdkInt = Build.VERSION.SDK_INT
        val packageManager: PackageManager = context.packageManager
        return availability(
            sdkInt = sdkInt,
            hasCredentialManager = sdkInt >= MIN_SDK && packageManager.hasSystemFeature(CREDENTIALS_FEATURE),
        )
    }
}
