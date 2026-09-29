package dev.sk2andy.materialbrowser.browser

/**
 * A workspace: its own set of tabs with a name, an emoji and an accent color. Storage isolation
 * turns a workspace into a container with separate cookies and sign-ins. The type keeps its
 * historical name inside the code; the UI calls it a workspace.
 */
data class BrowserProfile(
    val id: String,
    val emoji: String,
    val name: String = "",
    val accent: WorkspaceAccent = WorkspaceAccent.Default,
    val selectedTabId: String? = null,
    val isolationEnabled: Boolean = false,
    val protection: ProfileProtection? = null,
    val syncedDeviceId: String? = null,
    val syncedDisplayName: String? = null,
    val syncedIconCatalogId: String? = null,
    val syncedIconEmoji: String? = null,
    val syncedIconAccentHue: Int? = null,
    val linkedSyncDeviceId: String? = null,
    val newTabWallpaper: ProfileWallpaper? = null,
    val tabSwitcherWallpaper: ProfileWallpaper? = null,
)

data class ProfileProtection(
    val lockTrigger: ProfileLockTrigger,
    val cooldownMinutes: Int = ProfileProtectionRules.DEFAULT_COOLDOWN_MINUTES,
)

enum class ProfileLockTrigger(val wireValue: String) {
    AppBackgrounded("app_backgrounded"),
    AppClosed("app_closed"),
    Cooldown("cooldown"),
    ;

    companion object {
        fun fromWireValue(value: String?): ProfileLockTrigger? =
            entries.firstOrNull { it.wireValue == value }
    }
}

data class ProfileWallpaper(
    val zoom: Float = 1f,
    val normalizedPanX: Float = 0f,
    val normalizedPanY: Float = 0f,
)

enum class ProfileWallpaperTarget(val wireValue: String) {
    NewTab("new_tab"),
    TabSwitcher("tab_switcher"),
    ;

    companion object {
        fun fromWireValue(value: String?): ProfileWallpaperTarget? =
            entries.firstOrNull { it.wireValue == value }
    }
}

fun BrowserProfile.wallpaperFor(target: ProfileWallpaperTarget): ProfileWallpaper? = when (target) {
    ProfileWallpaperTarget.NewTab -> newTabWallpaper
    ProfileWallpaperTarget.TabSwitcher -> tabSwitcherWallpaper
}

fun BrowserProfile.withWallpaper(
    target: ProfileWallpaperTarget,
    wallpaper: ProfileWallpaper?,
): BrowserProfile = when (target) {
    ProfileWallpaperTarget.NewTab -> copy(newTabWallpaper = wallpaper)
    ProfileWallpaperTarget.TabSwitcher -> copy(tabSwitcherWallpaper = wallpaper)
}

val BrowserProfile.isSynced: Boolean
    get() = syncedDeviceId != null

val BrowserProfile.isSyncLinked: Boolean
    get() = syncedDeviceId != null || linkedSyncDeviceId != null

/** Accent colors a workspace can tint the browser chrome with. */
enum class WorkspaceAccent(val wireValue: String) {
    Violet("violet"),
    Blue("blue"),
    Teal("teal"),
    Green("green"),
    Amber("amber"),
    Coral("coral"),
    Rose("rose"),
    Graphite("graphite"),
    ;

    companion object {
        val Default = Violet

        fun fromWireValue(value: String?): WorkspaceAccent =
            entries.firstOrNull { it.wireValue == value } ?: Default
    }
}

object WorkspaceNameRules {
    const val MAX_LENGTH = 32

    /** Trims, collapses inner whitespace and caps the length; blank means "use the default". */
    fun normalize(value: String): String =
        value.trim().replace(WHITESPACE, " ").take(MAX_LENGTH).trim()

    private val WHITESPACE = Regex("\\s+")
}

const val DEFAULT_PROFILE_ID = "candy"
const val DEFAULT_PROFILE_EMOJI = "🏠"
const val MAX_PROFILES = 12

val DEFAULT_BROWSER_PROFILE = BrowserProfile(
    id = DEFAULT_PROFILE_ID,
    emoji = DEFAULT_PROFILE_EMOJI,
)
