package dev.sk2andy.materialbrowser.ui

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

    @Test
    fun `core permissions are listed in board order, rare ones only once used`() {
        val visible = SiteInfoRules.visiblePermissions(
            entries = untouched.reversed(),
            notificationsSupported = true,
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

        val withMidi = untouched.map { item ->
            if (item.permission == SitePermission.MidiSysex) {
                item.copy(decision = SitePermissionDecision.Block)
            } else {
                item
            }
        }
        assertEquals(
            SitePermission.MidiSysex,
            SiteInfoRules.visiblePermissions(withMidi, notificationsSupported = true, isPrivate = false)
                .last()
                .permission,
        )
    }

    @Test
    fun `notifications are hidden where they cannot work`() {
        val webView = SiteInfoRules.visiblePermissions(untouched, notificationsSupported = false, isPrivate = false)
        val private = SiteInfoRules.visiblePermissions(untouched, notificationsSupported = true, isPrivate = true)

        assertFalse(webView.any { item -> item.permission == SitePermission.Notifications })
        assertFalse(private.any { item -> item.permission == SitePermission.Notifications })
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
