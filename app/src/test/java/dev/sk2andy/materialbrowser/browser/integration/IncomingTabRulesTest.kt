package dev.sk2andy.materialbrowser.browser.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingTabRulesTest {
    @Test
    fun `a link from another app never opens in the private session`() {
        assertFalse(IncomingTabRules.OPENS_PRIVATE)
    }

    @Test
    fun `back returns to a regular tab, never across into a private one`() {
        assertEquals("tab", IncomingTabRules.openerTabId("tab", selectedIsPrivate = false))
        assertNull(IncomingTabRules.openerTabId("private", selectedIsPrivate = true))
    }
}
