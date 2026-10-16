package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    if (rememberDrawsV5()) {
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
        return
    }
    AlertDialog(
        onDismissRequest = { onChoice(FederatedLoginPromptChoice.Deny) },
        modifier = Modifier.testTag(FederatedLoginPromptTestTags.Dialog),
        title = {
            Text(
                stringResource(
                    R.string.federated_login_dialog_title,
                    offer.provider.displayName,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.federated_login_dialog_message,
                        offer.pageHost,
                    ),
                )
                if (offer.isPrivate) {
                    Text(
                        stringResource(R.string.federated_login_private_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onChoice(FederatedLoginPromptChoice.AllowForTab) },
                    modifier = Modifier.testTag(FederatedLoginPromptTestTags.AllowForTab),
                ) {
                    Text(stringResource(R.string.federated_login_allow_for_tab))
                }
                if (!offer.isPrivate) {
                    FilledTonalButton(
                        onClick = { onChoice(FederatedLoginPromptChoice.AllowForProfile) },
                        modifier = Modifier.testTag(
                            FederatedLoginPromptTestTags.AllowForProfile,
                        ),
                    ) {
                        Text(stringResource(R.string.federated_login_allow_for_profile))
                    }
                }
                TextButton(
                    onClick = { onChoice(FederatedLoginPromptChoice.Deny) },
                    modifier = Modifier.testTag(FederatedLoginPromptTestTags.Deny),
                ) {
                    Text(stringResource(R.string.federated_login_deny))
                }
            }
        },
    )
}
