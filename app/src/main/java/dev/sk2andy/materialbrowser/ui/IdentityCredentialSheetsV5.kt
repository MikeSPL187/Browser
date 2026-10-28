package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.AndroidCredentialPromptHost
import dev.sk2andy.materialbrowser.browser.credentials.IdentityCredentialPrompts
import dev.sk2andy.materialbrowser.browser.credentials.IdentityPromptRequest
import dev.sk2andy.materialbrowser.ui.v5.V5ChoiceSheet
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet

internal object IdentityCredentialSheetTestTags {
    const val Choice = "identity_credential_choice"
    const val Privacy = "identity_credential_privacy"
}

/**
 * FedCM's questions under v5 (the phone wholly on v5): which provider or account to sign in with
 * as a v5 choice sheet, the provider's policies as a v5 prompt with «Continue». While this window
 * draws v5 it tells [IdentityCredentialPrompts] so, and the host asks here instead of in a system
 * dialog; on the tablet (v4) nothing is drawn and the dialogs stay. «Close» and a swipe down answer
 * as the dialogs' «Cancel» did.
 */
@Composable
internal fun IdentityCredentialPromptSheets() {
    val activity = AndroidCredentialPromptHost.activityContext(LocalContext.current) ?: return
    val windowId = System.identityHashCode(activity)
    DisposableEffect(windowId) {
        IdentityCredentialPrompts.attach(windowId)
        onDispose { IdentityCredentialPrompts.detach(windowId) }
    }
    val current = IdentityCredentialPrompts.current?.takeIf { it.windowId == windowId }
    // The last question stays drawn while its sheet goes down.
    var shown by remember { mutableStateOf<IdentityPromptRequest?>(null) }
    SideEffect { if (current != null) shown = current }
    val request = current ?: shown ?: return
    IdentityCredentialSheetV5(
        request = request,
        visible = current?.id == request.id,
        onAnswer = { answer -> IdentityCredentialPrompts.answer(request.id, answer) },
    )
}

/** One question as its v5 sheet; [onAnswer] gets the index, [IdentityPromptRequest.ACCEPT] or null. */
@Composable
internal fun IdentityCredentialSheetV5(request: IdentityPromptRequest, visible: Boolean, onAnswer: (Int?) -> Unit) {
    V5PromptTheme {
        when (request) {
            is IdentityPromptRequest.Choice -> V5ChoiceSheet(
                visible = visible,
                title = request.title,
                options = request.options,
                selected = -1,
                onSelect = onAnswer,
                onDismiss = { onAnswer(null) },
                testTag = IdentityCredentialSheetTestTags.Choice,
            )
            is IdentityPromptRequest.Privacy -> V5PromptSheet(
                visible = visible,
                title = stringResource(R.string.identity_sign_in_with, request.providerName),
                message = listOfNotNull(
                    request.providerDomain,
                    stringResource(R.string.identity_privacy_policy, request.privacyPolicyUrl),
                    request.termsOfServiceUrl?.let { stringResource(R.string.identity_terms_of_service, it) },
                ).joinToString("\n\n"),
                actions = listOf(
                    V5PromptAction(
                        label = stringResource(R.string.identity_continue),
                        onClick = { onAnswer(IdentityPromptRequest.ACCEPT) },
                        role = V5PromptRole.Primary,
                    ),
                ),
                onDismiss = { onAnswer(null) },
                testTag = IdentityCredentialSheetTestTags.Privacy,
            )
        }
    }
}
