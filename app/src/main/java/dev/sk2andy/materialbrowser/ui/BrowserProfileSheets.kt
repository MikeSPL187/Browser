package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.ProfileProtection
import dev.sk2andy.materialbrowser.browser.ProfileWallpaperTarget
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent

internal data class ProfileCreationOptions(
    val protection: ProfileProtection? = null,
    val wallpaperTargets: Set<ProfileWallpaperTarget> = emptySet(),
    val name: String = "",
    val accent: WorkspaceAccent = WorkspaceAccent.Default,
)

