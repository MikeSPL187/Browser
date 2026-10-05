package dev.sk2andy.materialbrowser.shared.ui.theme

import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.data.BrowserDensity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListDensityRulesTest {
    @Test
    fun normalKeepsEveryRowAsDesigned() {
        assertEquals(64.dp, ListDensityRules.rowMinHeight(64.dp, BrowserDensity.Normal))
        assertEquals(10.dp, ListDensityRules.verticalPadding(10.dp, BrowserDensity.Normal))
    }

    @Test
    fun compactTightensAndComfortableLoosens() {
        assertEquals(52.dp, ListDensityRules.rowMinHeight(64.dp, BrowserDensity.Compact))
        assertEquals(4.dp, ListDensityRules.verticalPadding(10.dp, BrowserDensity.Compact))
        assertEquals(72.dp, ListDensityRules.rowMinHeight(64.dp, BrowserDensity.Comfortable))
        assertEquals(14.dp, ListDensityRules.verticalPadding(10.dp, BrowserDensity.Comfortable))
    }

    @Test
    fun noRowDropsUnderTheTouchTarget() {
        listOf(44.dp, 52.dp, 64.dp).forEach { base ->
            BrowserDensity.entries.forEach { density ->
                val height = ListDensityRules.rowMinHeight(base, density)
                assertTrue(height >= ListDensityRules.minTouchTarget, "$base $density $height")
            }
        }
        assertEquals(0.dp, ListDensityRules.verticalPadding(4.dp, BrowserDensity.Compact))
    }
}
