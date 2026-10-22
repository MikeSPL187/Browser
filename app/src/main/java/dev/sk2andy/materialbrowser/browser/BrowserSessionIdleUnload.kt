package dev.sk2andy.materialbrowser.browser

import android.os.SystemClock

/**
 * Remembers when each live engine session was last opened and picks the ones to let go of: the
 * oldest over the person's limit ([BrowserSessionResidencyRules.evictionOrder]) and the ones nobody
 * has opened for [BrowserSessionResidencyRules.IDLE_UNLOAD_MILLIS]
 * ([BrowserSessionResidencyRules.idleUnloadOrder]). While the app is in the foreground it asks
 * [check] for a trim every [BrowserSessionResidencyRules.IDLE_CHECK_INTERVAL_MILLIS]; leaving for
 * the background asks once and stops the timer, so nothing polls there. [clock] is monotonic, so a
 * change of the wall clock moves no tab closer to unloading.
 */
internal class BrowserSessionIdleUnload(
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
    private val check: () -> Unit,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val accessOrder = mutableMapOf<String, Long>()
    private val accessMillis = mutableMapOf<String, Long>()
    private var accessSequence = 0L
    private var running = false
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            check()
            if (running) postDelayed(this, BrowserSessionResidencyRules.IDLE_CHECK_INTERVAL_MILLIS)
        }
    }

    fun markAccess(tabId: String) {
        accessSequence++
        accessOrder[tabId] = accessSequence
        accessMillis[tabId] = clock()
    }

    fun forget(tabId: String) {
        accessOrder.remove(tabId)
        accessMillis.remove(tabId)
    }

    /** The sessions to evict now, over-limit ones first; access records of gone sessions are dropped. */
    fun evictionOrder(
        residentTabIds: Set<String>,
        protectedTabIds: Set<String>,
        limit: Int,
    ): List<String> {
        accessOrder.keys.retainAll(residentTabIds)
        accessMillis.keys.retainAll(residentTabIds)
        val overLimit = BrowserSessionResidencyRules.evictionOrder(
            residentTabIds = residentTabIds,
            accessOrder = accessOrder,
            protectedTabIds = protectedTabIds,
            limit = limit,
        )
        val idle = BrowserSessionResidencyRules.idleUnloadOrder(
            residentTabIds = residentTabIds,
            lastAccessMillis = accessMillis,
            protectedTabIds = protectedTabIds,
            nowMillis = clock(),
        )
        return (overLimit + idle).distinct()
    }

    fun onForeground() {
        if (running) return
        running = true
        postDelayed(tick, BrowserSessionResidencyRules.IDLE_CHECK_INTERVAL_MILLIS)
    }

    fun onBackground() {
        stopTimer()
        check()
    }

    fun close() {
        stopTimer()
        accessOrder.clear()
        accessMillis.clear()
    }

    private fun stopTimer() {
        if (!running) return
        running = false
        removeCallbacks(tick)
    }
}
