package dev.sk2andy.materialbrowser.ui.passwords

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.AndroidCredentialPromptHost
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginAccount
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginAnswer
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginPrompts
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginRequest
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.LibraryRow
import dev.sk2andy.materialbrowser.ui.LibrarySiteTile
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPasswords
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultLoginPromptSheets() {
    val request = VaultLoginPrompts.current ?: return
    val activity = AndroidCredentialPromptHost.activityContext(LocalContext.current)
    if (activity == null || System.identityHashCode(activity) != request.windowId) return
    val dismiss = { VaultLoginPrompts.answer(request.id, VaultLoginAnswer.Dismiss) }
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        when (request) {
            is VaultLoginRequest.Save -> VaultSaveSheetContent(
                request = request,
                onSave = { VaultLoginPrompts.answer(request.id, VaultLoginAnswer.Save) },
                onDismiss = dismiss,
            )
            is VaultLoginRequest.Generate -> PasswordGeneratorContent(
                subtitle = request.site,
                useLabel = stringResource(
                    if (request.canSave) R.string.passwords_generator_use_and_save else R.string.passwords_generator_use,
                ),
                useNeedsFingerprint = request.canSave,
                onUse = { password -> VaultLoginPrompts.answer(request.id, VaultLoginAnswer.Use(password)) },
            )
            is VaultLoginRequest.Offer -> VaultOfferSheetContent(
                request = request,
                onSetUp = { VaultLoginPrompts.answer(request.id, VaultLoginAnswer.SetUp) },
                onDismiss = dismiss,
            )
            is VaultLoginRequest.Select -> VaultSelectSheetContent(
                request = request,
                onPick = { username -> VaultLoginPrompts.answer(request.id, VaultLoginAnswer.Pick(username)) },
                onDismiss = dismiss,
            )
        }
    }
}

@Composable
internal fun VaultSaveSheetContent(
    request: VaultLoginRequest.Save,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    VaultSheetColumn(modifier = Modifier.testTag(VaultLoginSheetTestTags.Save)) {
        VaultSheetHeader(
            title = stringResource(
                if (request.update) R.string.passwords_sheet_update_title else R.string.passwords_sheet_save_title,
                request.site,
            ),
        ) {
            Text(
                text = stringResource(R.string.passwords_sheet_save_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        VaultSheetCard {
            val username = request.username.ifEmpty { stringResource(R.string.passwords_no_username) }
            LibraryRow(
                title = username,
                detail = stringResource(R.string.passwords_sheet_hidden_password),
                leading = { LibrarySiteTile(label = username, colorKey = request.site) },
                onClick = {},
                enabled = false,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VolaPasswords.sheetButtonGap),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(VolaPasswords.sheetButtonHeight)
                    .testTag(VaultLoginSheetTestTags.SaveDismiss),
            ) {
                Text(stringResource(R.string.passwords_sheet_not_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .height(VolaPasswords.sheetButtonHeight)
                    .testTag(VaultLoginSheetTestTags.SaveConfirm),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                VaultButtonIcon(VolaIcons.Fingerprint)
                Text(
                    stringResource(if (request.update) R.string.passwords_sheet_update else R.string.passwords_save),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun VaultSelectSheetContent(
    request: VaultLoginRequest.Select,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
) {
    VaultSheetColumn(modifier = Modifier.testTag(VaultLoginSheetTestTags.Select)) {
        VaultSheetHeader(title = stringResource(R.string.passwords_sheet_signin_title, request.site)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(VolaPasswords.sheetVerifiedGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    VolaIcons.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(VolaPasswords.sheetVerifiedIcon),
                    tint = VolaTheme.extendedColors.ok,
                )
                Text(
                    text = stringResource(R.string.passwords_sheet_verified),
                    style = MaterialTheme.typography.labelMedium,
                    color = VolaTheme.extendedColors.ok,
                )
            }
        }
        VaultSheetCard {
            request.accounts.forEachIndexed { index, account ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                AccountRow(
                    account = account,
                    site = request.site,
                    nowMillis = nowMillis,
                    onClick = { onPick(account.username) },
                    modifier = Modifier.testTag(VaultLoginSheetTestTags.account(index)),
                )
            }
        }
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(VolaPasswords.sheetButtonHeight)
                .testTag(VaultLoginSheetTestTags.SelectDismiss),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            VaultButtonIcon(VolaIcons.Close)
            Text(stringResource(R.string.passwords_sheet_dont_fill))
        }
    }
}

/**
 * «Sign in faster with Vola» on a sign-in field while the vault is not set up (#123, H2): the
 * system password manager fills only browsers it trusts, so the passwords have to move here.
 */
@Composable
internal fun VaultOfferSheetContent(
    request: VaultLoginRequest.Offer,
    onSetUp: () -> Unit,
    onDismiss: () -> Unit,
) {
    VaultSheetColumn(modifier = Modifier.testTag(VaultLoginSheetTestTags.Offer)) {
        VaultSheetHeader(title = stringResource(R.string.passwords_offer_title)) {
            Text(
                text = stringResource(R.string.passwords_offer_body, request.site),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VolaPasswords.sheetButtonGap),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(VolaPasswords.sheetButtonHeight)
                    .testTag(VaultLoginSheetTestTags.OfferDismiss),
            ) {
                Text(stringResource(R.string.passwords_sheet_not_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onSetUp,
                modifier = Modifier
                    .weight(1f)
                    .height(VolaPasswords.sheetButtonHeight)
                    .testTag(VaultLoginSheetTestTags.OfferSetUp),
            ) {
                Text(stringResource(R.string.passwords_offer_set_up), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: VaultLoginAccount,
    site: String,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val username = account.username.ifEmpty { stringResource(R.string.passwords_no_username) }
    val detail = account.lastUsedAtMillis?.let { used ->
        stringResource(
            R.string.passwords_sheet_last_used,
            DateUtils.getRelativeTimeSpanString(used, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString(),
        )
    } ?: stringResource(R.string.passwords_sheet_never_used)
    LibraryRow(
        title = username,
        detail = detail,
        leading = { LibrarySiteTile(label = username, colorKey = site + username) },
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Box(
                modifier = Modifier.size(VolaPasswords.sheetActionSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    VolaIcons.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(VolaPasswords.sheetActionIcon),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
internal fun VaultSheetColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(VolaPasswords.sheetPadding),
        verticalArrangement = Arrangement.spacedBy(VolaPasswords.sheetGap),
    ) {
        content()
    }
}

@Composable
internal fun VaultSheetHeader(title: String, subtitle: @Composable () -> Unit) {
    Row(
        modifier = Modifier.padding(VolaPasswords.sheetHeaderPadding),
        horizontalArrangement = Arrangement.spacedBy(VolaPasswords.sheetHeaderGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(VolaPasswords.sheetIconSize)
                .background(MaterialTheme.colorScheme.secondaryContainer, VolaPasswords.sheetIconShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VolaIcons.Key,
                contentDescription = null,
                modifier = Modifier.size(VolaPasswords.sheetIconGlyph),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            subtitle()
        }
    }
}

@Composable
internal fun VaultSheetCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = VolaPasswords.sheetCardShape,
        color = VolaTheme.extendedColors.card,
    ) {
        Column { content() }
    }
}

@Composable
internal fun VaultButtonIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
}

/** «Sign in to …», the vault offer and «Save password?» on one sheet each (board W-Autofill). */
@VolaPreviews
@Composable
private fun VaultLoginSheetsPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                VaultSelectSheetContent(
                    request = VaultLoginRequest.Select(
                        id = 1,
                        windowId = 0,
                        site = "mail.example.com",
                        accounts = listOf(
                            VaultLoginAccount("anna@example.com", lastUsedAtMillis = PREVIEW_NOW - DateUtils.DAY_IN_MILLIS),
                            VaultLoginAccount("anna.work@example.com", lastUsedAtMillis = null),
                        ),
                    ),
                    onPick = {},
                    onDismiss = {},
                    nowMillis = PREVIEW_NOW,
                )
                VaultOfferSheetContent(
                    request = VaultLoginRequest.Offer(id = 3, windowId = 0, site = "id.vk.ru"),
                    onSetUp = {},
                    onDismiss = {},
                )
                VaultSaveSheetContent(
                    request = VaultLoginRequest.Save(
                        id = 2,
                        windowId = 0,
                        site = "mail.example.com",
                        username = "anna@example.com",
                        update = false,
                    ),
                    onSave = {},
                    onDismiss = {},
                )
            }
        }
    }
}

private const val PREVIEW_NOW = 1_759_600_000_000L
