package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.ProtectionDay
import dev.sk2andy.materialbrowser.data.ProtectionReportPersistence
import dev.sk2andy.materialbrowser.data.ProtectionReportRules
import dev.sk2andy.materialbrowser.data.ProtectionWeek

/**
 * The weekly protection report (ROADMAP Q5b, П7): trackers the blocker stopped, by site and day,
 * counted on the device for the new tab card and its report. [BrowserController] reports the
 * blocked trackers of regular tabs; private tabs are never recorded.
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
        days = ProtectionReportRules.prune(store.loadDays(), host.today())
        isCardVisible = store.loadCardVisible()
        refresh()
    }

    /** Counts [blocked] trackers on the page at [pageUrl]; pages that are not on the web are skipped. */
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

    /** Recomputes the week, for example when the new tab opens on a later day. */
    fun refresh() {
        week = ProtectionReportRules.week(days, host.today())
    }

    fun clear() {
        days = emptyList()
        host.removeCallbacks(saveDays)
        dirty = false
        store.saveDays(days)
        refresh()
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
