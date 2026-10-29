package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLayerRulesTest {
    @Test
    fun `a page and its More screen split the settings`() {
        SettingsLayerRules.moreOf.forEach { (more, page) ->
            assertEquals(SettingsLayer.Main, SettingsLayerRules.layer(page))
            assertEquals(SettingsLayer.More, SettingsLayerRules.layer(more))
            assertEquals(page, more.parent)
        }
        assertTrue(SettingsLayer.Main.main && !SettingsLayer.Main.more)
        assertTrue(SettingsLayer.More.more && !SettingsLayer.More.main)
    }
}
