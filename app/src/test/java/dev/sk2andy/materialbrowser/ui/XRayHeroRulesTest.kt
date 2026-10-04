package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XRayHeroRulesTest {
    @Test
    fun `the count is found wherever the language puts it`() {
        assertEquals(0..1, XRayHeroRules.countRange("14 requests", 14))
        assertEquals(10..13, XRayHeroRules.countRange("Blocked — 1284", 1284))
        assertNull(XRayHeroRules.countRange("requests", 14))
    }
}
