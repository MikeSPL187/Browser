package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncState
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileSync
import dev.sk2andy.materialbrowser.ui.v5.V5SettingsProfile
import dev.sk2andy.materialbrowser.ui.v5.V5SettingsProfileCard

/**
 * The profile card on top of the v5 settings (canvas «Settings») bound to the browser: the profile
 * in front and how the own server's sync stands. A tap opens the same profiles sheet the overview
 * has, so profiles keep one place; with profiles off it opens Settings → Sync instead.
 */
@Composable
internal fun BrowserSettingsProfileCardV5(controller: BrowserController, onOpenSync: () -> Unit) {
    val sync = rememberBrowserRemoteSync(controller)
    var sheet by rememberSaveable { mutableStateOf(false) }
    val profile = controller.profiles.firstOrNull { it.id == controller.activeProfileId }
    V5SettingsProfileCard(
        profile = V5SettingsProfile(
            name = profile.v5DisplayName(),
            icon = profile?.let { WorkspaceIcons.vector(it.emoji) },
            sync = BrowserSettingsProfileRules.sync(sync.available, sync.state, sync::relativeTime),
        ),
        onClick = { if (controller.profilesEnabled) sheet = true else onOpenSync() },
    )
    BrowserProfileSheetV5(controller, visible = sheet, onDismiss = { sheet = false })
}

internal object BrowserSettingsProfileRules {
    /** The line under the profile's name: off, syncing, failed, or when it last synced. */
    fun sync(available: Boolean, state: RemoteSyncState, relativeTime: (Long) -> String): V5ProfileSync = when {
        !available -> V5ProfileSync.Unavailable
        !state.connected -> V5ProfileSync.Off
        state.syncing -> V5ProfileSync.Syncing
        state.lastError != null -> V5ProfileSync.Failed
        else -> V5ProfileSync.Synced(state.lastSyncedAtMillis?.let(relativeTime))
    }
}
