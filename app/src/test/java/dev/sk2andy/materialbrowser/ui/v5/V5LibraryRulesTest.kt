package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5LibraryRulesTest {
    @Test
    fun `a row swiped left past the card close share is deleted`() {
        assertTrue(V5LibraryRules.deletes(offsetPx = -160f, widthPx = 400f))
        assertTrue(V5LibraryRules.deletes(offsetPx = -400f, widthPx = 400f))
    }

    @Test
    fun `a shorter swipe or an unmeasured row springs back`() {
        assertFalse(V5LibraryRules.deletes(offsetPx = -100f, widthPx = 400f))
        assertFalse(V5LibraryRules.deletes(offsetPx = 0f, widthPx = 400f))
        assertFalse(V5LibraryRules.deletes(offsetPx = -100f, widthPx = 0f))
    }

    @Test
    fun `a short fling to the left deletes as a card in the overview closes`() {
        // 1000 dp/s at density 2 is 2000 px/s.
        assertTrue(V5SwipeToDeleteRules.deletes(offsetPx = -40f, velocityPxPerSecond = -2400f, widthPx = 400f, density = 2f))
        assertTrue(V5SwipeToDeleteRules.deletes(offsetPx = -200f, velocityPxPerSecond = 0f, widthPx = 400f, density = 2f))
    }

    @Test
    fun `a slow swipe, a fling back or no swipe at all keeps the row`() {
        assertFalse(V5SwipeToDeleteRules.deletes(offsetPx = -40f, velocityPxPerSecond = -1000f, widthPx = 400f, density = 2f))
        assertFalse(V5SwipeToDeleteRules.deletes(offsetPx = -40f, velocityPxPerSecond = 2400f, widthPx = 400f, density = 2f))
        assertFalse(V5SwipeToDeleteRules.deletes(offsetPx = 0f, velocityPxPerSecond = -2400f, widthPx = 400f, density = 2f))
        assertFalse(V5SwipeToDeleteRules.deletes(offsetPx = -40f, velocityPxPerSecond = -2400f, widthPx = 0f, density = 2f))
    }
}
