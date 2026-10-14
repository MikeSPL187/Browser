package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.browser.isSynced
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileItem
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileSheet

/** Profiles as v5 shows them (V11a, system-v5.md 1.2 and 10). */
internal object V5ProfileRules {
    /**
     * The v5 accent of a profile's stored color. v5 offers six accents; the two older colors with
     * no twin go to their nearest one (teal to green, coral to orange).
     */
    fun accent(accent: WorkspaceAccent): V5Accent = when (accent) {
        WorkspaceAccent.Violet -> V5Accent.Purple
        WorkspaceAccent.Blue -> V5Accent.Blue
        WorkspaceAccent.Teal, WorkspaceAccent.Green -> V5Accent.Green
        WorkspaceAccent.Amber, WorkspaceAccent.Coral -> V5Accent.Orange
        WorkspaceAccent.Rose -> V5Accent.Pink
        WorkspaceAccent.Graphite -> V5Accent.Graphite
    }

    /** The sheet's rows: this device's profiles in their order, then those from other devices. */
    fun items(
        profiles: List<BrowserProfile>,
        tabs: List<BrowserTab>,
        activeProfileId: String,
        lockedProfileIds: Set<String>,
        untitled: String,
    ): List<V5ProfileItem> {
        val counts = tabs.groupingBy(BrowserTab::profileId).eachCount()
        return profiles.sortedBy(BrowserProfile::isSynced).map { profile ->
            V5ProfileItem(
                id = profile.id,
                emoji = profile.emoji,
                name = profile.name.ifBlank { profile.syncedDisplayName.orEmpty() }.ifBlank { untitled },
                accent = accent(profile.accent),
                tabCount = counts[profile.id] ?: 0,
                active = profile.id == activeProfileId,
                locked = profile.id in lockedProfileIds,
                device = profile.syncedDisplayName.takeIf { profile.isSynced },
            )
        }
    }
}

/**
 * The profiles sheet of the v5 overview: a touch switches to the profile (a locked one asks to
 * sign in first) and the sheet goes, leaving the overview on the new profile's tabs.
 */
@Composable
internal fun BrowserProfileSheetV5(controller: BrowserController, visible: Boolean, onDismiss: () -> Unit) {
    val untitled = stringResource(R.string.workspace_untitled)
    V5ProfileSheet(
        visible = visible,
        profiles = V5ProfileRules.items(
            profiles = controller.profiles,
            tabs = controller.tabs.filterNot(BrowserTab::isIncognito),
            activeProfileId = controller.activeProfileId,
            lockedProfileIds = controller.lockedProfileIds,
            untitled = untitled,
        ),
        onSelect = { id ->
            if (id == controller.activeProfileId && !controller.isActiveProfileLocked) {
                onDismiss()
            } else {
                controller.requestProfileSelection(id) { switched -> if (switched) onDismiss() }
            }
        },
        onDismiss = onDismiss,
    )
}
