@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.commands.BrowserCommand
import dev.sk2andy.materialbrowser.browser.commands.CommandConfirmation
import dev.sk2andy.materialbrowser.browser.commands.CommandCookieScope
import dev.sk2andy.materialbrowser.browser.commands.CommandSuggestion
import dev.sk2andy.materialbrowser.capsule.SiteCapsule
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet

@Composable
internal fun BrowserTransientOverlays(
    controller: BrowserController,
    clearDialogVisible: Boolean,
    onClearDialogDismiss: () -> Unit,
    pendingCommand: CommandSuggestion?,
    onPendingCommandDismiss: () -> Unit,
    onPendingCommandConfirmed: (BrowserCommand) -> Unit,
    pendingCapsuleDelete: SiteCapsule?,
    onPendingCapsuleDeleteDismiss: () -> Unit,
) {
    if (clearDialogVisible) {
        ConfirmPrompt(
            title = stringResource(R.string.clear_data_title),
            message = stringResource(R.string.clear_data_scope_message),
            confirm = stringResource(R.string.action_delete),
            onConfirm = {
                controller.clearBrowsingData()
                onClearDialogDismiss()
            },
            onDismiss = onClearDialogDismiss,
        )
    }

    pendingCommand?.let { pending ->
        val isCookieCommand = pending.command.confirmation == CommandConfirmation.ClearCookies
        ConfirmPrompt(
            title = stringResource(
                if (isCookieCommand) {
                    R.string.command_cookie_confirm_title
                } else {
                    R.string.command_duplicates_confirm_title
                },
            ),
            message = if (isCookieCommand) {
                stringResource(
                    when (controller.commandCookieScope) {
                        CommandCookieScope.SharedRegularProfile -> R.string.command_cookie_confirm_regular
                        CommandCookieScope.IsolatedRegularProfile -> R.string.command_cookie_confirm_isolated
                        CommandCookieScope.PrivateProfile -> R.string.command_cookie_confirm_private
                        CommandCookieScope.AllBrowserProfiles -> R.string.command_cookie_confirm_all
                    },
                )
            } else {
                pluralStringResource(
                    R.plurals.command_duplicates_confirm_message,
                    pending.command.duplicateCount,
                    pending.command.duplicateCount,
                )
            },
            confirm = stringResource(
                if (isCookieCommand) R.string.action_delete else R.string.command_close_duplicates_name,
            ),
            onConfirm = { onPendingCommandConfirmed(pending.command) },
            onDismiss = onPendingCommandDismiss,
        )
    }

    if (controller.contentActions.isLinkPeekVisible) {
        BrowserLinkGlanceV5(controller)
    } else if (controller.contentActions.isVisible) {
        // A long press with no link under it, such as an image: the actions as a v5 sheet.
        controller.contentActions.target?.let { target ->
            V5PromptTheme {
                V5PromptSheet(
                    visible = true,
                    title = stringResource(R.string.content_actions_title),
                    message = null,
                    actions = listOfNotNull(
                        V5PromptAction(
                            stringResource(R.string.action_open_link_background_tab),
                            controller::openContextLinkInBackground,
                        ).takeIf { target.canOpenLinkInBackground },
                        V5PromptAction(
                            stringResource(R.string.action_download_image),
                            controller::downloadContextImage,
                        ).takeIf { target.canDownloadImage },
                    ),
                    onDismiss = controller.contentActions::dismiss,
                )
            }
        }
    }

    pendingCapsuleDelete?.let { capsule ->
        AlertDialog(
            onDismissRequest = { onPendingCapsuleDeleteDismiss() },
            title = { Text(stringResource(R.string.capsule_delete_title)) },
            text = {
                Text(
                    stringResource(
                        if (capsule.ownsDedicatedProfile) {
                            R.string.capsule_delete_dedicated_message
                        } else {
                            R.string.capsule_delete_message
                        },
                    ),
                )
            },
            // Three choices stand in a column, the removing one first and «Cancel» last, so a large font
            // or a narrow phone never squeezes them into one row (L-07).
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = {
                            controller.deleteSiteCapsuleAsync(capsule.id, capsule.ownsDedicatedProfile) {}
                            onPendingCapsuleDeleteDismiss()
                        },
                    ) {
                        Text(
                            stringResource(
                                if (capsule.ownsDedicatedProfile) R.string.capsule_delete_with_profile else R.string.action_delete,
                            ),
                        )
                    }
                    if (capsule.ownsDedicatedProfile) {
                        TextButton(
                            onClick = {
                                controller.deleteSiteCapsuleAsync(capsule.id, false) {}
                                onPendingCapsuleDeleteDismiss()
                            },
                        ) { Text(stringResource(R.string.capsule_delete_only)) }
                    }
                    TextButton(onClick = { onPendingCapsuleDeleteDismiss() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            },
        )
    }
}

/**
 * A question before something that cannot be taken back: the v5 prompt sheet (its «Close» and a
 * swipe down cancel). Both questions here remove data, so the action is red.
 */
@Composable
private fun ConfirmPrompt(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    V5PromptTheme {
        V5PromptSheet(
            visible = true,
            title = title,
            message = message,
            actions = listOf(V5PromptAction(confirm, onConfirm, V5PromptRole.Destructive)),
            onDismiss = onDismiss,
        )
    }
}
