package dev.sk2andy.materialbrowser.ui.theme.v5

import android.view.HapticFeedbackConstants
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Shapes, type, motion and haptics against system-v5.md. */
class V5TokensTest {
    @Test
    fun `an unsmoothed corner is a plain circular arc`() {
        val corner = smoothCorner(radius = 16f, smoothing = 0f, budget = 100f)
        assertEquals(16f, corner.extent, EPSILON)
        assertEquals(90f, corner.arcDegrees, EPSILON)
        assertEquals(0f, corner.a, EPSILON)
        assertEquals(0f, corner.c, EPSILON)
        assertEquals(0f, corner.d, EPSILON)
    }

    @Test
    fun `smoothing 0_6 spreads the corner over 1_6 radii`() {
        val corner = smoothCorner(radius = 16f, smoothing = V5_CORNER_SMOOTHING, budget = 100f)
        assertEquals(25.6f, corner.extent, EPSILON)
        assertEquals(36f, corner.arcDegrees, EPSILON)
        assertTrue(corner.a > 0f && corner.b > 0f && corner.c > 0f && corner.d > 0f)
        // The run-in handles, the run-in end and the arc fill the extent exactly.
        assertTrue(corner.a + corner.b + corner.c < corner.extent)
    }

    @Test
    fun `a short side lowers smoothing before the radius`() {
        val tight = smoothCorner(radius = 16f, smoothing = V5_CORNER_SMOOTHING, budget = 20f)
        assertEquals(16f, tight.radius, EPSILON)
        assertEquals(20f, tight.extent, EPSILON)
        val tiny = smoothCorner(radius = 16f, smoothing = V5_CORNER_SMOOTHING, budget = 10f)
        assertEquals(10f, tiny.radius, EPSILON)
        assertEquals(90f, tiny.arcDegrees, EPSILON)
    }

    @Test
    fun `no radius means no corner`() {
        assertEquals(0f, smoothCorner(radius = 0f, smoothing = V5_CORNER_SMOOTHING, budget = 50f).extent, EPSILON)
    }

    @Test
    fun `concentric radius subtracts the padding`() {
        assertEquals(16.dp, V5Shapes.concentric(V5Shapes.radiusXl, 16.dp))
        assertEquals(0.dp, V5Shapes.concentric(V5Shapes.radiusXs, 16.dp))
    }

    @Test
    fun `type scale matches the spec with 17 sp body text`() {
        val type = V5Type.default
        assertEquals(17.sp, type.body.fontSize)
        assertEquals(17.sp, type.headline.fontSize)
        assertEquals(34.sp, type.largeTitle.fontSize)
        assertEquals(41.sp, type.largeTitle.lineHeight)
        assertEquals(12.sp, type.caption.fontSize)
        assertEquals(600, type.headline.fontWeight?.weight)
        assertEquals(650, type.title.fontWeight?.weight)
        assertEquals("tnum", with(V5Type) { type.caption.tabular() }.fontFeatureSettings)
    }

    @Test
    fun `optical size stays inside Inter's axis`() {
        assertEquals(14f, interOpticalSize(12f), EPSILON)
        assertEquals(17f, interOpticalSize(17f), EPSILON)
        assertEquals(32f, interOpticalSize(34f), EPSILON)
    }

    @Test
    fun `springs carry the spec's damping and stiffness`() {
        assertEquals(V5Spring(0.8f, 380f), V5Motion.addressMorph)
        assertEquals(V5Spring(0.85f, 500f), V5Motion.tabSwipe)
        assertEquals(V5Spring(0.82f, 300f), V5Motion.overviewEnter)
        assertEquals(V5Spring(1f, 1600f), V5Motion.effects)
        assertEquals(150, V5Motion.REDUCED_FADE_MILLIS)
    }

    @Test
    fun `bar is 64 dp around a 48 dp capsule`() {
        assertEquals(V5Sizes.bar, V5Sizes.barCapsule + V5Spacing.space2 * 2)
        assertEquals(V5Sizes.touchTarget, V5Sizes.barCapsule)
    }

    @Test
    fun `haptics use API 34 constants and fall back below`() {
        assertEquals(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
            v5HapticConstant(V5Haptic.ThresholdReached, sdk = 34))
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, v5HapticConstant(V5Haptic.ThresholdReached, sdk = 33))
        assertNull(v5HapticConstant(V5Haptic.ThresholdLeft, sdk = 33))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, v5HapticConstant(V5Haptic.DragStart, sdk = 31))
        assertEquals(HapticFeedbackConstants.CONFIRM, v5HapticConstant(V5Haptic.Confirm, sdk = 31))
    }

    @Test
    fun `materials fall back to the opaque tint without blur`() {
        val tint = V5Materials.thick.tint(dark = true, blurAvailable = false, reduceTransparency = false)
        assertEquals(0.97f, tint.alpha, 0.01f)
        assertEquals(1f, V5Materials.thin.tint(dark = false, blurAvailable = true, reduceTransparency = true).alpha)
        assertEquals(0.72f, V5Materials.thin.tint(dark = false, blurAvailable = true, reduceTransparency = false).alpha,
            0.01f)
    }

    private companion object {
        const val EPSILON = 0.01f
    }
}
