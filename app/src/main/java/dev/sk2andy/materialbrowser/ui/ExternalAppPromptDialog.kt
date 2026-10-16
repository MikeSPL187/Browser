package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.ExternalAppPrompt

internal object ExternalAppPromptTestTags {
    const val Dialog = "external_app_prompt_dialog"
    const val Confirm = "external_app_prompt_confirm"
    const val Cancel = "external_app_prompt_cancel"
}

@Composable
internal fun ExternalAppPromptDialog(
    prompt: ExternalAppPrompt,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    // V28, B9a: under v5 on a phone the one v5 prompt sheet; closing it cancels.
    if (rememberDrawsV5()) {
        V5PromptTheme {
            V5PromptSheet(
                visible = true,
                title = stringResource(R.string.external_app_prompt_title),
                message = stringResource(R.string.external_app_prompt_message, prompt.destination),
                actions = listOf(
                    V5PromptAction(
                        stringResource(R.string.external_app_prompt_confirm),
                        onConfirm,
                        V5PromptRole.Primary,
                        ExternalAppPromptTestTags.Confirm,
                    ),
                ),
                onDismiss = onCancel,
                testTag = ExternalAppPromptTestTags.Dialog,
            )
        }
        return
    }
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag(ExternalAppPromptTestTags.Dialog),
        title = { Text(stringResource(R.string.external_app_prompt_title)) },
        text = {
            Text(
                stringResource(
                    R.string.external_app_prompt_message,
                    prompt.destination,
                ),
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag(ExternalAppPromptTestTags.Confirm),
            ) {
                Text(stringResource(R.string.external_app_prompt_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(ExternalAppPromptTestTags.Cancel),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
