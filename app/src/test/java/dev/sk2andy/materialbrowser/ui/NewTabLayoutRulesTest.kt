package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import org.junit.Assert.assertEquals
import org.junit.Test

class NewTabLayoutRulesTest {
    @Test
    fun theLastCardClearsTheFloatingAddressBarAndItsMargins() {
        assertEquals(
            56.dp + ADDRESS_BAR_VERTICAL_MARGIN * 2,
            NewTabLayoutRules.bottomContentPadding(addressBarDocked = false, addressBarHeight = 56.dp),
        )
    }

    @Test
    fun aDockedBarLeavesThePlainGap() {
        assertEquals(
            VolaSpacing.x12,
            NewTabLayoutRules.bottomContentPadding(addressBarDocked = true, addressBarHeight = 56.dp),
        )
    }

    @Test
    fun theGapIsNeverSmallerThanBefore() {
        assertEquals(
            VolaSpacing.x12,
            NewTabLayoutRules.bottomContentPadding(addressBarDocked = false, addressBarHeight = 0.dp),
        )
    }
}
