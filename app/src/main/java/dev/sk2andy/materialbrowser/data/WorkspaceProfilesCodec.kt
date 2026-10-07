package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ProfileLockTrigger
import dev.sk2andy.materialbrowser.browser.ProfileProtection
import dev.sk2andy.materialbrowser.browser.ProfileProtectionRules
import dev.sk2andy.materialbrowser.browser.ProfileWallpaper
import dev.sk2andy.materialbrowser.browser.ProfileWallpaperRules
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.browser.WorkspaceNameRules
import org.json.JSONArray
import org.json.JSONObject

/**
 * The stored workspaces as read back. [unreadableEntries] are raw values that could not be parsed
 * into a workspace; they are written back unchanged so a damaged entry is never silently replaced.
 */
internal data class DecodedWorkspaceProfiles(
    val profiles: List<BrowserProfile>,
    val unreadableEntries: List<Any> = emptyList(),
) {
    val hasUnreadableEntries: Boolean
        get() = unreadableEntries.isNotEmpty()
}

/** What [BrowserSessionStore.loadStoredProfiles] returns to the browser at start. */
data class StoredProfiles(
    val profiles: List<BrowserProfile>,
    val activeProfileId: String,
    /** Some stored entry could not be read: tabs it owned must not move to another workspace. */
    val hasUnreadableEntries: Boolean,
)

/** Reads and writes the stored workspace list, one entry at a time. */
internal object WorkspaceProfilesCodec {
    /** What a protection object with an unknown policy turns into: lock on every leave. */
    val FAIL_CLOSED_PROTECTION = ProfileProtection(lockTrigger = ProfileLockTrigger.AppBackgrounded)

    fun decode(raw: String?): DecodedWorkspaceProfiles {
        if (raw == null) return DecodedWorkspaceProfiles(emptyList())
        // A value that is not an array at all is kept whole as one unreadable entry.
        val array = runCatching { JSONArray(raw) }.getOrNull()
            ?: return DecodedWorkspaceProfiles(emptyList(), listOf(raw))
        val profiles = mutableListOf<BrowserProfile>()
        val unreadable = mutableListOf<Any>()
        for (index in 0 until array.length()) {
            val entry = array.opt(index) ?: JSONObject.NULL
            val profile = (entry as? JSONObject)
                ?.let { item -> runCatching { decodeProfile(item) }.getOrNull() }
            when {
                profile == null -> unreadable += entry
                profiles.none { it.id == profile.id } -> profiles += profile
            }
        }
        return DecodedWorkspaceProfiles(profiles, unreadable)
    }

    fun encode(profiles: List<BrowserProfile>, unreadableEntries: List<Any> = emptyList()): String {
        val array = JSONArray()
        profiles.forEach { profile -> array.put(encodeProfile(profile)) }
        unreadableEntries.forEach(array::put)
        return array.toString()
    }

    private fun decodeProfile(item: JSONObject): BrowserProfile? {
        val id = item.optString("id").trim()
        val emoji = item.optString("emoji").trim()
        if (id.isEmpty() || emoji.isEmpty()) return null
        val legacyWallpaper = item.optJSONObject("wallpaper")?.toProfileWallpaper()
        val hasTargetWallpapers = item.has("newTabWallpaper") || item.has("tabSwitcherWallpaper")
        return BrowserProfile(
            id = id,
            emoji = emoji,
            name = WorkspaceNameRules.normalize(item.optString("name")),
            accent = WorkspaceAccent.fromWireValue(
                item.optString("accent").takeIf(String::isNotBlank),
            ),
            selectedTabId = item.optString("selectedTabId").takeIf(String::isNotBlank),
            isolationEnabled = item.optBoolean("isolationEnabled", false),
            protection = item.decodeProtection(),
            newTabWallpaper = if (hasTargetWallpapers) {
                item.optJSONObject("newTabWallpaper")?.toProfileWallpaper()
            } else {
                legacyWallpaper
            },
            tabSwitcherWallpaper = if (hasTargetWallpapers) {
                item.optJSONObject("tabSwitcherWallpaper")?.toProfileWallpaper()
            } else {
                legacyWallpaper
            },
        )
    }

    /** No protection only when none is stored; anything stored but unreadable fails closed. */
    private fun JSONObject.decodeProtection(): ProfileProtection? {
        if (!has("protection") || isNull("protection")) return null
        val stored = optJSONObject("protection") ?: return FAIL_CLOSED_PROTECTION
        val trigger = ProfileLockTrigger.fromWireValue(stored.optString("lockTrigger"))
            ?: return FAIL_CLOSED_PROTECTION
        return ProfileProtectionRules.normalize(
            ProfileProtection(
                lockTrigger = trigger,
                cooldownMinutes = stored.optInt(
                    "cooldownMinutes",
                    ProfileProtectionRules.DEFAULT_COOLDOWN_MINUTES,
                ),
            ),
        )
    }

    private fun encodeProfile(profile: BrowserProfile): JSONObject = JSONObject()
        .put("id", profile.id)
        .put("emoji", profile.emoji)
        .put("name", profile.name)
        .put("accent", profile.accent.wireValue)
        .put("selectedTabId", profile.selectedTabId)
        .put("isolationEnabled", profile.isolationEnabled)
        .put("protection", profile.protection.toJson())
        .put("newTabWallpaper", profile.newTabWallpaper.toJson())
        .put("tabSwitcherWallpaper", profile.tabSwitcherWallpaper.toJson())

    private fun JSONObject.toProfileWallpaper(): ProfileWallpaper = ProfileWallpaperRules.sanitize(
        ProfileWallpaper(
            zoom = optDouble("zoom", 1.0).toFloat(),
            normalizedPanX = optDouble("normalizedPanX", 0.0).toFloat(),
            normalizedPanY = optDouble("normalizedPanY", 0.0).toFloat(),
        ),
    )

    private fun ProfileWallpaper?.toJson(): Any = this
        ?.let(ProfileWallpaperRules::sanitize)
        ?.let { wallpaper ->
            JSONObject()
                .put("zoom", wallpaper.zoom.toDouble())
                .put("normalizedPanX", wallpaper.normalizedPanX.toDouble())
                .put("normalizedPanY", wallpaper.normalizedPanY.toDouble())
        }
        ?: JSONObject.NULL

    private fun ProfileProtection?.toJson(): Any = this
        ?.let(ProfileProtectionRules::normalize)
        ?.let { protection ->
            JSONObject()
                .put("lockTrigger", protection.lockTrigger.wireValue)
                .put("cooldownMinutes", protection.cooldownMinutes)
        }
        ?: JSONObject.NULL
}
