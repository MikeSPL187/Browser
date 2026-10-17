package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.credentials.IdentityPromptRequest
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/* FedCM under ui.v5. Preview-only providers, accounts and addresses are not localized. */

/** Which account to sign in with, as a v5 choice sheet. */
@VolaPreviews
@Composable
private fun IdentityAccountChoiceV5Preview() {
    MaterialBrowserTheme {
        IdentityCredentialSheetV5(
            request = IdentityPromptRequest.Choice(
                id = 1,
                windowId = 0,
                title = "Example ID (id.example)",
                options = listOf("Anna Petrova\nanna@mail.example", "Work\nanna@work.example"),
            ),
            visible = true,
            onAnswer = {},
        )
    }
}

/** The provider's policies before the first sign-in, with «Continue». */
@VolaPreviews
@Composable
private fun IdentityPrivacyV5Preview() {
    MaterialBrowserTheme {
        IdentityCredentialSheetV5(
            request = IdentityPromptRequest.Privacy(
                id = 2,
                windowId = 0,
                providerName = "Example ID",
                providerDomain = "id.example",
                privacyPolicyUrl = "https://id.example/privacy",
                termsOfServiceUrl = "https://id.example/terms",
            ),
            visible = true,
            onAnswer = {},
        )
    }
}
