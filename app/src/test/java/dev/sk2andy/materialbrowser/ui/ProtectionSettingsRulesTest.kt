package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionSettingsRulesTest {
    @Test
    fun `the current interface keeps everything on one page`() {
        assertEquals(
            ProtectionSettingsLayer.All,
            ProtectionSettingsRules.layer(uiV5 = false, SettingsDestination.ProtectionAndData),
        )
        assertTrue(ProtectionSettingsLayer.All.main && ProtectionSettingsLayer.All.more)
    }

    @Test
    fun `under v5 the page and its More page split the settings`() {
        val main = ProtectionSettingsRules.layer(uiV5 = true, SettingsDestination.ProtectionAndData)
        val more = ProtectionSettingsRules.layer(uiV5 = true, SettingsDestination.ProtectionMore)
        assertEquals(ProtectionSettingsLayer.Main, main)
        assertEquals(ProtectionSettingsLayer.More, more)
        assertTrue(main.main && !main.more)
        assertTrue(more.more && !more.main)
        assertFalse(main == more)
    }
}
