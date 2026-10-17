package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserSessionResidencyRulesTest {
    @Test
    fun `oldest unprotected residents are evicted first`() {
        val evictions = BrowserSessionResidencyRules.evictionOrder(
            residentTabIds = setOf("first", "second", "third", "selected"),
            accessOrder = mapOf(
                "first" to 1L,
                "second" to 2L,
                "third" to 3L,
                "selected" to 4L,
            ),
            protectedTabIds = setOf("selected"),
            limit = 2,
        )

        assertEquals(listOf("first", "second"), evictions)
    }

    @Test
    fun `protected residents may temporarily exceed limit`() {
        val evictions = BrowserSessionResidencyRules.evictionOrder(
            residentTabIds = setOf("selected", "audio", "preview"),
            accessOrder = mapOf("selected" to 1L, "audio" to 2L, "preview" to 3L),
            protectedTabIds = setOf("selected", "audio", "preview"),
            limit = 1,
        )

        assertEquals(emptyList<String>(), evictions)
    }

    @Test
    fun `missing access order is oldest with deterministic tie break`() {
        val evictions = BrowserSessionResidencyRules.evictionOrder(
            residentTabIds = setOf("z", "a", "recent"),
            accessOrder = mapOf("recent" to 1L),
            protectedTabIds = emptySet(),
            limit = 1,
        )

        assertEquals(listOf("a", "z"), evictions)
    }

    @Test
    fun `limit is normalized to supported bounds`() {
        assertEquals(BrowserSessionResidencyRules.MIN_LIMIT, BrowserSessionResidencyRules.normalizedLimit(-1))
        assertEquals(
            BrowserSessionResidencyRules.MAX_LIMIT,
            BrowserSessionResidencyRules.normalizedLimit(Int.MAX_VALUE),
        )
    }

    @Test
    fun `memory warnings ask for fewer live sessions without changing the setting`() {
        assertEquals(3, BrowserSessionResidencyRules.pressureLimit(level = 10, limit = 10))
        assertEquals(1, BrowserSessionResidencyRules.pressureLimit(level = 15, limit = 10))
        assertEquals(3, BrowserSessionResidencyRules.pressureLimit(level = 40, limit = 10))
        assertEquals(1, BrowserSessionResidencyRules.pressureLimit(level = 60, limit = 10))
        assertEquals(1, BrowserSessionResidencyRules.pressureLimit(level = 80, limit = 10))
    }

    @Test
    fun `hiding the interface or a mild warning trims nothing`() {
        assertEquals(null, BrowserSessionResidencyRules.pressureLimit(level = 5, limit = 10))
        assertEquals(null, BrowserSessionResidencyRules.pressureLimit(level = 20, limit = 10))
    }

    @Test
    fun `a setting already at or below the pressure limit is left alone`() {
        assertEquals(null, BrowserSessionResidencyRules.pressureLimit(level = 10, limit = 2))
        assertEquals(null, BrowserSessionResidencyRules.pressureLimit(level = 80, limit = 1))
    }

    @Test
    fun `a critical warning keeps only protected sessions`() {
        val limit = requireNotNull(BrowserSessionResidencyRules.pressureLimit(level = 15, limit = 10))
        val evictions = BrowserSessionResidencyRules.evictionOrder(
            residentTabIds = setOf("selected", "audio", "old", "older"),
            accessOrder = mapOf("older" to 1L, "old" to 2L, "audio" to 3L, "selected" to 4L),
            protectedTabIds = setOf("selected", "audio"),
            limit = limit,
        )

        assertEquals(listOf("older", "old"), evictions)
    }
}
