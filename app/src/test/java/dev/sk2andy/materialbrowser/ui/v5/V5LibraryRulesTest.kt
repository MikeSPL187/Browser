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
}
