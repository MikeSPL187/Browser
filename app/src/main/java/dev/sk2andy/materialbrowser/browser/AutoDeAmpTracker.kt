package dev.sk2andy.materialbrowser.browser

import android.os.SystemClock

/**
 * Per-tab memory of auto de-AMP: which navigation request is the newest, and which publisher
 * page last replaced an AMP page, so a publisher that redirects back to AMP cannot loop.
 */
internal class AutoDeAmpTracker(private val now: () -> Long = SystemClock::elapsedRealtime) {
    private val requestGenerations = mutableMapOf<String, Long>()
    private val replacements = mutableMapOf<String, Replacement>()

    /** Starts a navigation request in [tabId] and returns its generation. */
    fun nextRequest(tabId: String): Long {
        val generation = requestGenerations.getOrDefault(tabId, 0L) + 1L
        requestGenerations[tabId] = generation
        return generation
    }

    fun isLatestRequest(tabId: String, generation: Long): Boolean =
        requestGenerations[tabId] == generation

    /** True when [publisherUrl] replaced an AMP page in [tabId] moments ago: do not again. */
    fun isLooping(tabId: String, publisherUrl: String): Boolean {
        val replacement = replacements[tabId] ?: return false
        return replacement.publisherUrl == publisherUrl && replacement.expiresAt >= now()
    }

    fun remember(tabId: String, publisherUrl: String) {
        replacements[tabId] = Replacement(publisherUrl, expiresAt = now() + LOOP_GUARD_MILLIS)
    }

    fun forget(tabId: String) {
        requestGenerations.remove(tabId)
        replacements.remove(tabId)
    }

    private data class Replacement(val publisherUrl: String, val expiresAt: Long)

    companion object {
        const val LOOP_GUARD_MILLIS = 15_000L
    }
}
