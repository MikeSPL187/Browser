package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserSessionIdleUnloadTest {
    private var now = 0L
    private val scheduled = mutableListOf<Pair<Runnable, Long>>()
    private val removed = mutableListOf<Runnable>()
    private var checks = 0
    private val unload = BrowserSessionIdleUnload(
        postDelayed = { tick, delayMillis -> scheduled += tick to delayMillis },
        removeCallbacks = { tick -> removed += tick },
        check = { checks++ },
        clock = { now },
    )

    @Test
    fun `a background tab not opened for two hours is unloaded, the selected one stays`() {
        unload.markAccess("background")
        unload.markAccess("selected")
        now += BrowserSessionResidencyRules.IDLE_UNLOAD_MILLIS

        val evictions = unload.evictionOrder(
            residentTabIds = setOf("background", "selected"),
            protectedTabIds = setOf("selected"),
            limit = BrowserSessionResidencyRules.DEFAULT_LIMIT,
        )

        assertEquals(listOf("background"), evictions)
    }

    @Test
    fun `opening a tab again restarts its idle time`() {
        unload.markAccess("tab")
        now += BrowserSessionResidencyRules.IDLE_UNLOAD_MILLIS - 1
        unload.markAccess("tab")
        now += BrowserSessionResidencyRules.IDLE_UNLOAD_MILLIS - 1

        val evictions = unload.evictionOrder(setOf("tab"), emptySet(), BrowserSessionResidencyRules.DEFAULT_LIMIT)

        assertEquals(emptyList<String>(), evictions)
    }

    @Test
    fun `over-limit tabs come first and an idle one is listed once`() {
        unload.markAccess("old")
        unload.markAccess("newer")
        unload.markAccess("selected")
        now += BrowserSessionResidencyRules.IDLE_UNLOAD_MILLIS

        val evictions = unload.evictionOrder(
            residentTabIds = setOf("old", "newer", "selected"),
            protectedTabIds = setOf("selected"),
            limit = 2,
        )

        assertEquals(listOf("old", "newer"), evictions)
    }

    @Test
    fun `a forgotten or closed session is not counted again`() {
        unload.markAccess("forgotten")
        unload.markAccess("closed")
        unload.forget("forgotten")
        now += BrowserSessionResidencyRules.IDLE_UNLOAD_MILLIS

        assertEquals(emptyList<String>(), unload.evictionOrder(setOf("forgotten"), emptySet(), 10))
        assertEquals(emptyList<String>(), unload.evictionOrder(setOf("closed"), emptySet(), 10))
    }

    @Test
    fun `the open app checks every five minutes and the background stops the timer after one check`() {
        unload.onForeground()
        unload.onForeground()
        assertEquals(1, scheduled.size)
        assertEquals(5 * 60 * 1000L, scheduled.single().second)

        scheduled.removeAt(0).first.run()
        assertEquals(1, checks)
        assertEquals(1, scheduled.size)

        val tick = scheduled.single().first
        unload.onBackground()
        assertEquals(2, checks)
        assertEquals(listOf(tick), removed)

        tick.run()
        assertEquals(2, checks)
        assertEquals(1, scheduled.size)
    }

    @Test
    fun `close stops the timer and drops every access record`() {
        unload.markAccess("tab")
        unload.onForeground()
        unload.close()
        now += BrowserSessionResidencyRules.IDLE_UNLOAD_MILLIS

        assertEquals(1, removed.size)
        assertTrue(unload.evictionOrder(setOf("tab"), emptySet(), 10).isEmpty())
    }
}
