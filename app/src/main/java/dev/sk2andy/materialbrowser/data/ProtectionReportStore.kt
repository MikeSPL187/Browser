package dev.sk2andy.materialbrowser.data

import android.content.Context
import org.json.JSONObject

/** The saved days of the weekly protection report and whether its new tab card is shown. */
interface ProtectionReportPersistence {
    fun loadDays(): List<ProtectionDay>
    fun saveDays(days: List<ProtectionDay>)
    fun loadCardVisible(): Boolean
    fun saveCardVisible(visible: Boolean)
}

/**
 * The report in its own preferences file. Counts per site and day only, never page addresses,
 * and only for the last seven days.
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

/** `{"<epoch day>": {"<site>": <blocked>}}`; anything malformed is skipped. */
internal object ProtectionReportCodec {
    fun encode(days: List<ProtectionDay>): String =
        JSONObject().apply {
            days.forEach { day ->
                put(day.epochDay.toString(), JSONObject(day.blockedBySite))
            }
        }.toString()

    fun decode(json: String): List<ProtectionDay> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        return root.keys().asSequence().mapNotNull { key ->
            val epochDay = key.toLongOrNull() ?: return@mapNotNull null
            val sites = root.optJSONObject(key) ?: return@mapNotNull null
            val counts = sites.keys().asSequence()
                .mapNotNull { site -> sites.optInt(site, 0).takeIf { it > 0 }?.let { site to it } }
                .toMap()
            ProtectionDay(epochDay, counts).takeIf { counts.isNotEmpty() }
        }.sortedBy(ProtectionDay::epochDay).toList()
    }
}
