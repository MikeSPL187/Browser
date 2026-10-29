package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.actions.PendingDownloadChoice
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet

@Composable
internal fun DownloadManagerChooserDialog(
    choice: PendingDownloadChoice,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    // V28, B9a: under v5 on a phone the one v5 prompt sheet, one row per download manager.
    val builtIn = stringResource(R.string.settings_download_manager_builtin)
    V5PromptTheme {
        V5PromptSheet(
            visible = true,
            title = stringResource(R.string.download_manager_chooser_title),
            message = null,
            actions = listOf(V5PromptAction(builtIn, { onSelect(null) })) +
                choice.apps.map { app -> V5PromptAction(app.label, { onSelect(app.id) }) },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun DownloadManagerChoiceRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = false, onClick = onClick)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
