package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.CaptchaCompatibilityOffer
import dev.sk2andy.materialbrowser.browser.CaptchaCompatibilityPromptChoice

internal object CaptchaCompatibilityPromptTestTags {
    const val Dialog = "captcha-compatibility-dialog"
    const val AllowForTab = "captcha-compatibility-allow-tab"
    const val AllowForProfile = "captcha-compatibility-allow-profile"
    const val Deny = "captcha-compatibility-deny"
}

@Composable
internal fun CaptchaCompatibilityPromptDialog(
    offer: CaptchaCompatibilityOffer,
    onChoice: (CaptchaCompatibilityPromptChoice) -> Unit,
) {
    // V28, B9a: under v5 on a phone the one v5 prompt sheet; closing it does not allow.
    val message = stringResource(R.string.captcha_compatibility_dialog_message, offer.pageHost)
    val privateNote = stringResource(R.string.captcha_compatibility_private_note)
    V5PromptTheme {
        V5PromptSheet(
            visible = true,
            title = stringResource(R.string.captcha_compatibility_dialog_title, offer.provider.displayName),
            message = if (offer.isPrivate) "$message\n\n$privateNote" else message,
            actions = listOfNotNull(
                V5PromptAction(
                    stringResource(R.string.captcha_compatibility_allow_for_tab),
                    { onChoice(CaptchaCompatibilityPromptChoice.AllowForTab) },
                    V5PromptRole.Primary,
                    CaptchaCompatibilityPromptTestTags.AllowForTab,
                ),
                V5PromptAction(
                    stringResource(R.string.captcha_compatibility_allow_for_profile),
                    { onChoice(CaptchaCompatibilityPromptChoice.AllowForProfile) },
                    testTag = CaptchaCompatibilityPromptTestTags.AllowForProfile,
                ).takeUnless { offer.isPrivate },
                V5PromptAction(
                    stringResource(R.string.captcha_compatibility_deny),
                    { onChoice(CaptchaCompatibilityPromptChoice.Deny) },
                    testTag = CaptchaCompatibilityPromptTestTags.Deny,
                ),
            ),
            onDismiss = { onChoice(CaptchaCompatibilityPromptChoice.Deny) },
            testTag = CaptchaCompatibilityPromptTestTags.Dialog,
        )
    }
}
