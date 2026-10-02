package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EssentialsGridRulesTest {
    @Test
    fun `slot follows the tile center across columns and rows`() {
        assertEquals(0, slot(x = 10f, y = 10f, count = 8))
        assertEquals(3, slot(x = 390f, y = 10f, count = 8))
        assertEquals(5, slot(x = 150f, y = 130f, count = 8))
    }

    @Test
    fun `slot is clamped to the grid and to the entries`() {
        assertEquals(0, slot(x = -50f, y = -50f, count = 8))
        assertEquals(7, slot(x = 999f, y = 999f, count = 8))
        assertEquals(2, slot(x = 390f, y = 130f, count = 3))
        assertEquals(0, slot(x = 10f, y = 10f, count = 0))
    }

    @Test
    fun `right to left grids count columns from the right`() {
        assertEquals(3, slot(x = 10f, y = 10f, count = 8, rtl = true))
        assertEquals(4, slot(x = 390f, y = 130f, count = 8, rtl = true))
    }

    private fun slot(x: Float, y: Float, count: Int, rtl: Boolean = false) =
        EssentialsGridRules.slotAt(
            x = x,
            y = y,
            pitchX = 100f,
            pitchY = 120f,
            columns = 4,
            count = count,
            rtl = rtl,
        )
}
