package dev.sk2andy.materialbrowser.data

import android.content.Context

/** Where each profile keeps its hidden distracting items; called off the main thread. */
internal interface HiddenDistractionsStorage {
    fun load(profileId: String): HiddenDistractions

    fun save(profileId: String, state: HiddenDistractions)

    fun delete(profileId: String)
}

/** One preference per profile in the app's private storage; private tabs never reach it. */
internal class HiddenDistractionsStore(context: Context) : HiddenDistractionsStorage {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun load(profileId: String): HiddenDistractions =
        HiddenDistractionsCodec.decode(preferences.getString(key(profileId), null))

    override fun save(profileId: String, state: HiddenDistractions) {
        preferences.edit().apply {
            if (state.sites.isEmpty()) remove(key(profileId)) else putString(key(profileId), HiddenDistractionsCodec.encode(state))
        }.apply()
    }

    override fun delete(profileId: String) {
        preferences.edit().remove(key(profileId)).apply()
    }

    private fun key(profileId: String) = "profile_$profileId"

    private companion object {
        const val PREFERENCES = "hidden_distractions"
    }
}
