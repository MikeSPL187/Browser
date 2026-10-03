package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaStatePageTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

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
) {
    val name = workspace?.name?.takeIf(String::isNotBlank)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ProfileProtectionTestTags.LockedOverlay),
        color = MaterialTheme.colorScheme.surface,
    ) {
        VolaStatePage(
            title = if (name != null) {
                stringResource(R.string.profile_locked_title_named, name)
            } else {
                stringResource(R.string.profile_locked_title)
            },
            message = stringResource(
                if (unlockAvailable) {
                    R.string.profile_locked_message
                } else {
                    R.string.profile_protection_unavailable
                },
            ),
            icon = { LockedWorkspaceGem(workspace) },
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .safeDrawingPadding(),
        ) {
            VolaStatePagePrimaryButton(
                text = stringResource(R.string.profile_unlock_action),
                onClick = onUnlock,
                icon = VolaIcons.Fingerprint,
                enabled = unlockAvailable,
            )
            if (canSwitchProfile) {
                VolaStatePageTextButton(
                    text = stringResource(R.string.profile_locked_switch_action),
                    onClick = onSwitchProfile,
                )
            }
        }
    }
}

/** The workspace's gem with a lock on its corner; a lock alone when the workspace is unknown. */
@Composable
private fun LockedWorkspaceGem(workspace: BrowserProfile?) {
    if (workspace == null) {
        VolaStatePageIcon(icon = VolaIcons.Lock, tone = VolaStatePageTone.Accent)
        return
    }
    Box {
        WorkspaceGem(workspace = workspace, size = VolaStatePageTokens.iconContainerSize)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = VolaStatePageTokens.badgeOffset, y = VolaStatePageTokens.badgeOffset)
                .size(VolaStatePageTokens.badgeSize),
            shape = CircleShape,
            color = VolaTheme.extendedColors.card,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = VolaElevation.level2,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = VolaIcons.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(VolaStatePageTokens.badgeIconSize),
                )
            }
        }
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
