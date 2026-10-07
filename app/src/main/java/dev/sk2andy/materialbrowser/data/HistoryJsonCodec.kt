package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_ID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the saved history row by row: one damaged row is skipped instead of hiding every visit,
 * so the next write does not replace the remaining history with an empty list.
 */
internal object HistoryJsonCodec {
    fun decode(raw: String?): List<HistoryEntry> {
        if (raw == null) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                runCatching { decodeEntry(item) }.getOrNull()?.let(::add)
            }
        }
    }

    private fun decodeEntry(item: JSONObject): HistoryEntry = HistoryEntry(
        url = item.getString("url"),
        title = item.optString("title"),
        lastVisitedAt = item.optLong("lastVisitedAt"),
        profileId = item.optString("profileId", DEFAULT_PROFILE_ID)
            .takeIf(String::isNotBlank)
            ?: DEFAULT_PROFILE_ID,
        visitId = item.optString("visitId"),
    )
}
