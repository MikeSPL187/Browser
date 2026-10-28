package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.WorkspaceIconResources
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_EMOJI
import dev.sk2andy.materialbrowser.browser.DistinctNames
import dev.sk2andy.materialbrowser.browser.MAX_PROFILES
import dev.sk2andy.materialbrowser.browser.ProfileLockTrigger
import dev.sk2andy.materialbrowser.browser.ProfileProtection
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.browser.WorkspaceDeletionRules
import dev.sk2andy.materialbrowser.browser.WorkspaceNameRules
import dev.sk2andy.materialbrowser.browser.isSynced
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.LocalV5ProfileAccent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileDraft
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileEditSheet
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileIconChoice
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileItem
import dev.sk2andy.materialbrowser.ui.v5.V5ProfilePrivacy
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileSheet
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet

/** The active profile's v5 accent, which every v5 screen takes (V11c, system-v5.md 1.2). */
internal val BrowserController.v5Accent: V5Accent
    get() = profiles.firstOrNull { it.id == activeProfileId }?.let { V5ProfileRules.accent(it.accent) } ?: V5Accent.Default

/** [LocalV5ProfileAccent] under the browser's own name, where the browser's screens provide it. */
internal val ProfileAccentV5 get() = LocalV5ProfileAccent

/** Profiles as v5 shows them (V11a, system-v5.md 1.2 and 10). */
internal object V5ProfileRules {
    /** The profiles in the order the sheet lists them and the overview pages through them: this device's first. */
    fun order(profiles: List<BrowserProfile>): List<BrowserProfile> = profiles.sortedBy(BrowserProfile::isSynced)

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

    /** The stored color for a v5 accent the user chose (its twin among the stored ones). */
    fun stored(accent: V5Accent): WorkspaceAccent = when (accent) {
        V5Accent.Blue -> WorkspaceAccent.Blue
        V5Accent.Green -> WorkspaceAccent.Green
        V5Accent.Orange -> WorkspaceAccent.Amber
        V5Accent.Pink -> WorkspaceAccent.Rose
        V5Accent.Purple -> WorkspaceAccent.Violet
        V5Accent.Graphite -> WorkspaceAccent.Graphite
    }

    /**
     * The name a profile is saved under: blank becomes [untitled], and a name another of [others]
     * already shows gets the next number, so no two profiles read the same (PF-03).
     */
    fun distinctName(name: String, others: List<BrowserProfile>, untitled: String): String = DistinctNames.distinct(
        name = WorkspaceNameRules.normalize(name).ifBlank { untitled },
        taken = others.map { it.name.ifBlank { untitled } },
        maxLength = WorkspaceNameRules.MAX_LENGTH,
    )

    /** The sheet's rows: this device's profiles in their order, then those from other devices. */
    fun items(
        profiles: List<BrowserProfile>,
        tabs: List<BrowserTab>,
        activeProfileId: String,
        lockedProfileIds: Set<String>,
        untitled: String,
    ): List<V5ProfileItem> {
        val counts = tabs.groupingBy(BrowserTab::profileId).eachCount()
        return order(profiles).map { profile ->
            V5ProfileItem(
                id = profile.id,
                icon = WorkspaceIcons.vector(profile.emoji),
                name = profile.name.ifBlank { profile.syncedDisplayName.orEmpty() }.ifBlank { untitled },
                accent = accent(profile.accent),
                // A locked profile hides its tabs until it is opened, their number too (ПР-4).
                tabCount = if (profile.id in lockedProfileIds) null else counts[profile.id] ?: 0,
                active = profile.id == activeProfileId,
                locked = profile.id in lockedProfileIds,
                device = profile.syncedDisplayName.takeIf { profile.isSynced },
            )
        }
    }
}

/** Which profile the edit sheet is open for (V11b). */
private sealed interface ProfileEdit {
    data object New : ProfileEdit

    data class Existing(val id: String) : ProfileEdit
}

/**
 * The profiles sheet of the v5 overview: a touch switches to the profile (a locked one asks to
 * sign in first) and the sheet goes, leaving the overview on the new profile's tabs. «New profile»
 * and a long press open the edit sheet in its place, one layer at a time.
 */
@Composable
internal fun BrowserProfileSheetV5(controller: BrowserController, visible: Boolean, onDismiss: () -> Unit) {
    val untitled = stringResource(R.string.v5_profile_untitled)
    var edit by remember { mutableStateOf<ProfileEdit?>(null) }
    LaunchedEffect(visible) { if (!visible) edit = null }
    val local = controller.localBrowserProfiles
    V5ProfileSheet(
        visible = visible && edit == null,
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
        onNew = { edit = ProfileEdit.New },
        profileLimit = MAX_PROFILES.takeIf { local.size >= it },
        onEdit = { id ->
            // Only this device's profiles are edited here, and a locked one only after it is opened.
            if (local.any { it.id == id } && id !in controller.lockedProfileIds) edit = ProfileEdit.Existing(id)
        },
    )
    BrowserProfileEditSheetV5(
        controller = controller,
        edit = edit,
        onBack = { edit = null },
        onFinished = {
            edit = null
            onDismiss()
        },
    )
}

/** «New profile» and «Edit profile» bound to the browser's profiles. */
@Composable
private fun BrowserProfileEditSheetV5(
    controller: BrowserController,
    edit: ProfileEdit?,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val untitled = stringResource(R.string.v5_profile_untitled)
    val profile = (edit as? ProfileEdit.Existing)?.let { target ->
        controller.localBrowserProfiles.firstOrNull { it.id == target.id }
    }
    val iconKeys = controller.syncIconCatalog.icons.map { it.emoji }
    val icons = iconKeys.map { key ->
        V5ProfileIconChoice(key, WorkspaceIcons.vector(key), stringResource(WorkspaceIconResources.nameFor(key)))
    }
    var newOwnStorage by remember(edit) { mutableStateOf(controller.isProfileIsolationSupported) }
    var deleting by remember { mutableStateOf<BrowserProfile?>(null) }
    var storageChange by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val initial = remember(edit, profile?.id) {
        profile?.let { V5ProfileDraft(it.name, it.emoji, V5ProfileRules.accent(it.accent)) }
            ?: V5ProfileDraft(
                name = "",
                iconKey = iconKeys.firstOrNull() ?: DEFAULT_PROFILE_EMOJI,
                accent = V5Accent.Default,
            )
    }
    V5ProfileEditSheet(
        visible = edit == ProfileEdit.New || profile != null,
        creating = edit == ProfileEdit.New,
        initial = initial,
        icons = icons,
        privacy = if (profile == null) {
            V5ProfilePrivacy(
                ownStorage = newOwnStorage.takeIf { controller.isProfileIsolationSupported },
                biometricLock = null,
            )
        } else {
            V5ProfilePrivacy(
                ownStorage = profile.isolationEnabled.takeIf { controller.isProfileIsolationSupported },
                biometricLock = (profile.protection != null).takeIf { controller.isProfileProtectionSupported },
            )
        },
        onSave = { draft ->
            if (profile == null) {
                val created = controller.createProfile(
                    emoji = draft.iconKey,
                    isolationEnabled = newOwnStorage && controller.isProfileIsolationSupported,
                    // Other devices' profiles count too: the overview and TalkBack show them side by side (ПР-6).
                    name = V5ProfileRules.distinctName(draft.name, controller.profiles, untitled),
                    accent = V5ProfileRules.stored(draft.accent),
                )
                if (created != null) {
                    view.performV5Haptic(V5Haptic.Confirm)
                } else {
                    // The limit reached from elsewhere, or the store refused: said, not just gone (ПР-2).
                    view.performV5Haptic(V5Haptic.Reject)
                    V5Feedback.show(context, R.string.v5_profile_create_failed)
                }
                onFinished()
            } else {
                if (draft.name != profile.name) {
                    val others = controller.profiles.filter { it.id != profile.id }
                    controller.updateProfileName(profile.id, V5ProfileRules.distinctName(draft.name, others, untitled))
                }
                if (draft.iconKey != profile.emoji) controller.updateProfileEmoji(profile.id, draft.iconKey)
                if (draft.accent != initial.accent) {
                    controller.updateProfileAccent(profile.id, V5ProfileRules.stored(draft.accent))
                }
                onBack()
            }
        },
        onOwnStorageChange = { on ->
            if (profile == null) newOwnStorage = on else storageChange = profile.id to on
        },
        onBiometricLockChange = { on ->
            profile?.let { target ->
                val protection = if (on) ProfileProtection(ProfileLockTrigger.AppBackgrounded) else null
                controller.updateProfileProtection(target.id, protection) { changed ->
                    if (changed) view.performV5Haptic(if (on) V5Haptic.ToggleOn else V5Haptic.ToggleOff)
                }
            }
        },
        onDismiss = onBack,
        onDelete = profile?.takeIf { controller.canDeleteProfile(it.id) }?.let { target -> { deleting = target } },
        deleteUnavailable = stringResource(
            if (controller.localBrowserProfiles.size <= 1) R.string.v5_profile_delete_last else R.string.v5_profile_delete_unavailable,
        ),
    )
    // Both questions are v5 prompt sheets: «Close» and a swipe down keep things as they are.
    deleting?.let { target ->
        val name = target.name.ifBlank { untitled }
        val local = controller.localBrowserProfiles
        val heir = WorkspaceDeletionRules.fallbackProfileId(
            localProfileIds = local.map(BrowserProfile::id),
            activeProfileId = controller.activeProfileId,
            deletedProfileId = target.id,
        )?.let { id -> local.firstOrNull { it.id == id } }
        V5PromptSheet(
            visible = true,
            title = stringResource(R.string.workspace_delete_confirm_title, name),
            // Everything deleteProfileAsync takes with the profile, and where its tabs go (PF-04).
            message = stringResource(
                if (target.isolationEnabled) R.string.v5_profile_delete_message_storage else R.string.v5_profile_delete_message,
                heir?.name?.ifBlank { untitled } ?: untitled,
            ),
            actions = listOf(
                V5PromptAction(
                    label = stringResource(R.string.action_delete),
                    role = V5PromptRole.Destructive,
                    onClick = {
                        deleting = null
                        onBack()
                        controller.deleteProfileAsync(target.id) { deleted ->
                            if (deleted) view.performV5Haptic(V5Haptic.Confirm)
                        }
                    },
                ),
            ),
            onDismiss = { deleting = null },
        )
    }
    storageChange?.let { (profileId, on) ->
        V5PromptSheet(
            visible = true,
            title = stringResource(R.string.v5_profile_storage_title),
            message = stringResource(if (on) R.string.v5_profile_storage_on else R.string.v5_profile_storage_off),
            actions = listOf(
                V5PromptAction(
                    label = stringResource(R.string.action_switch_storage),
                    role = V5PromptRole.Primary,
                    onClick = {
                        storageChange = null
                        controller.setProfileIsolationAsync(profileId, on) { changed ->
                            if (changed) view.performV5Haptic(V5Haptic.Confirm)
                        }
                    },
                ),
            ),
            onDismiss = { storageChange = null },
        )
    }
}
