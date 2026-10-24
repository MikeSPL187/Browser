package dev.sk2andy.materialbrowser.browser

internal object BrowserSessionResidencyRules {
    const val DEFAULT_LIMIT = 10
    const val MIN_LIMIT = 1
    const val MAX_LIMIT = 20

    /** Live background sessions kept while the system asks for memory but is not yet critical. */
    const val LOW_MEMORY_LIMIT = 3

    /**
     * A live background session nobody has opened for this long lets go of its page (it comes back
     * from the saved state when the tab is selected), as Edge's sleeping tabs do. Shorter only
     * after measurements.
     */
    const val IDLE_UNLOAD_MILLIS = 2 * 60 * 60 * 1000L

    /** How often the open app looks for idle sessions; in the background nothing polls. */
    const val IDLE_CHECK_INTERVAL_MILLIS = 5 * 60 * 1000L

    fun normalizedLimit(limit: Int): Int = limit.coerceIn(MIN_LIMIT, MAX_LIMIT)

    /**
     * The limit to trim to once at an Android memory warning ([android.content.ComponentCallbacks2]
     * level), or null when the warning asks for nothing. The person's [limit] stays as it is: the
     * next trim after the warning uses it again. A critical warning keeps only the protected
     * sessions (the selected tab, media, a pending permission), which the eviction never touches.
     */
    fun pressureLimit(level: Int, limit: Int): Int? {
        val reduced = when {
            level >= TRIM_MEMORY_MODERATE -> MIN_LIMIT
            level >= TRIM_MEMORY_BACKGROUND -> LOW_MEMORY_LIMIT
            level >= TRIM_MEMORY_UI_HIDDEN -> return null
            level >= TRIM_MEMORY_RUNNING_CRITICAL -> MIN_LIMIT
            level >= TRIM_MEMORY_RUNNING_LOW -> LOW_MEMORY_LIMIT
            else -> return null
        }
        return reduced.takeIf { it < normalizedLimit(limit) }
    }

    // The levels of android.content.ComponentCallbacks2, kept here so the rules stay plain JVM code.
    private const val TRIM_MEMORY_RUNNING_LOW = 10
    private const val TRIM_MEMORY_RUNNING_CRITICAL = 15
    private const val TRIM_MEMORY_UI_HIDDEN = 20
    private const val TRIM_MEMORY_BACKGROUND = 40
    private const val TRIM_MEMORY_MODERATE = 60

    fun evictionOrder(
        residentTabIds: Set<String>,
        accessOrder: Map<String, Long>,
        protectedTabIds: Set<String>,
        limit: Int,
    ): List<String> {
        val excessCount = (residentTabIds.size - normalizedLimit(limit)).coerceAtLeast(0)
        if (excessCount == 0) return emptyList()
        return residentTabIds.asSequence()
            .filterNot(protectedTabIds::contains)
            .sortedWith(
                compareBy<String> { tabId -> accessOrder[tabId] ?: Long.MIN_VALUE }
                    .thenBy { tabId -> tabId },
            )
            .take(excessCount)
            .toList()
    }

    /**
     * The live sessions idle for at least [idleMillis] at [nowMillis], oldest first, except the
     * protected ones (the same set [evictionOrder] spares). A session without a known access time
     * or with one after [nowMillis] (a clock that went back) is not idle.
     */
    fun idleUnloadOrder(
        residentTabIds: Set<String>,
        lastAccessMillis: Map<String, Long>,
        protectedTabIds: Set<String>,
        nowMillis: Long,
        idleMillis: Long = IDLE_UNLOAD_MILLIS,
    ): List<String> = residentTabIds.asSequence()
        .filterNot(protectedTabIds::contains)
        .filter { tabId ->
            val accessedAt = lastAccessMillis[tabId] ?: return@filter false
            accessedAt <= nowMillis && nowMillis - accessedAt >= idleMillis
        }
        .sortedWith(
            compareBy<String> { tabId -> lastAccessMillis.getValue(tabId) }
                .thenBy { tabId -> tabId },
        )
        .toList()
}
