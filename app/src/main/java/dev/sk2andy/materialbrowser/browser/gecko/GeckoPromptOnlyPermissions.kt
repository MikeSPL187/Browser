package dev.sk2andy.materialbrowser.browser.gecko

import android.os.Handler
import android.os.Looper
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission
import org.mozilla.geckoview.StorageController

/**
 * Location and protected media (DRM) are decided by Vola per profile and origin: allow once, allow,
 * block. GeckoView stores whatever the delegate answers in its own permission manager, and a stored
 * answer makes Gecko skip the delegate next time — and with it "allow once", a later revoke in Vola
 * and the checks that the tab is in front. Gecko therefore keeps no answer of its own for these:
 * each is reset to "prompt" once stored, and any left from before is reset at start (V29).
 */
internal object GeckoPromptOnlyPermissionRules {
    private val types = setOf(
        PermissionDelegate.PERMISSION_GEOLOCATION,
        PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS,
    )

    fun isPromptOnly(type: Int): Boolean = type in types

    fun needsReset(type: Int, value: Int): Boolean =
        isPromptOnly(type) && value != ContentPermission.VALUE_PROMPT
}

internal class GeckoPromptOnlyPermissions(private val storageController: StorageController) {
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Resets every stored answer for these permissions, such as one kept by an older Vola. */
    fun resetAll() {
        storageController.getAllPermissions()
            .withHandler(mainHandler)
            .accept({ permissions -> permissions.orEmpty().filter(::needsReset).forEach(::reset) }, {})
    }

    /**
     * Resets what Gecko stores for [answered] once it has stored it. Gecko writes the answer a moment
     * after the delegate returns it, so the store is read a few times before giving up.
     */
    fun resetAfterAnswer(answered: ContentPermission, attempt: Int = 0) {
        if (!GeckoPromptOnlyPermissionRules.isPromptOnly(answered.permission)) return
        storageController.getPermissions(answered.uri, answered.contextId, answered.privateMode)
            .withHandler(mainHandler)
            .accept(
                { permissions ->
                    val stored = permissions.orEmpty().filter { permission ->
                        permission.permission == answered.permission && needsReset(permission)
                    }
                    stored.forEach(::reset)
                    if (attempt + 1 < MAX_ATTEMPTS) {
                        mainHandler.postDelayed({ resetAfterAnswer(answered, attempt + 1) }, RETRY_MILLIS)
                    }
                },
                {},
            )
    }

    private fun needsReset(permission: ContentPermission): Boolean =
        GeckoPromptOnlyPermissionRules.needsReset(permission.permission, permission.value)

    private fun reset(permission: ContentPermission) {
        storageController.setPermission(permission, ContentPermission.VALUE_PROMPT)
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
        const val RETRY_MILLIS = 200L
    }
}
