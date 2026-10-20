package dev.sk2andy.materialbrowser.browser.gecko

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission

class GeckoPromptOnlyPermissionRulesTest {
    @Test
    fun `gecko keeps no answer of its own for location and protected media`() {
        listOf(
            PermissionDelegate.PERMISSION_GEOLOCATION,
            PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS,
        ).forEach { type ->
            assertTrue(GeckoPromptOnlyPermissionRules.needsReset(type, ContentPermission.VALUE_ALLOW))
            assertTrue(GeckoPromptOnlyPermissionRules.needsReset(type, ContentPermission.VALUE_DENY))
            assertFalse(GeckoPromptOnlyPermissionRules.needsReset(type, ContentPermission.VALUE_PROMPT))
        }
    }

    @Test
    fun `notifications and tracking keep their own reconciliation`() {
        listOf(
            PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION,
            PermissionDelegate.PERMISSION_TRACKING,
            PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
        ).forEach { type ->
            assertFalse(GeckoPromptOnlyPermissionRules.needsReset(type, ContentPermission.VALUE_ALLOW))
        }
    }
}
