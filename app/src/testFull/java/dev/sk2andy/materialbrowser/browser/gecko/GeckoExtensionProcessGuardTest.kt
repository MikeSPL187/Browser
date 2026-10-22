package dev.sk2andy.materialbrowser.browser.gecko

import org.junit.Assert.assertEquals
import org.junit.Test

class GeckoExtensionProcessGuardTest {
    @Test
    fun `a disabled extensions process is started again`() {
        var restarts = 0
        val guard = GeckoExtensionProcessGuard { restarts++ }

        guard.onDisabledProcessSpawning()

        assertEquals(1, restarts)
    }

    @Test
    fun `a process that keeps crashing is restarted only a few times`() {
        var restarts = 0
        val guard = GeckoExtensionProcessGuard { restarts++ }

        repeat(GeckoExtensionProcessGuard.MAX_RESTARTS + 2) { guard.onDisabledProcessSpawning() }

        assertEquals(GeckoExtensionProcessGuard.MAX_RESTARTS, restarts)
        assertEquals(GeckoExtensionProcessGuard.MAX_RESTARTS, guard.restarts)
    }
}
