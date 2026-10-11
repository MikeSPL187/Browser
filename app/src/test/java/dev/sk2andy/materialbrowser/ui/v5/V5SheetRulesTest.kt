package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5SheetRulesTest {
    private val height = 1000f
    private val density = 2f

    @Test
    fun `a sheet follows the finger down and resists going up`() {
        assertEquals(120f, V5SheetRules.drag(100f, 20f), 0.001f)
        assertEquals(-4f, V5SheetRules.drag(0f, -20f), 0.001f)
        assertEquals(-8f, V5SheetRules.drag(-4f, -20f), 0.001f)
        // From below the rest only the part above it is damped.
        assertEquals(-2f, V5SheetRules.drag(10f, -20f), 0.001f)
    }

    @Test
    fun `a sheet closes past a quarter of its height or on a downward fling`() {
        assertTrue(V5SheetRules.dismisses(250f, 0f, height, density))
        assertFalse(V5SheetRules.dismisses(240f, 0f, height, density))
        assertTrue(V5SheetRules.dismisses(40f, 2000f, height, density))
        assertFalse(V5SheetRules.dismisses(0f, 5000f, height, density))
        // Flung back up, it stays even when dragged far.
        assertFalse(V5SheetRules.dismisses(400f, -2000f, height, density))
        assertFalse(V5SheetRules.dismisses(400f, 0f, 0f, density))
    }

    @Test
    fun `the scrim fades with presence and travel`() {
        assertEquals(1f, V5SheetRules.scrimAlpha(1f, 0f, height), 0.001f)
        assertEquals(0.5f, V5SheetRules.scrimAlpha(1f, 500f, height), 0.001f)
        assertEquals(0.25f, V5SheetRules.scrimAlpha(0.5f, 500f, height), 0.001f)
        assertEquals(0f, V5SheetRules.scrimAlpha(0f, 0f, height), 0.001f)
        assertEquals(1f, V5SheetRules.scrimAlpha(1f, -10f, height), 0.001f)
    }
}
