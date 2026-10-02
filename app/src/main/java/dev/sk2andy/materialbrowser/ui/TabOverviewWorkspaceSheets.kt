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

    /** The workspace whose icon is being picked, or [NEW_PROFILE_TARGET] for a new one. */
    var emojiPickerTargetId by mutableStateOf<String?>(null)

    val isOpen: Boolean
        get() = actionsProfileId != null ||
            isolationChange != null ||
            protectionTargetId != null ||
            emojiPickerTargetId != null

    fun startCreating() {
        emojiPickerTargetId = NEW_PROFILE_TARGET
    }

    /** A workspace that just locked takes its open sheets with it. */
    fun forgetLocked(lockedProfileIds: Set<String>) {
        if (actionsProfileId in lockedProfileIds) actionsProfileId = null
        if (isolationChange?.first in lockedProfileIds) isolationChange = null
        if (protectionTargetId in lockedProfileIds) protectionTargetId = null
        if (emojiPickerTargetId in lockedProfileIds) emojiPickerTargetId = null
    }
}

/** The workspace sheets of the tab overview: settings, protection, storage and the icon picker. */
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
    ProfileActionsSheet(
        profile = actionProfile,
        canDelete = controller.localBrowserProfiles.size > 1,
        isolationSupported = controller.isProfileIsolationSupported,
        onChangeEmoji = {
            val target = actionProfile ?: return@ProfileActionsSheet
            state.actionsProfileId = null
            state.emojiPickerTargetId = target.id
        },
        onCustomizeWallpaper = { wallpaperTarget ->
            val target = actionProfile ?: return@ProfileActionsSheet
            state.actionsProfileId = null
            onEditProfileWallpaper(target.id, wallpaperTarget)
        },
        onDelete = {
            val target = actionProfile ?: return@ProfileActionsSheet
            state.actionsProfileId = null
            controller.deleteProfileAsync(target.id) { deleted ->
                if (deleted) rootView.performConfirmHaptic()
            }
        },
        onIsolationChange = { enabled ->
            val target = actionProfile ?: return@ProfileActionsSheet
            state.actionsProfileId = null
            state.isolationChange = target.id to enabled
        },
        profileProtectionSupported = controller.isProfileProtectionSupported,
        onConfigureProtection = {
            val target = actionProfile ?: return@ProfileActionsSheet
            state.actionsProfileId = null
            state.protectionTargetId = target.id
        },
        onDisableProtection = {
            val target = actionProfile ?: return@ProfileActionsSheet
            state.actionsProfileId = null
            controller.updateProfileProtection(target.id, protection = null) { changed ->
                if (changed) rootView.performConfirmHaptic()
            }
        },
        onDismiss = { state.actionsProfileId = null },
        onRename = { name ->
            val target = actionProfile ?: return@ProfileActionsSheet
            controller.updateProfileName(target.id, name)
        },
        onAccentChange = { accent ->
            val target = actionProfile ?: return@ProfileActionsSheet
            if (controller.updateProfileAccent(target.id, accent)) {
                rootView.performConfirmHaptic()
            }
        },
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

    val emojiPickerTarget = state.emojiPickerTargetId
    EmojiPickerSheet(
        visible = emojiPickerTarget != null,
        creatingProfile = emojiPickerTarget == NEW_PROFILE_TARGET,
        isolationSupported = controller.isProfileIsolationSupported,
        profileProtectionSupported = controller.isProfileProtectionSupported,
        emojis = controller.syncIconCatalog.icons.map { it.emoji },
        selectedEmoji = controller.localBrowserProfiles
            .firstOrNull { it.id == emojiPickerTarget }
            ?.emoji,
        onCreate = { emoji, isolationEnabled, options ->
            if (emojiPickerTarget != NEW_PROFILE_TARGET) return@EmojiPickerSheet
            val profileId = controller.createProfile(
                emoji = emoji,
                isolationEnabled = isolationEnabled,
                name = options.name,
                accent = options.accent,
            )
            if (profileId != null) {
                state.emojiPickerTargetId = null
                rootView.performConfirmHaptic()
                onConfigureCreatedProfile(profileId, options)
            }
        },
        onSelect = { emoji ->
            val target = emojiPickerTarget ?: return@EmojiPickerSheet
            if (target == NEW_PROFILE_TARGET) return@EmojiPickerSheet
            state.emojiPickerTargetId = null
            val changed = controller.updateProfileEmoji(target, emoji)
            if (changed) rootView.performConfirmHaptic()
        },
        onDismiss = { state.emojiPickerTargetId = null },
    )
}
