package dev.sk2andy.materialbrowser.browser.integration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultBrowserRoleRulesTest {
    @Test
    fun `the role dialog is asked for only while the role is available and not held`() {
        assertTrue(DefaultBrowserRoleRules.canRequest(roleAvailable = true, roleHeld = false))
        assertFalse(DefaultBrowserRoleRules.canRequest(roleAvailable = true, roleHeld = true))
        assertFalse(DefaultBrowserRoleRules.canRequest(roleAvailable = false, roleHeld = false))
    }

    @Test
    fun `a silent refusal falls back to the settings`() {
        assertTrue(DefaultBrowserRoleRules.openSettingsAfterAnswer(roleHeld = false, answeredAfterMillis = 40L))
    }

    @Test
    fun `a granted role or a refusal the user made opens nothing more`() {
        assertFalse(DefaultBrowserRoleRules.openSettingsAfterAnswer(roleHeld = true, answeredAfterMillis = 40L))
        assertFalse(
            DefaultBrowserRoleRules.openSettingsAfterAnswer(
                roleHeld = false,
                answeredAfterMillis = DefaultBrowserRoleRules.SILENT_REFUSAL_MILLIS + 2_000L,
            ),
        )
    }
}
