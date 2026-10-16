package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5NameField
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserWebPrompt
import dev.sk2andy.materialbrowser.browser.BrowserWebPromptChoice
import dev.sk2andy.materialbrowser.browser.BrowserWebPromptKind
import dev.sk2andy.materialbrowser.browser.BrowserWebPromptRules

@Composable
internal fun BrowserWebPromptDialog(
    prompt: BrowserWebPrompt,
    onConfirm: (String?) -> Unit,
    onCancel: () -> Unit,
) {
    var value by remember(prompt.id) { mutableStateOf(prompt.defaultValue.orEmpty()) }
    var selectedChoices by remember(prompt.id) {
        mutableStateOf(prompt.choices.filter(BrowserWebPromptChoice::selected).map(BrowserWebPromptChoice::id).toSet())
    }
    // V28, B9b: under v5 on a phone the one v5 prompt sheet; closing it cancels, as Back did.
    if (rememberDrawsV5()) {
        BrowserWebPromptSheetV5(
            prompt = prompt,
            value = value,
            onValueChange = { value = it },
            selectedChoices = selectedChoices,
            onToggleChoice = { id ->
                val selected = id in selectedChoices
                selectedChoices = if (prompt.allowMultiple) {
                    if (selected) selectedChoices - id else selectedChoices + id
                } else setOf(id)
            },
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
        return
    }
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag(BrowserWebPromptTestTags.Dialog),
        title = {
            Text(
                prompt.title ?: stringResource(
                    when (prompt.kind) {
                        BrowserWebPromptKind.BeforeUnload -> R.string.web_prompt_leave_title
                        BrowserWebPromptKind.Repost -> R.string.web_prompt_repost_title
                        else -> R.string.web_prompt_title
                    },
                ),
            )
        },
        text = {
            Column {
                val message = prompt.message ?: when (prompt.kind) {
                    BrowserWebPromptKind.BeforeUnload ->
                        stringResource(R.string.web_prompt_leave_message)
                    BrowserWebPromptKind.Repost -> stringResource(R.string.web_prompt_repost_message)
                    else -> null
                }
                message?.let { Text(it) }
                if (prompt.kind in setOf(BrowserWebPromptKind.Text, BrowserWebPromptKind.Color, BrowserWebPromptKind.DateTime)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { updated ->
                            value = updated.take(BrowserWebPromptRules.MAX_INPUT_LENGTH)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(BrowserWebPromptTestTags.Input),
                        singleLine = true,
                    )
                }
                if (prompt.kind == BrowserWebPromptKind.Choice) {
                    prompt.choices.filterNot(BrowserWebPromptChoice::separator).forEach { choice ->
                        val selected = choice.id in selectedChoices
                        TextButton(
                            enabled = !choice.disabled,
                            onClick = {
                                selectedChoices = if (prompt.allowMultiple) {
                                    if (selected) selectedChoices - choice.id else selectedChoices + choice.id
                                } else setOf(choice.id)
                            },
                        ) {
                            if (prompt.allowMultiple) Checkbox(checked = selected, onCheckedChange = null)
                            else RadioButton(selected = selected, onClick = null)
                            Text(choice.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        when (prompt.kind) {
                            BrowserWebPromptKind.Text,
                            BrowserWebPromptKind.Color,
                            BrowserWebPromptKind.DateTime,
                            -> value
                            BrowserWebPromptKind.Choice -> selectedChoices.joinToString("\u001F")
                            else -> null
                        },
                    )
                },
                modifier = Modifier.testTag(BrowserWebPromptTestTags.Confirm),
            ) {
                Text(
                    stringResource(
                        when (prompt.kind) {
                            BrowserWebPromptKind.BeforeUnload -> R.string.web_prompt_leave
                            BrowserWebPromptKind.Repost -> R.string.web_prompt_resend
                            else -> R.string.web_prompt_ok
                        },
                    ),
                )
            }
        },
        dismissButton = if (prompt.kind == BrowserWebPromptKind.Alert) {
            null
        } else {
            {
                TextButton(onClick = onCancel) {
                    Text(
                        stringResource(
                            if (prompt.kind == BrowserWebPromptKind.BeforeUnload) {
                                R.string.web_prompt_stay
                            } else {
                                R.string.action_cancel
                            },
                        ),
                    )
                }
            }
        },
    )
}

internal object BrowserWebPromptTestTags {
    const val Dialog = "browser_web_prompt_dialog"
    const val Input = "browser_web_prompt_input"
    const val Confirm = "browser_web_prompt_confirm"
}

private val TEXT_KINDS = setOf(BrowserWebPromptKind.Text, BrowserWebPromptKind.Color, BrowserWebPromptKind.DateTime)

/** The page's alert, confirm, prompt or select on the v5 prompt sheet (V28, B9b). */
@Composable
private fun BrowserWebPromptSheetV5(
    prompt: BrowserWebPrompt,
    value: String,
    onValueChange: (String) -> Unit,
    selectedChoices: Set<String>,
    onToggleChoice: (String) -> Unit,
    onConfirm: (String?) -> Unit,
    onCancel: () -> Unit,
) {
    val confirm = {
        onConfirm(
            when (prompt.kind) {
                in TEXT_KINDS -> value
                BrowserWebPromptKind.Choice -> selectedChoices.joinToString("\u001F")
                else -> null
            },
        )
    }
    val title = prompt.title ?: stringResource(
        when (prompt.kind) {
            BrowserWebPromptKind.BeforeUnload -> R.string.web_prompt_leave_title
            BrowserWebPromptKind.Repost -> R.string.web_prompt_repost_title
            else -> R.string.web_prompt_title
        },
    )
    val message = prompt.message ?: when (prompt.kind) {
        BrowserWebPromptKind.BeforeUnload -> stringResource(R.string.web_prompt_leave_message)
        BrowserWebPromptKind.Repost -> stringResource(R.string.web_prompt_repost_message)
        else -> null
    }
    val confirmLabel = stringResource(
        when (prompt.kind) {
            BrowserWebPromptKind.BeforeUnload -> R.string.web_prompt_leave
            BrowserWebPromptKind.Repost -> R.string.web_prompt_resend
            else -> R.string.web_prompt_ok
        },
    )
    // An alert only informs; every other question keeps its way out as a row too.
    val cancelLabel = stringResource(
        if (prompt.kind == BrowserWebPromptKind.BeforeUnload) R.string.web_prompt_stay else R.string.action_cancel,
    )
    V5PromptTheme {
        V5PromptSheet(
            visible = true,
            title = title,
            message = message,
            actions = listOfNotNull(
                V5PromptAction(confirmLabel, confirm, V5PromptRole.Primary, BrowserWebPromptTestTags.Confirm),
                V5PromptAction(cancelLabel, onCancel).takeUnless { prompt.kind == BrowserWebPromptKind.Alert },
            ),
            onDismiss = onCancel,
            testTag = BrowserWebPromptTestTags.Dialog,
        ) {
            if (prompt.kind in TEXT_KINDS) {
                V5NameField(
                    value = value,
                    placeholder = stringResource(R.string.v5_web_prompt_answer),
                    onValueChange = onValueChange,
                    onDone = confirm,
                    maxLength = BrowserWebPromptRules.MAX_INPUT_LENGTH,
                    modifier = Modifier.testTag(BrowserWebPromptTestTags.Input),
                    capitalize = false,
                )
            }
            if (prompt.kind == BrowserWebPromptKind.Choice) {
                V5ListGroup {
                    prompt.choices.filterNot(BrowserWebPromptChoice::separator).forEach { choice ->
                        V5ListRow(
                            title = choice.label,
                            trailing = if (choice.id in selectedChoices) V5RowTrailing.Check else V5RowTrailing.None,
                            enabled = !choice.disabled,
                            onClick = { onToggleChoice(choice.id) },
                        )
                    }
                }
            }
        }
    }
}
