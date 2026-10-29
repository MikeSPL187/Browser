package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.credentials.AndroidCredentialPromptHost
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginAnswer
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginPrompts

internal object VaultLoginSheetTestTags {
    const val Save = "vault_login_sheet_save"
    const val SaveConfirm = "vault_login_sheet_save_confirm"
    const val SaveDismiss = "vault_login_sheet_save_dismiss"
    const val Select = "vault_login_sheet_select"
    const val SelectDismiss = "vault_login_sheet_select_dismiss"
    const val Offer = "vault_login_sheet_offer"
    const val OfferSetUp = "vault_login_sheet_offer_set_up"
    const val OfferDismiss = "vault_login_sheet_offer_dismiss"
    fun account(index: Int) = "vault_login_sheet_account:$index"
}

/**
 * The sheets of Vola's password host over the page: «Save password?» after a sign-in and «Sign in
 * to …» when a login field is touched. Only the window that asked shows them.
 */
@Composable
internal fun VaultLoginPromptSheets() {
    val request = VaultLoginPrompts.current ?: return
    val activity = AndroidCredentialPromptHost.activityContext(LocalContext.current)
    if (activity == null || System.identityHashCode(activity) != request.windowId) return
    val dismiss = { VaultLoginPrompts.answer(request.id, VaultLoginAnswer.Dismiss) }
    VaultLoginPromptSheetV5(request, dismiss)
}
