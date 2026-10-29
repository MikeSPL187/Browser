package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.v5.V5NameField
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.HttpAuthPrompt

@Composable
internal fun HttpAuthPromptDialog(
    prompt: HttpAuthPrompt,
    onSubmit: (username: String, password: String) -> Unit,
    onCancel: () -> Unit,
) {
    var username by remember(prompt.id) { mutableStateOf("") }
    var password by remember(prompt.id) { mutableStateOf("") }
    var passwordVisible by remember(prompt.id) { mutableStateOf(false) }
    // V28, B9b: under v5 on a phone the one v5 prompt sheet with the two fields; closing it cancels.
    val message = stringResource(
        if (prompt.isProxy) R.string.http_auth_proxy_dialog_message else R.string.http_auth_dialog_message,
        prompt.host,
    )
    val realm = prompt.realm?.let { stringResource(R.string.http_auth_dialog_realm, it) }
    val insecure = stringResource(
        if (prompt.isProxy) R.string.http_auth_proxy_insecure_warning else R.string.http_auth_dialog_insecure_warning,
    ).takeUnless { prompt.isPageSecure }
    V5PromptTheme {
        V5PromptSheet(
            visible = true,
            title = stringResource(
                if (prompt.isProxy) R.string.http_auth_proxy_dialog_title else R.string.http_auth_dialog_title,
            ),
            message = listOfNotNull(message, realm, insecure).joinToString("\n\n"),
            actions = listOf(
                V5PromptAction(
                    stringResource(R.string.http_auth_sign_in),
                    { onSubmit(username, password) },
                    V5PromptRole.Primary,
                    HttpAuthPromptTestTags.Submit,
                ),
            ),
            onDismiss = onCancel,
            testTag = HttpAuthPromptTestTags.Dialog,
        ) {
            V5NameField(
                value = username,
                placeholder = stringResource(R.string.http_auth_username),
                onValueChange = { username = it },
                onDone = {},
                maxLength = MAX_HTTP_AUTH_USERNAME_LENGTH,
                modifier = Modifier.testTag(HttpAuthPromptTestTags.Username),
                capitalize = false,
            )
            V5NameField(
                value = password,
                placeholder = stringResource(R.string.http_auth_password),
                onValueChange = { password = it },
                onDone = { onSubmit(username, password) },
                maxLength = MAX_HTTP_AUTH_PASSWORD_LENGTH,
                modifier = Modifier.testTag(HttpAuthPromptTestTags.Password),
                password = true,
            )
        }
    }
}

internal object HttpAuthPromptTestTags {
    const val Dialog = "http_auth_dialog"
    const val Username = "http_auth_username"
    const val Password = "http_auth_password"
    const val Submit = "http_auth_submit"
}

private const val MAX_HTTP_AUTH_USERNAME_LENGTH = 1_024
private const val MAX_HTTP_AUTH_PASSWORD_LENGTH = 4_096
