package dev.sk2andy.materialbrowser.browser.gecko

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeckoEngineProtectionsTest {
    private val stripped = GeckoEngineProtections.strippedQueryParameters

    @Test
    fun `click identifiers of the big ad networks are stripped`() {
        listOf("fbclid", "gclid", "msclkid", "yclid", "ysclid", "twclid", "ttclid", "mc_eid")
            .forEach { parameter -> assertTrue(parameter, parameter in stripped) }
    }

    @Test
    fun `campaign tags that do not identify a person stay`() {
        assertFalse(stripped.any { parameter -> parameter.startsWith("utm_") })
    }

    @Test
    fun `the list holds plain unique lowercase names`() {
        assertEquals(stripped.size, stripped.toSet().size)
        stripped.forEach { parameter ->
            assertEquals(parameter, parameter.lowercase())
            assertTrue(parameter, parameter.matches(Regex("[a-z0-9_]+")))
        }
    }
}
