package dev.sk2andy.materialbrowser.browser.protection

import dev.sk2andy.materialbrowser.blocking.SitePrivacyOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporarySitePrivacyOverridesTest {
    private val exception = SitePrivacyOverrides(fingerprintingProtectionDisabled = true)

    @Test
    fun closingAPrivateTabTellsTheEngineItsExceptionIsGone() {
        var changes = 0
        val overrides = TemporarySitePrivacyOverrides { changes++ }
        overrides["private"] = mapOf("example.com" to exception)
        assertEquals(1, changes)

        overrides.remove("private")

        assertEquals(2, changes)
        assertNull(overrides["private"])
        assertTrue(SiteProtectionRules.fingerprintingExceptionHosts(overrides.values).isEmpty())
    }

    @Test
    fun leavingPrivateModeClearsEveryTabAndTellsTheEngineOnce() {
        var changes = 0
        val overrides = TemporarySitePrivacyOverrides { changes++ }
        overrides["a"] = mapOf("a.example" to exception)
        overrides["b"] = mapOf("b.example" to exception)

        overrides.clear()

        assertEquals(3, changes)
        assertTrue(overrides.values.isEmpty())
    }

    @Test
    fun nothingToForgetChangesNothing() {
        var changes = 0
        val overrides = TemporarySitePrivacyOverrides { changes++ }

        overrides.remove("missing")
        overrides.clear()

        assertEquals(0, changes)
    }

    @Test
    fun anEmptyOverrideSetForgetsTheTab() {
        val overrides = TemporarySitePrivacyOverrides { }
        overrides["tab"] = mapOf("example.com" to exception)

        overrides["tab"] = emptyMap()

        assertNull(overrides["tab"])
    }

    @Test
    fun aPersistentChoiceReplacesTheTemporaryOneForThatHostOnly() {
        var changes = 0
        val overrides = TemporarySitePrivacyOverrides { changes++ }
        overrides["tab"] = mapOf("a.example" to exception, "b.example" to exception)

        overrides.clearHost("tab", "a.example")

        assertEquals(setOf("b.example"), overrides["tab"]?.keys)
        assertEquals(2, changes)
        overrides.clearHost("tab", "b.example")
        assertNull(overrides["tab"])
    }
}
