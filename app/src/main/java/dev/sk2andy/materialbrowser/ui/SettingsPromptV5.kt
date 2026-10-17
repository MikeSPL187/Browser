package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5NameField
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet

/*
 * The questions settings ask, as v5 asks them on a phone (owner 17.10: the phone all v5): the v5
 * prompt sheet with its one action, an address or a name typed in a v5 field. The pages keep
 * their current dialogs for a wide window.
 */

/** A settings question as the v5 prompt sheet; «Close» and a swipe down cancel. */
@Composable
internal fun SettingsPromptV5(
    title: String,
    message: String?,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    testTag: String? = null,
    confirmTestTag: String? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    /** A line in red under the message, such as a version that does not match. */
    warning: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    V5PromptTheme {
        V5PromptSheet(
            visible = true,
            title = title,
            message = message,
            actions = listOf(
                V5PromptAction(
                    label = confirm,
                    onClick = onConfirm,
                    role = if (destructive) V5PromptRole.Destructive else V5PromptRole.Primary,
                    testTag = confirmTestTag,
                    enabled = confirmEnabled,
                ),
            ),
            onDismiss = onDismiss,
            testTag = testTag,
        ) {
            if (warning != null) {
                Text(
                    warning,
                    style = V5Theme.type.footnote,
                    color = V5Theme.colors.danger,
                    modifier = Modifier.padding(horizontal = V5Spacing.space4),
                )
            }
            content()
        }
    }
}

/** A line typed into a [SettingsPromptV5]: the v5 field, and under it a hint or, in red, a problem. */
@Composable
internal fun SettingsPromptFieldV5(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    problem: String? = null,
    keyboardType: KeyboardType? = null,
    /** On a settings page, whose content keeps the side margin already. */
    inPage: Boolean = false,
) {
    val sideMargin = if (inPage) 0.dp else V5Spacing.marginPhone
    Column {
        V5NameField(
            value = value,
            placeholder = placeholder,
            onValueChange = onValueChange,
            onDone = onDone,
            maxLength = maxLength,
            capitalize = false,
            keyboardType = keyboardType,
            modifier = modifier,
            sideMargin = sideMargin,
        )
        val note = problem ?: hint
        if (note != null) {
            Text(
                note,
                style = V5Theme.type.footnote,
                color = if (problem != null) V5Theme.colors.danger else V5Theme.colors.labelSecondary,
                modifier = Modifier.padding(
                    start = sideMargin + V5Spacing.space4,
                    end = sideMargin,
                    top = V5Spacing.space2,
                ),
            )
        }
    }
}
