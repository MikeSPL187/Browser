package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V5ProfileDotsRulesTest {
    @Test
    fun `up to five profiles show all their dots, more a window around the active one`() {
        assertEquals(0 until 3, V5ProfileDotsRules.window(V5ProfileDots(3, 1)))
        assertEquals(0 until 5, V5ProfileDotsRules.window(V5ProfileDots(5, 4)))
        assertEquals(0 until 5, V5ProfileDotsRules.window(V5ProfileDots(8, 1)))
        assertEquals(2 until 7, V5ProfileDotsRules.window(V5ProfileDots(8, 4)))
        assertEquals(3 until 8, V5ProfileDotsRules.window(V5ProfileDots(8, 7)))
    }

    @Test
    fun `a swipe toward the start goes to the next profile, past 30 percent or flung`() {
        val width = 400f
        assertEquals(1, V5ProfileDotsRules.step(-130f, 0f, width, density = 1f, rtl = false))
        assertEquals(-1, V5ProfileDotsRules.step(130f, 0f, width, density = 1f, rtl = false))
        assertEquals(0, V5ProfileDotsRules.step(-100f, 0f, width, density = 1f, rtl = false))
        assertEquals(1, V5ProfileDotsRules.step(-40f, -1200f, width, density = 1f, rtl = false))
        // A fling back against the drag does not commit.
        assertEquals(0, V5ProfileDotsRules.step(-40f, 1200f, width, density = 1f, rtl = false))
        // Right to left the pages turn the other way.
        assertEquals(1, V5ProfileDotsRules.step(130f, 0f, width, density = 1f, rtl = true))
    }

    @Test
    fun `the list of profiles does not wrap`() {
        val dots = V5ProfileDots(count = 3, active = 0)
        assertEquals(1, V5ProfileDotsRules.target(dots, 1))
        assertNull(V5ProfileDotsRules.target(dots, -1))
        assertNull(V5ProfileDotsRules.target(dots.copy(active = 2), 1))
        assertNull(V5ProfileDotsRules.target(dots, 0))
    }
}
