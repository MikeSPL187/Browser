package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.isSynced
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileSheet
@Composable
internal fun MoveTabToProfileDialog(
    tab: BrowserTab?,
    profiles: List<BrowserProfile>,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val resolvedTab = tab ?: return
    val targets = profiles.filter { profile ->
        profile.id != resolvedTab.profileId &&
            (!resolvedTab.isIncognito || !profile.isSynced)
    }
    LaunchedEffect(resolvedTab.id, targets) {
        if (targets.isEmpty()) onDismiss()
    }
    if (targets.isEmpty()) return
    MoveTabToProfileSheetV5(targets, onMove, onDismiss)
}

/**
 * Under v5 on a phone (V28, B11): the profiles sheet, titled «Move to profile», listing only the
 * profiles the tab can go to; one touch moves it.
 */
@Composable
private fun MoveTabToProfileSheetV5(
    targets: List<BrowserProfile>,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val untitled = stringResource(R.string.v5_profile_untitled)
    val dark = MaterialTheme.colorScheme.background.luminance() < MOVE_SHEET_DARK_LUMINANCE
    VolaV5Theme(dark = dark) {
        V5ProfileSheet(
            visible = true,
            profiles = V5ProfileRules.items(
                profiles = targets,
                tabs = emptyList(),
                activeProfileId = "",
                lockedProfileIds = emptySet(),
                untitled = untitled,
            ).map { item -> item.copy(tabCount = null) },
            onSelect = onMove,
            onDismiss = onDismiss,
            title = stringResource(R.string.action_move_tab_to_profile),
        )
    }
}

private const val MOVE_SHEET_DARK_LUMINANCE = 0.5f
