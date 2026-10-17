package dev.sk2andy.materialbrowser.ui.passwords

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginAnswer
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginPrompts
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginRequest
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.V5PromptTheme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing

/*
 * Vola's password sheets over the page as v5 asks every question (owner 17.10: the phone all v5):
 * the v5 prompt sheet with the account rows in a group. «Close» and a swipe down say «Not now».
 */

/** [request] as a v5 sheet; true once it is drawn. */
@Composable
internal fun VaultLoginPromptSheetV5(request: VaultLoginRequest, onDismiss: () -> Unit): Boolean {
    when (request) {
        is VaultLoginRequest.Save -> V5PromptTheme { SaveSheet(request, onDismiss) }
        is VaultLoginRequest.Select -> V5PromptTheme { SelectSheet(request, onDismiss) }
        is VaultLoginRequest.Offer -> V5PromptTheme { OfferSheet(request, onDismiss) }
        is VaultLoginRequest.Generate -> V5PromptTheme {
            PasswordGeneratorSheetV5(
                visible = true,
                subtitle = request.site,
                useLabel = stringResource(
                    if (request.canSave) R.string.passwords_generator_use_and_save else R.string.passwords_generator_use,
                ),
                useNeedsFingerprint = request.canSave,
                onUse = { password -> VaultLoginPrompts.answer(request.id, VaultLoginAnswer.Use(password)) },
                onDismiss = onDismiss,
            )
        }
    }
    return true
}

@Composable
private fun SaveSheet(request: VaultLoginRequest.Save, onDismiss: () -> Unit) {
    val username = request.username.ifEmpty { stringResource(R.string.passwords_no_username) }
    V5PromptSheet(
        visible = true,
        title = stringResource(
            if (request.update) R.string.passwords_sheet_update_title else R.string.passwords_sheet_save_title,
            request.site,
        ),
        message = stringResource(R.string.passwords_sheet_save_body),
        actions = listOf(
            V5PromptAction(
                stringResource(if (request.update) R.string.passwords_sheet_update else R.string.passwords_save),
                { VaultLoginPrompts.answer(request.id, VaultLoginAnswer.Save) },
                V5PromptRole.Primary,
                VaultLoginSheetTestTags.SaveConfirm,
            ),
        ),
        onDismiss = onDismiss,
        testTag = VaultLoginSheetTestTags.Save,
    ) {
        V5ListGroup {
            V5ListRow(
                title = username,
                subtitle = stringResource(R.string.passwords_sheet_hidden_password),
                icon = V5RowIcon(VolaIcons.Key, V5Tile.Gray),
            )
        }
    }
}

@Composable
private fun SelectSheet(
    request: VaultLoginRequest.Select,
    onDismiss: () -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
) {
    V5PromptSheet(
        visible = true,
        title = stringResource(R.string.passwords_sheet_signin_title, request.site),
        message = stringResource(R.string.passwords_sheet_verified),
        actions = listOf(
            V5PromptAction(
                stringResource(R.string.passwords_sheet_dont_fill),
                onDismiss,
                testTag = VaultLoginSheetTestTags.SelectDismiss,
            ),
        ),
        onDismiss = onDismiss,
        testTag = VaultLoginSheetTestTags.Select,
    ) {
        V5ListGroup {
            request.accounts.forEachIndexed { index, account ->
                val username = account.username.ifEmpty { stringResource(R.string.passwords_no_username) }
                V5ListRow(
                    title = username,
                    subtitle = account.lastUsedAtMillis?.let { used ->
                        stringResource(
                            R.string.passwords_sheet_last_used,
                            DateUtils.getRelativeTimeSpanString(used, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString(),
                        )
                    } ?: stringResource(R.string.passwords_sheet_never_used),
                    icon = V5RowIcon(VolaIcons.Key, V5Tile.Gray),
                    trailing = V5RowTrailing.Chevron,
                    onClick = { VaultLoginPrompts.answer(request.id, VaultLoginAnswer.Pick(account.username)) },
                    modifier = Modifier.testTag(VaultLoginSheetTestTags.account(index)),
                )
            }
        }
    }
}

/** «Sign in faster with Vola» while the vault is not set up (#123, H2). */
@Composable
private fun OfferSheet(request: VaultLoginRequest.Offer, onDismiss: () -> Unit) {
    V5PromptSheet(
        visible = true,
        title = stringResource(R.string.passwords_offer_title),
        message = stringResource(R.string.passwords_offer_body, request.site),
        actions = listOf(
            V5PromptAction(
                stringResource(R.string.passwords_offer_set_up),
                { VaultLoginPrompts.answer(request.id, VaultLoginAnswer.SetUp) },
                V5PromptRole.Primary,
                VaultLoginSheetTestTags.OfferSetUp,
            ),
        ),
        onDismiss = onDismiss,
        testTag = VaultLoginSheetTestTags.Offer,
    )
}
