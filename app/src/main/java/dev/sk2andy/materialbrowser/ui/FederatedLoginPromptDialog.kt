package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.FederatedLoginOffer
import dev.sk2andy.materialbrowser.browser.FederatedLoginPromptChoice

internal object FederatedLoginPromptTestTags {
    const val Dialog = "federated-login-dialog"
    const val AllowForTab = "federated-login-allow-tab"
    const val AllowForProfile = "federated-login-allow-profile"
    const val Deny = "federated-login-deny"
}

@Composable
internal fun FederatedLoginPromptDialog(
    offer: FederatedLoginOffer,
    onChoice: (FederatedLoginPromptChoice) -> Unit,
) {
    // V28, B9a: under v5 on a phone the one v5 prompt sheet; closing it does not allow.
    val message = stringResource(R.string.federated_login_dialog_message, offer.pageHost)
    val privateNote = stringResource(R.string.federated_login_private_note)
    V5PromptTheme {
        V5PromptSheet(
            visible = true,
            title = stringResource(R.string.federated_login_dialog_title, offer.provider.displayName),
            message = if (offer.isPrivate) "$message\n\n$privateNote" else message,
            actions = listOfNotNull(
                V5PromptAction(
                    stringResource(R.string.federated_login_allow_for_tab),
                    { onChoice(FederatedLoginPromptChoice.AllowForTab) },
                    V5PromptRole.Primary,
                    FederatedLoginPromptTestTags.AllowForTab,
                ),
                V5PromptAction(
                    stringResource(R.string.federated_login_allow_for_profile),
                    { onChoice(FederatedLoginPromptChoice.AllowForProfile) },
                    testTag = FederatedLoginPromptTestTags.AllowForProfile,
                ).takeUnless { offer.isPrivate },
                V5PromptAction(
                    stringResource(R.string.federated_login_deny),
                    { onChoice(FederatedLoginPromptChoice.Deny) },
                    testTag = FederatedLoginPromptTestTags.Deny,
                ),
            ),
            onDismiss = { onChoice(FederatedLoginPromptChoice.Deny) },
            testTag = FederatedLoginPromptTestTags.Dialog,
        )
    }
}
