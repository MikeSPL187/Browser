package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLayerRulesTest {
    @Test
    fun `the current interface keeps everything on one page`() {
        listOf(SettingsDestination.ProtectionAndData, SettingsDestination.Browser).forEach { page ->
            assertEquals(SettingsLayer.All, SettingsLayerRules.layer(uiV5 = false, page))
        }
        assertTrue(SettingsLayer.All.main && SettingsLayer.All.more)
    }

    @Test
    fun `under v5 a page and its More screen split the settings`() {
        SettingsLayerRules.moreOf.forEach { (more, page) ->
            assertEquals(SettingsLayer.Main, SettingsLayerRules.layer(uiV5 = true, page))
            assertEquals(SettingsLayer.More, SettingsLayerRules.layer(uiV5 = true, more))
            assertEquals(page, more.parent)
        }
        assertTrue(SettingsLayer.Main.main && !SettingsLayer.Main.more)
        assertTrue(SettingsLayer.More.more && !SettingsLayer.More.main)
    }

    @Test
    fun `settings speak for v5 on every width while the flag is on`() {
        assertTrue(SettingsLayerRules.speaksForV5(flag = true, smallestWidthDp = 411f))
        assertTrue(SettingsLayerRules.speaksForV5(flag = true, smallestWidthDp = 800f))
        assertTrue(!SettingsLayerRules.speaksForV5(flag = false, smallestWidthDp = 411f))
    }
}
