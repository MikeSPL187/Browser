package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import org.junit.Assert.assertTrue
import org.junit.Test

class VolaTabMorphMotionTest {
    private val start = AnimationVector1D(0f)
    private val end = AnimationVector1D(1f)
    private val still = AnimationVector1D(0f)
    // Animatable(0f) settles at the default displacement threshold.
    private val spec = spring(
        dampingRatio = VolaMotion.tabMorph().dampingRatio,
        stiffness = VolaMotion.tabMorph().stiffness,
        visibilityThreshold = Spring.DefaultDisplacementThreshold,
    ).vectorize(Float.VectorConverter)

    @Test
    fun `the morph settles within its budget`() {
        val millis = spec.getDurationNanos(start, end, still) / 1_000_000L

        assertTrue("settles in $millis ms", millis <= VolaMotion.TAB_MORPH_SETTLE_MILLIS)
    }

    @Test
    fun `the page never overshoots its card`() {
        val durationNanos = spec.getDurationNanos(start, end, still)
        (0..100).forEach { step ->
            val value = spec.getValueFromNanos(durationNanos * step / 100, start, end, still).value
            assertTrue("$value at step $step", value in 0f..1.0001f)
        }
    }
}
