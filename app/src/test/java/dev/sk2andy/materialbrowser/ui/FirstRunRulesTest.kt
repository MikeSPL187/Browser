package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `back from setup returns to the welcome`() {
        assertEquals(FirstRunStage.Welcome, FirstRunRules.back(FirstRunStage.Setup))
        assertEquals(FirstRunStage.Welcome, FirstRunRules.back(FirstRunStage.Welcome))
        assertEquals(FirstRunStage.Gestures, FirstRunRules.back(FirstRunStage.Gestures))
    }
}
