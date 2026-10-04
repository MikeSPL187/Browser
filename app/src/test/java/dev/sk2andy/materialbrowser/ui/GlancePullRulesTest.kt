package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlancePullRulesTest {
    @Test
    fun `card follows the finger up but not down`() {
        assertEquals(-40f, GlancePullRules.offsetAfterDrag(0f, -40f))
        assertEquals(-60f, GlancePullRules.offsetAfterDrag(-40f, -20f))
        assertEquals(-10f, GlancePullRules.offsetAfterDrag(-40f, 30f))
        assertEquals(0f, GlancePullRules.offsetAfterDrag(-10f, 50f))
        assertEquals(0f, GlancePullRules.offsetAfterDrag(0f, 80f))
    }

    @Test
    fun `only a pull past the threshold opens the link`() {
        assertTrue(GlancePullRules.opensOnRelease(-120f, 100f))
        assertTrue(GlancePullRules.opensOnRelease(-100f, 100f))
        assertFalse(GlancePullRules.opensOnRelease(-99f, 100f))
        assertFalse(GlancePullRules.opensOnRelease(0f, 100f))
        assertFalse(GlancePullRules.opensOnRelease(-10f, 0f))
    }
}
