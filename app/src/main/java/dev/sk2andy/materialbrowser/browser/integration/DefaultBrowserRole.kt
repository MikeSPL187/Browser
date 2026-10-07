package dev.sk2andy.materialbrowser.browser.integration

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

object DefaultBrowserRole {
    private const val REQUEST_KEY = "vola_default_browser_role"

    fun isHeld(context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
            roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
    }

    /**
     * Makes Vola the default browser: the system's own «Set as default» dialog when the browser
     * role can be asked for, otherwise the Default apps settings. False when neither opened.
     * Whether it worked shows on return, when the browser checks the role again.
     */
    fun request(activity: Activity): Boolean {
        val roleManager = activity.getSystemService(RoleManager::class.java)
        val asked = activity is ComponentActivity && roleManager != null &&
            DefaultBrowserRoleRules.canRequest(
                roleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER),
                roleHeld = roleManager.isRoleHeld(RoleManager.ROLE_BROWSER),
            ) &&
            requestRole(activity, roleManager)
        return asked || openSettings(activity)
    }

    fun openSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    /** The role dialog must be started for a result; the launcher lives only until it answers. */
    private fun requestRole(activity: ComponentActivity, roleManager: RoleManager): Boolean {
        val startedAt = SystemClock.elapsedRealtime()
        var launcher: ActivityResultLauncher<Intent>? = null
        launcher = activity.activityResultRegistry.register(
            REQUEST_KEY,
            ActivityResultContracts.StartActivityForResult(),
        ) {
            launcher?.unregister()
            val fallBack = DefaultBrowserRoleRules.openSettingsAfterAnswer(
                roleHeld = isHeld(activity),
                answeredAfterMillis = SystemClock.elapsedRealtime() - startedAt,
            )
            if (fallBack) openSettings(activity)
        }
        return try {
            launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
            true
        } catch (_: ActivityNotFoundException) {
            launcher.unregister()
            false
        }
    }
}

internal object DefaultBrowserRoleRules {
    /** Faster than anyone can read the dialog and refuse: the system answered without showing it. */
    const val SILENT_REFUSAL_MILLIS = 500L

    /** The dialog only makes sense while the role exists and is someone else's. */
    fun canRequest(roleAvailable: Boolean, roleHeld: Boolean): Boolean = roleAvailable && !roleHeld

    /**
     * After the dialog: a refusal that came back at once was the system's («Don't ask again»), so
     * the Default apps settings open instead and the button never goes dead. A refusal the user
     * made stays respected.
     */
    fun openSettingsAfterAnswer(roleHeld: Boolean, answeredAfterMillis: Long): Boolean =
        !roleHeld && answeredAfterMillis < SILENT_REFUSAL_MILLIS
}
