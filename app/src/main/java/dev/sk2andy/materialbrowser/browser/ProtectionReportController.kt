package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.blocking.CandyHostCanonicalizer
import dev.sk2andy.materialbrowser.blocking.CandyPublicSuffixRules
import dev.sk2andy.materialbrowser.data.ProtectionDay
import dev.sk2andy.materialbrowser.data.ProtectionReportPersistence
import dev.sk2andy.materialbrowser.data.ProtectionReportRules
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.data.ProtectionWeek
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * The protection report: requests the blocker stopped, by site, tracker and day, counted on the
 * device for the new tab card's week (ROADMAP Q5b, П7) and the privacy report's 7 or 30 days
 * (V18). The blocker does not tell ads from trackers or from the user's own block rules, so the
 * report names them together. [BrowserController] notes every blocked request of a tab
 * ([noteBlocked], any thread) and reports the tab's blocks in bursts ([record]); private tabs and
 * tabs of a locked workspace are never recorded.
 */
class ProtectionReportController internal constructor(
    private val store: ProtectionReportPersistence,
    private val host: Host,
) {
    internal interface Host {
        /** Days since 1970-01-01 in the device time zone. */
        fun today(): Long

        fun postDelayed(action: Runnable, delayMillis: Long)

        fun removeCallbacks(action: Runnable)
    }

    private var days by mutableStateOf(emptyList<ProtectionDay>())
    private var dirty = false

    /** Blocked requests by tracker, per tab, since the tab's last [record]. */
    private val pendingTrackers = ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>>()
    private val saveDays = Runnable(::flush)

    var week by mutableStateOf(ProtectionWeek.empty(0))
        private set
    var isCardVisible by mutableStateOf(true)
        private set

    fun restore() {
        days = store.loadDays()
        isCardVisible = store.loadCardVisible()
        refresh()
    }

    /** A request of the tab was blocked; [record] adds its tracker to the report. Any thread. */
    fun noteBlocked(tabId: String, requestUrl: String) {
        val requestHost = CandyHostCanonicalizer.webHost(requestUrl) ?: return
        val tracker = CandyPublicSuffixRules.registrableDomain(requestHost) ?: requestHost
        val trackers = pendingTrackers.computeIfAbsent(tabId) { ConcurrentHashMap() }
        if (trackers.size < MAX_PENDING_TRACKERS_PER_TAB || tracker in trackers) {
            trackers.computeIfAbsent(tracker) { AtomicInteger() }.incrementAndGet()
        }
    }

    /** Drops what [noteBlocked] gathered for a tab that closed or left its site. */
    fun forgetTab(tabId: String) {
        pendingTrackers.remove(tabId)
    }

    /**
     * Counts [blocked] requests of the tab on the page at [pageUrl], with the trackers noted since
     * the last call; pages that are not on the web are skipped. A private tab or a tab of a locked
     * workspace ([ProtectionReportRules.countsTab]) only drops the noted trackers.
     */
    fun record(tabId: String, pageUrl: String, blocked: Int, isPrivate: Boolean, workspaceProtected: Boolean) {
        val trackers = pendingTrackers.remove(tabId)?.mapValues { it.value.get() }.orEmpty()
        if (ProtectionReportRules.countsTab(isPrivate, workspaceProtected)) record(pageUrl, blocked, trackers)
    }

    /** Counts [blocked] requests on the page at [pageUrl]; pages that are not on the web are skipped. */
    fun record(pageUrl: String, blocked: Int, trackers: Map<String, Int> = emptyMap()) {
        val site = ProtectionReportRules.site(pageUrl) ?: return
        if (blocked <= 0) return
        days = ProtectionReportRules.record(days, host.today(), site, blocked, trackers)
        refresh()
        if (!dirty) {
            dirty = true
            host.postDelayed(saveDays, SAVE_DELAY_MILLIS)
        }
    }

    /** The privacy report over the last [windowDays] days (7 or 30), read as Compose state. */
    fun summary(windowDays: Int): ProtectionSummary = ProtectionReportRules.summary(days, host.today(), windowDays)

    /**
     * Recomputes the week, for example when the new tab opens on a later day. Days the report no
     * longer keeps are dropped from the saved report too, so it never keeps more than
     * [ProtectionReportRules.KEPT_DAYS] days.
     */
    fun refresh() {
        val today = host.today()
        val kept = ProtectionReportRules.prune(days, today)
        if (kept != days) {
            days = kept
            host.removeCallbacks(saveDays)
            dirty = false
            store.saveDays(days)
        }
        week = ProtectionReportRules.week(days, today)
    }

    /** Empties the report, for example with the browsing data. */
    fun clear() {
        pendingTrackers.clear()
        days = emptyList()
        host.removeCallbacks(saveDays)
        dirty = false
        store.saveDays(days)
        refresh()
    }

    /**
     * A workspace lock was turned on or off. Turning one on clears the report: the days so far do
     * not say which workspace a site was opened in, so the locked one's sites cannot be told
     * apart and removed alone.
     */
    fun onWorkspaceProtectionChanged(wasProtected: Boolean, isProtected: Boolean) {
        if (!wasProtected && isProtected) clear()
    }

    fun updateCardVisible(visible: Boolean) {
        if (isCardVisible == visible) return
        isCardVisible = visible
        store.saveCardVisible(visible)
    }

    /** Writes pending counts now; the browser calls it when it goes away. */
    fun flush() {
        host.removeCallbacks(saveDays)
        if (!dirty) return
        dirty = false
        store.saveDays(days)
    }

    private companion object {
        /** Blocks arrive in bursts while a page loads; one write per burst is enough. */
        const val SAVE_DELAY_MILLIS = 3_000L

        /** Trackers noted per tab between two records; a page with more keeps the first ones. */
        const val MAX_PENDING_TRACKERS_PER_TAB = ProtectionReportRules.MAX_TRACKERS_PER_SITE
    }
}
