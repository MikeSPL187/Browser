package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.isSynced
import dev.sk2andy.materialbrowser.shared.ui.PlatformProfileEmoji
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp

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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_move_tab_to_profile)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                targets.forEach { profile ->
                    TextButton(
                        onClick = { onMove(profile.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PlatformProfileEmoji(emoji = profile.emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(profile.syncedDisplayName ?: profile.workspaceDisplayName())
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
