package dev.sk2andy.materialbrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Saved Essentials of every workspace; `null` from [load] means they were never saved. */
interface EssentialsPersistence {
    fun load(): Map<String, List<EssentialEntry>>?
    fun save(entries: Map<String, List<EssentialEntry>>)
}

/**
 * Essentials in their own preferences file, apart from `browser_session`, so the session store
 * does not grow with every feature. Like other preferences they move with a device transfer and
 * stay out of cloud backup (`data_extraction_rules.xml`).
 */
internal class EssentialsStore(context: Context) : EssentialsPersistence {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): Map<String, List<EssentialEntry>>? =
        preferences.getString(KEY_ENTRIES, null)?.let(EssentialsCodec::decode)

    override fun save(entries: Map<String, List<EssentialEntry>>) {
        preferences.edit().putString(KEY_ENTRIES, EssentialsCodec.encode(entries)).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "vola_essentials"
        const val KEY_ENTRIES = "entries_v1"
    }
}

/** `{"<workspace id>": [{"url": …, "title": …}, …]}`. Broken entries are skipped, not fatal. */
internal object EssentialsCodec {
    private const val URL = "url"
    private const val TITLE = "title"

    fun encode(entries: Map<String, List<EssentialEntry>>): String =
        JSONObject().apply {
            entries.forEach { (profileId, list) ->
                put(
                    profileId,
                    JSONArray().apply {
                        list.forEach { entry ->
                            put(JSONObject().put(URL, entry.url).put(TITLE, entry.title))
                        }
                    },
                )
            }
        }.toString()

    fun decode(json: String): Map<String, List<EssentialEntry>> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyMap()
        return root.keys().asSequence().associateWith { profileId ->
            val array = root.optJSONArray(profileId) ?: JSONArray()
            EssentialsRules.normalize(
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val url = item.optString(URL).takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                    EssentialEntry(url = url, title = item.optString(TITLE))
                },
            )
        }
    }
}
