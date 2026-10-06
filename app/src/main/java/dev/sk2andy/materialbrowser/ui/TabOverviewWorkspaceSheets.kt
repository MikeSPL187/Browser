package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.ProfileWallpaperTarget

/** Which workspace sheet or dialog the tab overview shows, if any. */
@Stable
internal class WorkspaceSheetsState {
    /** The workspace whose settings sheet is open (long press on its gem). */
    var actionsProfileId by mutableStateOf<String?>(null)

    /** A storage isolation change waiting for confirmation: workspace and the new value. */
    var isolationChange by mutableStateOf<Pair<String, Boolean>?>(null)
    var protectionTargetId by mutableStateOf<String?>(null)

    /** «New workspace» is open. */
    var creating by mutableStateOf(false)

    val isOpen: Boolean
        get() = actionsProfileId != null ||
            isolationChange != null ||
            protectionTargetId != null ||
            creating

    fun startCreating() {
        creating = true
    }

    /** A workspace that just locked takes its open sheets with it. */
    fun forgetLocked(lockedProfileIds: Set<String>) {
        if (actionsProfileId in lockedProfileIds) actionsProfileId = null
        if (isolationChange?.first in lockedProfileIds) isolationChange = null
        if (protectionTargetId in lockedProfileIds) protectionTargetId = null
    }
}

/** The workspace sheets of the tab overview: new workspace, its settings, protection, storage. */
@Composable
internal fun TabOverviewWorkspaceSheets(
    controller: BrowserController,
    state: WorkspaceSheetsState,
    onEditProfileWallpaper: (String, ProfileWallpaperTarget) -> Unit,
    onConfigureCreatedProfile: (String, ProfileCreationOptions) -> Unit,
) {
    val rootView = LocalView.current
    val actionProfile = state.actionsProfileId?.let { profileId ->
        controller.localBrowserProfiles.firstOrNull { it.id == profileId }
    }
    val icons = controller.syncIconCatalog.icons.map { it.emoji }
    // Closes the settings sheet, then runs [action] on its workspace.
    fun closeThen(action: (String) -> Unit) {
        val target = actionProfile ?: return
        state.actionsProfileId = null
        action(target.id)
    }
    WorkspaceSettingsSheet(
        profile = actionProfile,
        tabCount = actionProfile?.let { WorkspaceSheetRules.tabCount(controller.tabs, it.id) } ?: 0,
        essentialsCount = actionProfile?.let { controller.essentials.entriesFor(it.id).size } ?: 0,
        icons = icons,
        canDelete = actionProfile?.let { controller.canDeleteProfile(it.id) } == true,
        deleteBlockedBySync = actionProfile?.let { controller.isBoundSyncProfile(it.id) } == true,
        isolationSupported = controller.isProfileIsolationSupported,
        profileProtectionSupported = controller.isProfileProtectionSupported,
        onRename = { name ->
            actionProfile?.let { target -> controller.updateProfileName(target.id, name) }
        },
        onAccentChange = { accent ->
            val target = actionProfile ?: return@WorkspaceSettingsSheet
            if (controller.updateProfileAccent(target.id, accent)) rootView.performConfirmHaptic()
        },
        onIconChange = { emoji ->
            val target = actionProfile ?: return@WorkspaceSettingsSheet
            if (controller.updateProfileEmoji(target.id, emoji)) rootView.performConfirmHaptic()
        },
        onCustomizeWallpaper = { wallpaperTarget ->
            closeThen { profileId -> onEditProfileWallpaper(profileId, wallpaperTarget) }
        },
        onIsolationChange = { enabled ->
            closeThen { profileId -> state.isolationChange = profileId to enabled }
        },
        onEnableProtection = { closeThen { profileId -> state.protectionTargetId = profileId } },
        onDisableProtection = {
            val target = actionProfile ?: return@WorkspaceSettingsSheet
            controller.updateProfileProtection(target.id, protection = null) { changed ->
                if (changed) rootView.performConfirmHaptic()
            }
        },
        onDelete = {
            closeThen { profileId ->
                controller.deleteProfileAsync(profileId) { deleted ->
                    if (deleted) rootView.performConfirmHaptic()
                }
            }
        },
        onDismiss = { state.actionsProfileId = null },
    )

    val protectionProfile = state.protectionTargetId?.let { profileId ->
        controller.localBrowserProfiles.firstOrNull { profile -> profile.id == profileId }
    }
    if (protectionProfile != null) {
        ProfileProtectionDialog(
            current = protectionProfile.protection,
            onSave = { protection ->
                val profileId = protectionProfile.id
                state.protectionTargetId = null
                controller.updateProfileProtection(profileId, protection) { changed ->
                    if (changed) rootView.performConfirmHaptic()
                }
            },
            onDismiss = { state.protectionTargetId = null },
        )
    }

    state.isolationChange?.let { (profileId, enabled) ->
        AlertDialog(
            onDismissRequest = { state.isolationChange = null },
            title = { Text(stringResource(R.string.profile_isolation_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        if (enabled) R.string.profile_isolation_enable_message
                        else R.string.profile_isolation_disable_message,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        controller.setProfileIsolationAsync(profileId, enabled) { changed ->
                            if (changed) rootView.performConfirmHaptic()
                        }
                        state.isolationChange = null
                    },
                ) {
                    Text(stringResource(R.string.action_switch_storage))
                }
            },
            dismissButton = {
                TextButton(onClick = { state.isolationChange = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    NewWorkspaceSheet(
        visible = state.creating,
        isolationSupported = controller.isProfileIsolationSupported,
        profileProtectionSupported = controller.isProfileProtectionSupported,
        icons = icons,
        onCreate = { emoji, isolationEnabled, options ->
            val profileId = controller.createProfile(
                emoji = emoji,
                isolationEnabled = isolationEnabled,
                name = options.name,
                accent = options.accent,
            )
            if (profileId != null) {
                state.creating = false
                rootView.performConfirmHaptic()
                onConfigureCreatedProfile(profileId, options)
            }
        },
        onDismiss = { state.creating = false },
    )
}
