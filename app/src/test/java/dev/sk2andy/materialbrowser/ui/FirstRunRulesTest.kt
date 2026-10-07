package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunRulesTest {
    @Test
    fun `a new install starts with the welcome and an update with the lesson`() {
        assertEquals(FirstRunStage.Welcome, FirstRunRules.firstStage(showIntro = true))
        assertEquals(FirstRunStage.Gestures, FirstRunRules.firstStage(showIntro = false))
    }

    @Test
    fun `setup leads to the lesson only when it is wanted`() {
        assertEquals(FirstRunStage.Gestures, FirstRunRules.afterSetup(showGestures = true))
        assertNull(FirstRunRules.afterSetup(showGestures = false))
    }

    @Test
    fun `the first run counts the lesson only while it is switched on`() {
        assertEquals(3, FirstRunRules.stepCount(showGestures = true))
        assertEquals(2, FirstRunRules.stepCount(showGestures = false))
    }

    @Test
    fun `back from setup returns to the welcome and from the lesson to setup`() {
        assertEquals(FirstRunStage.Welcome, FirstRunRules.back(FirstRunStage.Setup, showIntro = true))
        assertEquals(FirstRunStage.Setup, FirstRunRules.back(FirstRunStage.Gestures, showIntro = true))
    }

    @Test
    fun `back leaves from the welcome and skips the lesson on its own`() {
        assertNull(FirstRunRules.back(FirstRunStage.Welcome, showIntro = true))
        assertNull(FirstRunRules.back(FirstRunStage.Gestures, showIntro = false))
    }

    @Test
    fun `only setup catches back in the overlay`() {
        assertTrue(FirstRunRules.handlesBack(FirstRunStage.Setup))
        // The welcome lets the system close Vola; the lesson handles its own «Back».
        assertFalse(FirstRunRules.handlesBack(FirstRunStage.Welcome))
        assertFalse(FirstRunRules.handlesBack(FirstRunStage.Gestures))
    }
}
