package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandyAnimationRulesTest {
    @Test
    fun `global setting overrides startup animation`() {
        assertFalse(
            CandyAnimationRules.startupAnimationEnabled(
                animationsEnabled = false,
                startupAnimationEnabled = true,
            ),
        )
        assertTrue(
            CandyAnimationRules.startupAnimationEnabled(
                animationsEnabled = true,
                startupAnimationEnabled = true,
            ),
        )
    }
}
