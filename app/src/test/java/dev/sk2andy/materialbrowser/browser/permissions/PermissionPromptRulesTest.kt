package dev.sk2andy.materialbrowser.browser.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionPromptRulesTest {
    @Test
    fun `the question follows what the site asks for`() {
        assertEquals(PermissionQuestion.Location, PermissionPromptRules.question(setOf(SitePermission.Location)))
        assertEquals(
            PermissionQuestion.CameraAndMicrophone,
            PermissionPromptRules.question(setOf(SitePermission.Microphone, SitePermission.Camera)),
        )
        assertEquals(
            PermissionQuestion.Notifications,
            PermissionPromptRules.question(setOf(SitePermission.Notifications)),
        )
        assertEquals(
            PermissionQuestion.Other,
            PermissionPromptRules.question(setOf(SitePermission.Location, SitePermission.Camera)),
        )
        assertEquals(PermissionQuestion.Other, PermissionPromptRules.question(setOf(SitePermission.MidiSysex)))
    }

    @Test
    fun `notifications are granted for good or not at all`() {
        assertEquals(
            listOf(PermissionPromptChoice.AllowAlways, PermissionPromptChoice.Block),
            PermissionPromptRules.choices(setOf(SitePermission.Notifications)),
        )
        assertEquals(
            listOf(PermissionPromptChoice.AllowAlways, PermissionPromptChoice.AllowOnce, PermissionPromptChoice.Block),
            PermissionPromptRules.choices(setOf(SitePermission.Camera)),
        )
    }

    @Test
    fun `the most sensitive permission heads the sheet`() {
        assertEquals(
            SitePermission.Camera,
            PermissionPromptRules.heroPermission(setOf(SitePermission.Location, SitePermission.Camera)),
        )
        assertEquals(SitePermission.Location, PermissionPromptRules.heroPermission(emptySet()))
    }
}
