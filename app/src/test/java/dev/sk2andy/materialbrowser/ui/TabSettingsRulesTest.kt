package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.TabOverviewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabSettingsRulesTest {
    @Test
    fun `only the hero overview cannot start the list at the bottom`() {
        assertTrue(TabSettingsRules.listCanStartAtBottom(TabOverviewMode.Grid))
        assertTrue(TabSettingsRules.listCanStartAtBottom(TabOverviewMode.List))
        assertFalse(TabSettingsRules.listCanStartAtBottom(TabOverviewMode.Hero))
    }

    @Test
    fun `slider steps count the stops between the ends`() {
        // 10, 20, … 90: seven stops between the ends, as before the cards.
        assertEquals(
            7,
            SettingsSliderRules.steps(
                TabSettingsRules.DismissResistanceRange,
                TabSettingsRules.DISMISS_RESISTANCE_STEP,
            ),
        )
        assertEquals(3, SettingsSliderRules.steps(2..6, 1))
        assertEquals(0, SettingsSliderRules.steps(4..5, 1))
        assertEquals(0, SettingsSliderRules.steps(4..4, 1))
    }

    @Test
    fun `under v5 the page is just Tabs, its other half More`() {
        assertEquals(R.string.settings_tabs_gestures_title, TabSettingsRules.title(SettingsLayer.All))
        assertEquals(R.string.settings_section_tabs, TabSettingsRules.title(SettingsLayer.Main))
        assertEquals(R.string.settings_appearance_advanced, TabSettingsRules.title(SettingsLayer.More))
    }
}
