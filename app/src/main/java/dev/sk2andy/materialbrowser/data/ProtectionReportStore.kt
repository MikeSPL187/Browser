package dev.sk2andy.materialbrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The saved report and whether its new tab card is shown. [loadDays] gives sites and totals only, a
 * few kilobytes read at start for the card; the trackers ([loadTrackers]) are read later, off the main
 * thread, and [saveDays] (off the main thread too) writes both.
 */
interface ProtectionReportPersistence {
    fun loadDays(): List<ProtectionDay>
    fun loadTrackers(): Map<Long, ProtectionDayTrackers>
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

    override fun loadTrackers(): Map<Long, ProtectionDayTrackers> =
        ProtectionReportCodec.decodeTrackers(preferences.getString(KEY_TRACKERS, null).orEmpty())

    override fun saveDays(days: List<ProtectionDay>) {
        preferences.edit()
            .putString(KEY_DAYS, ProtectionReportCodec.encode(days))
            .putString(KEY_TRACKERS, ProtectionReportCodec.encodeTrackers(days))
            .apply()
    }

    override fun loadCardVisible(): Boolean = preferences.getBoolean(KEY_CARD_VISIBLE, true)

    override fun saveCardVisible(visible: Boolean) {
        preferences.edit().putBoolean(KEY_CARD_VISIBLE, visible).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "vola_protection_report"
        const val KEY_DAYS = "days_v1"
        const val KEY_TRACKERS = "trackers_v1"
        const val KEY_CARD_VISIBLE = "new_tab_card_visible"
    }
}

/**
 * Days: `{"<epoch day>": {"<site>": <blocked>}, "totals": {"<epoch day>": <blocked>}}`. Trackers,
 * kept apart: `{"<epoch day>": {"blocked": {"<tracker>": <blocked>}, "sites": {"<site>":
 * ["<tracker>"]}}}`. Anything malformed is skipped. A day without a saved total (written before
 * totals existed) counts the sum of its sites, and older builds skip the `totals` key like any other
 * that is not a day.
 */
internal object ProtectionReportCodec {
    private const val KEY_TOTALS = "totals"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_SITES = "sites"

    fun encode(days: List<ProtectionDay>): String =
        JSONObject().apply {
            val totals = JSONObject()
            days.forEach { day ->
                put(day.epochDay.toString(), JSONObject(day.blockedBySite))
                totals.put(day.epochDay.toString(), day.total)
            }
            put(KEY_TOTALS, totals)
        }.toString()

    fun decode(json: String): List<ProtectionDay> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val totals = root.optJSONObject(KEY_TOTALS)
        return root.keys().asSequence().mapNotNull { key ->
            val epochDay = key.toLongOrNull() ?: return@mapNotNull null
            val counts = root.optJSONObject(key)?.let(::positiveCounts).orEmpty()
            if (counts.isEmpty()) return@mapNotNull null
            val day = ProtectionDay(epochDay, counts)
            val savedTotal = totals?.optInt(key, 0) ?: 0
            if (savedTotal > day.total) day.copy(total = savedTotal) else day
        }.sortedBy(ProtectionDay::epochDay).toList()
    }

    fun encodeTrackers(days: List<ProtectionDay>): String =
        JSONObject().apply {
            days.filter { it.blockedByTracker.isNotEmpty() }.forEach { day ->
                put(
                    day.epochDay.toString(),
                    JSONObject()
                        .put(KEY_BLOCKED, JSONObject(day.blockedByTracker))
                        .put(KEY_SITES, JSONObject(day.trackersBySite.mapValues { JSONArray(it.value.toList()) })),
                )
            }
        }.toString()

    fun decodeTrackers(json: String): Map<Long, ProtectionDayTrackers> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyMap()
        return root.keys().asSequence().mapNotNull { key ->
            val epochDay = key.toLongOrNull() ?: return@mapNotNull null
            val day = root.optJSONObject(key) ?: return@mapNotNull null
            val blocked = day.optJSONObject(KEY_BLOCKED)?.let(::positiveCounts).orEmpty()
            if (blocked.isEmpty()) return@mapNotNull null
            val sites = day.optJSONObject(KEY_SITES)?.let { saved ->
                saved.keys().asSequence().mapNotNull { site ->
                    saved.optJSONArray(site)?.let(::names)?.filter(blocked::containsKey)?.toSet()
                        ?.takeIf(Set<String>::isNotEmpty)?.let { site to it }
                }.toMap()
            }.orEmpty()
            epochDay to ProtectionDayTrackers(blocked, sites)
        }.toMap()
    }

    private fun names(array: JSONArray): Set<String> =
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.toSet()

    private fun positiveCounts(counts: JSONObject): Map<String, Int> = counts.keys().asSequence()
        .mapNotNull { name -> counts.optInt(name, 0).takeIf { it > 0 }?.let { name to it } }
        .toMap()
}
