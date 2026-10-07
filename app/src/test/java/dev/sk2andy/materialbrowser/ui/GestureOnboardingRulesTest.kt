package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureOnboardingRulesTest {
    @Test
    fun `tab switch accepts either horizontal direction`() {
        assertTrue(
            GestureOnboardingRules.isCompleted(
                GestureOnboardingStep.SwitchTabs,
                dragX = -72f,
                dragY = 10f,
                threshold = 72f,
            ),
        )
        assertTrue(
            GestureOnboardingRules.isCompleted(
                GestureOnboardingStep.SwitchTabs,
                dragX = 72f,
                dragY = -10f,
                threshold = 72f,
            ),
        )
    }

    @Test
    fun `overview and close require dominant upward drags`() {
        listOf(
            GestureOnboardingStep.OpenTabOverview,
            GestureOnboardingStep.CloseTab,
        ).forEach { step ->
            assertTrue(
                GestureOnboardingRules.isCompleted(
                    step,
                    dragX = 8f,
                    dragY = -72f,
                    threshold = 72f,
                ),
            )
            assertFalse(
                GestureOnboardingRules.isCompleted(
                    step,
                    dragX = 80f,
                    dragY = -72f,
                    threshold = 72f,
                ),
            )
        }
    }

    @Test
    fun `short drag never completes a step`() {
        GestureOnboardingStep.entries.forEach { step ->
            assertFalse(
                GestureOnboardingRules.isCompleted(
                    step,
                    dragX = 20f,
                    dragY = -20f,
                    threshold = 72f,
                ),
            )
        }
    }

    @Test
    fun `the practice card takes the room left between its smallest and largest size`() {
        assertEquals(300, GestureOnboardingRules.practiceHeight(available = 300, min = 240, max = 390))
        assertEquals(390, GestureOnboardingRules.practiceHeight(available = 900, min = 240, max = 390))
        // Too little room: the card keeps its smallest size and the lesson scrolls.
        assertEquals(240, GestureOnboardingRules.practiceHeight(available = 40, min = 240, max = 390))
    }

    @Test
    fun `only a short screen on its side puts the card beside the copy`() {
        // A phone on its side, 360 dp tall.
        assertTrue(GestureOnboardingRules.placesPracticeBeside(width = 780f, height = 360f, stackedMinHeight = 560f))
        // A phone upright, and a tablet on its side, stack the lesson.
        assertFalse(GestureOnboardingRules.placesPracticeBeside(width = 360f, height = 780f, stackedMinHeight = 560f))
        assertFalse(GestureOnboardingRules.placesPracticeBeside(width = 1280f, height = 800f, stackedMinHeight = 560f))
    }
}
