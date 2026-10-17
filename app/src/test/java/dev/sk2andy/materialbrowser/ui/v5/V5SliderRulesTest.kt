package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Test

class V5SliderRulesTest {
    @Test
    fun `the fill ends at the value and stays on the track`() {
        assertEquals(0.5f, V5SliderRules.fraction(150f, 50f..250f), 0.0001f)
        assertEquals(0f, V5SliderRules.fraction(10f, 50f..250f), 0f)
        assertEquals(1f, V5SliderRules.fraction(300f, 50f..250f), 0f)
        assertEquals("an empty range has no fill", 0f, V5SliderRules.fraction(5f, 5f..5f), 0f)
    }

    @Test
    fun `stops count both ends and the steps between them`() {
        // 50..250 by 10: 19 steps between the ends, 21 stops in all.
        assertEquals(0, V5SliderRules.stop(50f, 50f..250f, 19))
        assertEquals(8, V5SliderRules.stop(130f, 50f..250f, 19))
        assertEquals(20, V5SliderRules.stop(250f, 50f..250f, 19))
        assertEquals("no steps: just the two ends", 1, V5SliderRules.stop(0.6f, 0f..1f, 0))
    }
}
