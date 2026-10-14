package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5PullRefreshRulesTest {
    private val threshold = 200f

    @Test
    fun `the page follows the finger at 0_55 up to the threshold`() {
        assertEquals(55f, V5PullRefreshRules.pageOffset(100f, threshold), 0.001f)
        assertEquals(threshold, V5PullRefreshRules.pageOffset(threshold / 0.55f, threshold), 0.01f)
    }

    @Test
    fun `past the threshold the page gets heavier and never stops`() {
        val atThreshold = threshold / 0.55f
        val further = V5PullRefreshRules.pageOffset(atThreshold + 100f, threshold)
        assertEquals(threshold + 100f * 0.55f * 0.55f, further, 0.01f)
        assertTrue(V5PullRefreshRules.pageOffset(atThreshold + 200f, threshold) > further)
    }

    @Test
    fun `no pull for an upward, broken or zero input`() {
        assertEquals(0f, V5PullRefreshRules.pageOffset(-20f, threshold), 0f)
        assertEquals(0f, V5PullRefreshRules.pageOffset(Float.NaN, threshold), 0f)
        assertEquals(0f, V5PullRefreshRules.pageOffset(100f, 0f), 0f)
    }

    @Test
    fun `the reload is armed at the threshold and the shape forms by then`() {
        assertFalse(V5PullRefreshRules.armed(199f, threshold))
        assertTrue(V5PullRefreshRules.armed(200f, threshold))
        assertEquals(0.5f, V5PullRefreshRules.progress(100f, threshold), 0.001f)
        assertEquals(1f, V5PullRefreshRules.progress(400f, threshold), 0.001f)
        assertEquals(0f, V5PullRefreshRules.progress(100f, 0f), 0.001f)
    }

    @Test
    fun `the page waits low enough for the indicator under a tall status bar`() {
        val roomy = V5PullRefreshRules.holdOffset(200f, topInsetPx = 60f, indicatorPx = 100f, marginPx = 10f)
        val tall = V5PullRefreshRules.holdOffset(200f, topInsetPx = 140f, indicatorPx = 100f, marginPx = 10f)
        assertEquals(200f, roomy, 0.001f)
        assertEquals(260f, tall, 0.001f)
    }

    @Test
    fun `the indicator sits in the middle of the gap`() {
        // Gap from 60 (status bar) to 200 (page): its middle is 130, the indicator's top 130 - 50.
        assertEquals(80f, V5PullRefreshRules.indicatorTop(200f, topInsetPx = 60f, indicatorPx = 100f), 0.001f)
    }
}
