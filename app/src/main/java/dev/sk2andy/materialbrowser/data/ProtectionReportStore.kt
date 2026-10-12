package dev.sk2andy.materialbrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** The saved days of the weekly protection report and whether its new tab card is shown. */
interface ProtectionReportPersistence {
    fun loadDays(): List<ProtectionDay>
    fun saveDays(days: List<ProtectionDay>)
    fun loadCardVisible(): Boolean
    fun saveCardVisible(visible: Boolean)
}

/**
 * The report in its own preferences file. Counts per site and tracker and day only, never page
 * addresses, and only for the last [ProtectionReportRules.KEPT_DAYS] days.
 */
internal class ProtectionReportStore(context: Context) : ProtectionReportPersistence {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun loadDays(): List<ProtectionDay> =
        ProtectionReportCodec.decode(preferences.getString(KEY_DAYS, null).orEmpty())

    override fun saveDays(days: List<ProtectionDay>) {
        preferences.edit().putString(KEY_DAYS, ProtectionReportCodec.encode(days)).apply()
    }

    override fun loadCardVisible(): Boolean = preferences.getBoolean(KEY_CARD_VISIBLE, true)

    override fun saveCardVisible(visible: Boolean) {
        preferences.edit().putBoolean(KEY_CARD_VISIBLE, visible).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "vola_protection_report"
        const val KEY_DAYS = "days_v1"
        const val KEY_CARD_VISIBLE = "new_tab_card_visible"
    }
}

/**
 * `{"<epoch day>": {"<site>": <blocked>}, "totals": {"<epoch day>": <blocked>},
 * "trackers": {"<epoch day>": {"<tracker>": <blocked>}}, "siteTrackers": {"<epoch day>":
 * {"<site>": ["<tracker>"]}}}`; anything malformed is skipped. A day without a saved total
 * (written before totals existed) counts the sum of its sites, and older builds skip the keys that
 * are not days.
 */
internal object ProtectionReportCodec {
    private const val KEY_TOTALS = "totals"
    private const val KEY_TRACKERS = "trackers"
    private const val KEY_SITE_TRACKERS = "siteTrackers"

    fun encode(days: List<ProtectionDay>): String =
        JSONObject().apply {
            val totals = JSONObject()
            val trackers = JSONObject()
            val siteTrackers = JSONObject()
            days.forEach { day ->
                val key = day.epochDay.toString()
                put(key, JSONObject(day.blockedBySite))
                totals.put(key, day.total)
                if (day.blockedByTracker.isNotEmpty()) trackers.put(key, JSONObject(day.blockedByTracker))
                if (day.trackersBySite.isNotEmpty()) {
                    siteTrackers.put(key, JSONObject(day.trackersBySite.mapValues { JSONArray(it.value.toList()) }))
                }
            }
            put(KEY_TOTALS, totals)
            put(KEY_TRACKERS, trackers)
            put(KEY_SITE_TRACKERS, siteTrackers)
        }.toString()

    fun decode(json: String): List<ProtectionDay> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val totals = root.optJSONObject(KEY_TOTALS)
        val trackers = root.optJSONObject(KEY_TRACKERS)
        val siteTrackers = root.optJSONObject(KEY_SITE_TRACKERS)
        return root.keys().asSequence().mapNotNull { key ->
            val epochDay = key.toLongOrNull() ?: return@mapNotNull null
            val sites = root.optJSONObject(key) ?: return@mapNotNull null
            val counts = sites.keys().asSequence()
                .mapNotNull { site -> sites.optInt(site, 0).takeIf { it > 0 }?.let { site to it } }
                .toMap()
            if (counts.isEmpty()) return@mapNotNull null
            val day = ProtectionDay(
                epochDay = epochDay,
                blockedBySite = counts,
                blockedByTracker = trackers?.optJSONObject(key)?.let(::positiveCounts).orEmpty(),
                trackersBySite = siteTrackers?.optJSONObject(key)?.let { saved ->
                    saved.keys().asSequence().filter(counts::containsKey).mapNotNull { site ->
                        saved.optJSONArray(site)?.let(::names)?.takeIf(Set<String>::isNotEmpty)?.let { site to it }
                    }.toMap()
                }.orEmpty(),
            )
            val savedTotal = totals?.optInt(key, 0) ?: 0
            if (savedTotal > day.total) day.copy(total = savedTotal) else day
        }.sortedBy(ProtectionDay::epochDay).toList()
    }

    private fun names(array: JSONArray): Set<String> =
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.toSet()

    private fun positiveCounts(counts: JSONObject): Map<String, Int> = counts.keys().asSequence()
        .mapNotNull { name -> counts.optInt(name, 0).takeIf { it > 0 }?.let { name to it } }
        .toMap()
}
