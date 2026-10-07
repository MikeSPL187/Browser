package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.ProtectionDay
import dev.sk2andy.materialbrowser.data.ProtectionReportPersistence
import dev.sk2andy.materialbrowser.data.ProtectionReportRules
import dev.sk2andy.materialbrowser.data.ProtectionWeek

/**
 * The weekly protection report (ROADMAP Q5b, П7): requests the blocker stopped, by site and day,
 * counted on the device for the new tab card and its report. The blocker does not tell ads from
 * trackers or from the user's own block rules, so the report names them together. [BrowserController]
 * reports the blocks of regular tabs; private tabs are never recorded.
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

    private var days: List<ProtectionDay> = emptyList()
    private var dirty = false
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

    /** Counts [blocked] requests on the page at [pageUrl]; pages that are not on the web are skipped. */
    fun record(pageUrl: String, blocked: Int) {
        val site = ProtectionReportRules.site(pageUrl) ?: return
        if (blocked <= 0) return
        days = ProtectionReportRules.record(days, host.today(), site, blocked)
        refresh()
        if (!dirty) {
            dirty = true
            host.postDelayed(saveDays, SAVE_DELAY_MILLIS)
        }
    }

    /**
     * Recomputes the week, for example when the new tab opens on a later day. Days that left the
     * week are dropped from the saved report too, so it never keeps more than seven days.
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

    fun clear() {
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
    }
}
