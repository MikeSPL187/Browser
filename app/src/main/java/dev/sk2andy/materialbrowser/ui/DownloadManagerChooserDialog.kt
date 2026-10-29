package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
