package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_ID

/**
 * Where each profile's favorites live (v5 V11: every profile keeps its own, as in Safari). The list
 * saved before profiles had their own stays under the old key and belongs to the profile that owned
 * it then — the default profile, or the first one when the default was deleted — so the update moves
 * no data. Every other profile gets a key of its own.
 */
internal object FavoriteProfileKeys {
    const val LEGACY_KEY = "favorites"
    private const val PROFILE_PREFIX = "favorites:"

    fun key(profileId: String, legacyOwner: String): String =
        if (profileId == legacyOwner) LEGACY_KEY else PROFILE_PREFIX + profileId

    fun legacyOwner(profileIds: List<String>): String =
        if (DEFAULT_PROFILE_ID in profileIds) DEFAULT_PROFILE_ID else profileIds.firstOrNull() ?: DEFAULT_PROFILE_ID

    /** Whether a stored preference key holds some profile's favorites. */
    fun isFavoritesKey(key: String): Boolean = key == LEGACY_KEY || key.startsWith(PROFILE_PREFIX)
}
