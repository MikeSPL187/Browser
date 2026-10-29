package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.ui.theme.v5.LocalV5ProfileAccent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5StateAction
import dev.sk2andy.materialbrowser.ui.v5.V5StatePage
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/**
 * «Workspace locked» (board W-Locked) over a protected workspace until the user confirms it is
 * them. Its tabs are hidden underneath; the way out goes to another workspace.
 */
@Composable
internal fun ProfileLockedOverlay(
    workspace: BrowserProfile?,
    unlockAvailable: Boolean,
    canSwitchProfile: Boolean,
    onUnlock: () -> Unit,
    onSwitchProfile: () -> Unit,
    /**
     * The way out when nothing can open the profile and there is no other one (H-01): a new profile,
     * which leaves this one and its tabs locked and untouched. Null where another way exists.
     */
    onNewProfile: (() -> Unit)? = null,
) {
    val name = workspace?.name?.takeIf(String::isNotBlank)
    // V28, B13: under v5 on a phone the one v5 lock screen, in the profile's accent.
    VolaV5Theme(
        dark = MaterialTheme.colorScheme.background.luminance() < PROFILE_LOCK_DARK_LUMINANCE,
        accent = workspace?.let { V5ProfileRules.accent(it.accent) } ?: LocalV5ProfileAccent.current,
    ) {
        V5StatePage(
            icon = VolaIcons.Lock,
            title = if (name != null) {
                stringResource(R.string.profile_locked_title_named, name)
            } else {
                stringResource(R.string.profile_locked_title)
            },
            message = stringResource(
                if (unlockAvailable) R.string.profile_locked_message else R.string.profile_protection_unavailable,
            ),
            primary = V5StateAction(
                label = stringResource(R.string.profile_unlock_action),
                onClick = onUnlock,
                icon = VolaIcons.Fingerprint,
                enabled = unlockAvailable,
            ),
            secondary = listOfNotNull(
                V5StateAction(stringResource(R.string.profile_locked_switch_action), onSwitchProfile)
                    .takeIf { canSwitchProfile },
                onNewProfile?.let { V5StateAction(stringResource(R.string.profile_locked_new_action), it) },
            ),
            modifier = Modifier.testTag(ProfileProtectionTestTags.LockedOverlay),
        )
    }
}

internal object ProfileProtectionTestTags {
    const val LockedOverlay = "profile_locked_overlay"
}

@VolaPreviews
@Composable
private fun ProfileLockedOverlayPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        ProfileLockedOverlay(
            workspace = ProfileLockedPreviewWorkspace,
            unlockAvailable = true,
            canSwitchProfile = true,
            onUnlock = {},
            onSwitchProfile = {},
        )
    }
}

/** The «Work» workspace of the W-Locked board; also used by the tour's debug screen. */
internal val ProfileLockedPreviewWorkspace = BrowserProfile(
    id = "work",
    emoji = WorkspaceIcons.emojiFor("work").orEmpty(),
    name = "Work",
    accent = WorkspaceAccent.Teal,
)

/** Below this background luminance the shell is dark. */
private const val PROFILE_LOCK_DARK_LUMINANCE = 0.5f
