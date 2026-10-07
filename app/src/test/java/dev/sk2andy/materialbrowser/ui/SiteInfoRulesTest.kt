package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineCapabilities
import dev.sk2andy.materialbrowser.browser.permissions.PermissionRadarEntry
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionActivity
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteInfoRulesTest {
    private fun entry(
        permission: SitePermission,
        decision: SitePermissionDecision = SitePermissionDecision.Ask,
        activity: SitePermissionActivity = SitePermissionActivity.Idle,
    ) = PermissionRadarEntry(
        permission = permission,
        decision = decision,
        allowedForSession = false,
        activity = activity,
    )

    private val untouched = SitePermission.entries.map { permission -> entry(permission) }
    private val gecko = AndroidBrowserEngineCapabilities.GeckoView.sitePermissions
    private val webView = AndroidBrowserEngineCapabilities.SystemWebView.sitePermissions

    @Test
    fun `core permissions are listed in board order, rare ones only once used`() {
        val visible = SiteInfoRules.visiblePermissions(
            entries = untouched.reversed(),
            supportedPermissions = gecko,
            isPrivate = false,
        )

        assertEquals(
            listOf(
                SitePermission.Location,
                SitePermission.Camera,
                SitePermission.Microphone,
                SitePermission.Notifications,
            ),
            visible.map(PermissionRadarEntry::permission),
        )

        val withProtectedMedia = untouched.map { item ->
            if (item.permission == SitePermission.ProtectedMedia) {
                item.copy(decision = SitePermissionDecision.Block)
            } else {
                item
            }
        }
        assertEquals(
            SitePermission.ProtectedMedia,
            SiteInfoRules.visiblePermissions(withProtectedMedia, supportedPermissions = gecko, isPrivate = false)
                .last()
                .permission,
        )
    }

    @Test
    fun `notifications are hidden where they cannot work`() {
        val webViewRows = SiteInfoRules.visiblePermissions(untouched, supportedPermissions = webView, isPrivate = false)
        val private = SiteInfoRules.visiblePermissions(untouched, supportedPermissions = gecko, isPrivate = true)

        assertFalse(webViewRows.any { item -> item.permission == SitePermission.Notifications })
        assertFalse(private.any { item -> item.permission == SitePermission.Notifications })
    }

    @Test
    fun `permissions the engine does not route are hidden even with a saved decision`() {
        val decided = SitePermission.entries.map { permission ->
            entry(permission, decision = SitePermissionDecision.Block)
        }

        val geckoRows = SiteInfoRules.visiblePermissions(decided, gecko, isPrivate = false)
            .map(PermissionRadarEntry::permission)
        val webViewRows = SiteInfoRules.visiblePermissions(decided, webView, isPrivate = false)
            .map(PermissionRadarEntry::permission)

        assertFalse(SitePermission.MidiSysex in geckoRows)
        assertTrue(SitePermission.ProtectedMedia in geckoRows)
        assertFalse(SitePermission.MidiSysex in webViewRows)
        assertFalse(SitePermission.ProtectedMedia in webViewRows)
    }

    @Test
    fun `radar keeps unsupported notifications to explain them and drops other unsupported rows`() {
        assertEquals(
            listOf(
                SitePermission.Camera,
                SitePermission.Microphone,
                SitePermission.Location,
                SitePermission.Notifications,
            ),
            SiteInfoRules.radarEntries(untouched, webView).map(PermissionRadarEntry::permission),
        )
        assertEquals(
            listOf(
                SitePermission.Camera,
                SitePermission.Microphone,
                SitePermission.Location,
                SitePermission.Notifications,
                SitePermission.ProtectedMedia,
            ),
            SiteInfoRules.radarEntries(untouched, gecko).map(PermissionRadarEntry::permission),
        )
    }

    @Test
    fun `a decision, a session grant or live use make a permission customized`() {
        assertFalse(SiteInfoRules.isCustomized(entry(SitePermission.Camera)))
        assertTrue(
            SiteInfoRules.isCustomized(entry(SitePermission.Camera, SitePermissionDecision.Allow)),
        )
        assertTrue(
            SiteInfoRules.isCustomized(
                entry(SitePermission.Camera, activity = SitePermissionActivity.Active),
            ),
        )
        assertTrue(
            SiteInfoRules.isCustomized(entry(SitePermission.Camera).copy(allowedForSession = true)),
        )
    }

    @Test
    fun `the gem shows the host's first letter without www`() {
        assertEquals("N", SiteInfoRules.initial("www.north-guide.ru"))
        assertEquals("Ж", SiteInfoRules.initial("журнал.рф"))
        assertEquals("", SiteInfoRules.initial(""))
    }
}
