package dev.sk2andy.materialbrowser.shared.ui

/** A workspace as the profile sheets show it. */
data class BrowserViewportProfile(
    val id: String,
    val emoji: String,
    val isolationEnabled: Boolean = false,
    val displayName: String? = null,
    val syncedIconEmoji: String? = null,
    val syncedIconAccentHue: Int? = null,
    val isSyncLinked: Boolean = false,
)

object ProfileSwitcherTestTags {
    const val Switcher = "profile_switcher"
    const val Add = "profile_switcher_add"

    fun profile(profileId: String): String = "profile_switcher_profile:$profileId"

    fun syncedBadge(profileId: String): String = "profile_switcher_synced_badge:$profileId"
}
