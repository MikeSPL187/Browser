package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.PasskeyAvailability
import dev.sk2andy.materialbrowser.browser.credentials.TotpCodes
import dev.sk2andy.materialbrowser.shared.credentials.TotpRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.V5PromptTheme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.v5.V5ListDefaults
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5NameField
import dev.sk2andy.materialbrowser.ui.v5.V5PageBar
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5SearchField
import dev.sk2andy.materialbrowser.ui.v5.V5StateAction
import dev.sk2andy.materialbrowser.ui.v5.V5StatePage
import kotlinx.coroutines.delay

/*
 * The passwords under v5 (canvas «ц4»–«ц7», approved 17.10): closed, the list, one password, a new
 * or edited one — v5 pages, groups and sheets. What each action does is the activity's, through
 * the same callbacks and test tags as the current screens; the tablet keeps them.
 */

/** «ц4»: the vault is closed. Open it with the fingerprint, with the phrase, or erase it. */
@Composable
internal fun PasswordsLockedV5(
    busy: Boolean,
    message: String?,
    onUnlock: () -> Unit,
    onRecover: () -> Unit,
    onBack: () -> Unit,
    onErase: () -> Unit,
) {
    var erasing by remember { mutableStateOf(false) }
    V5PromptTheme {
        Box(Modifier.fillMaxSize()) {
            V5StatePage(
                icon = VolaIcons.Lock,
                title = stringResource(R.string.passwords_locked_title),
                message = message ?: stringResource(R.string.passwords_locked_body),
                primary = V5StateAction(
                    label = stringResource(R.string.passwords_unlock),
                    onClick = onUnlock,
                    icon = VolaIcons.Fingerprint,
                    enabled = !busy,
                    testTag = PasswordsTestTags.Unlock,
                ),
                secondary = listOf(
                    V5StateAction(stringResource(R.string.passwords_recover), onRecover, testTag = PasswordsTestTags.Recover),
                    V5StateAction(
                        stringResource(R.string.passwords_erase_locked),
                        { erasing = true },
                        testTag = PasswordsTestTags.Erase,
                        destructive = true,
                    ),
                ),
            )
            V5PageBar(title = "", onBack = onBack, modifier = Modifier.statusBarsPadding())
        }
        EraseSheet(visible = erasing && !busy, onErase = onErase, onDismiss = { erasing = false })
    }
}

/** «ц5»: search, the password check first, then every password with its site and user name. */
@Composable
internal fun PasswordsListV5(
    logins: List<VaultLogin>,
    onOpen: (VaultLogin) -> Unit,
    onAdd: () -> Unit,
    onLock: () -> Unit,
    onBack: () -> Unit,
    systemFillNote: Boolean,
    passkeyNote: PasskeyAvailability?,
    healthIssues: Int,
    onHealth: () -> Unit,
    onImport: () -> Unit,
    busy: Boolean,
    onTurnOff: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var erasing by remember { mutableStateOf(false) }
    val visible = remember(logins, query) { PasswordsRules.filter(logins, query) }
    V5PromptTheme {
        val colors = V5Theme.colors
        Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
            V5PageBar(title = stringResource(R.string.passwords_title), onBack = onBack) {
                IconButton(onClick = onLock, modifier = Modifier.testTag(PasswordsTestTags.Lock)) {
                    Icon(VolaIcons.Lock, contentDescription = stringResource(R.string.passwords_lock), tint = colors.accent)
                }
                IconButton(onClick = onAdd, modifier = Modifier.testTag(PasswordsTestTags.Add)) {
                    Icon(VolaIcons.Add, contentDescription = stringResource(R.string.passwords_add), tint = colors.accent)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = V5Spacing.space2, bottom = V5Spacing.space8)
                    .testTag(PasswordsTestTags.List),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
            ) {
                V5SearchField(
                    query = query,
                    onQueryChange = { query = it.take(MAX_QUERY_LENGTH) },
                    hint = stringResource(R.string.passwords_search),
                    modifier = Modifier.padding(horizontal = V5Spacing.marginPhone).fillMaxWidth(),
                    fieldModifier = Modifier.testTag(PasswordsTestTags.Search),
                )
                if (query.isBlank()) {
                    V5ListGroup {
                        if (logins.isNotEmpty()) {
                            V5ListRow(
                                title = stringResource(R.string.passwords_health_entry),
                                icon = V5RowIcon(VolaIcons.GppMaybe, V5Tile.Orange),
                                value = if (healthIssues > 0) {
                                    pluralStringResource(R.plurals.passwords_health_entry_issues, healthIssues, healthIssues)
                                } else {
                                    stringResource(R.string.passwords_health_entry_fine)
                                },
                                trailing = V5RowTrailing.Chevron,
                                onClick = onHealth,
                                modifier = Modifier.testTag(PasswordsTestTags.Health),
                            )
                        }
                        V5ListRow(
                            title = stringResource(R.string.passwords_import_entry),
                            icon = V5RowIcon(VolaIcons.UploadFile, V5Tile.Blue),
                            trailing = V5RowTrailing.Chevron,
                            onClick = onImport,
                            modifier = Modifier.testTag(PasswordsTestTags.Import),
                        )
                    }
                }
                ListNotes(systemFillNote, passkeyNote)
                if (visible.isEmpty()) {
                    val searching = query.isNotBlank()
                    EmptyNote(
                        title = stringResource(if (searching) R.string.passwords_no_results else R.string.passwords_empty_title),
                        body = stringResource(if (searching) R.string.passwords_no_results_body else R.string.passwords_empty_body),
                    )
                } else {
                    V5ListGroup(
                        header = stringResource(R.string.passwords_all_count, visible.size),
                        dividerInset = V5ListDefaults.dividerInsetText,
                    ) {
                        visible.forEach { login ->
                            V5ListRow(
                                title = PasswordsRules.displaySite(login.origin),
                                subtitle = login.username.ifEmpty { stringResource(R.string.passwords_no_username) },
                                trailing = V5RowTrailing.Chevron,
                                onClick = { onOpen(login) },
                                modifier = Modifier.testTag(PasswordsTestTags.login(login.id)),
                            )
                        }
                    }
                }
                if (query.isBlank()) {
                    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                        V5ListRow(
                            title = stringResource(R.string.passwords_turn_off),
                            titleColor = colors.danger,
                            enabled = !busy,
                            onClick = { erasing = true },
                            modifier = Modifier.testTag(PasswordsTestTags.Erase),
                        )
                    }
                }
            }
        }
        EraseSheet(visible = erasing && !busy, onErase = onTurnOff, onDismiss = { erasing = false })
    }
}

/** «ц6»: one password. A row copies on a tap; the password shows only on request; delete in red. */
@Composable
internal fun PasswordDetailV5(
    login: VaultLogin,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onCopyCode: (String) -> Unit,
    onSetTotp: (String) -> Boolean,
    onRemoveTotp: () -> Unit,
) {
    // Shown only while this page is on screen: never restored after the app was in the background.
    var revealed by remember(login.id) { mutableStateOf(false) }
    var addingTotp by remember { mutableStateOf(false) }
    var confirmRemoveTotp by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val site = PasswordsRules.displaySite(login.origin)
    V5PromptTheme {
        val colors = V5Theme.colors
        Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
            V5PageBar(title = site, onBack = onBack) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.passwords_edit), style = V5Theme.type.body, color = colors.accent)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
            ) {
                V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                    V5ListRow(
                        title = stringResource(R.string.passwords_username),
                        value = login.username.ifEmpty { stringResource(R.string.passwords_no_username) },
                        enabled = login.username.isNotEmpty(),
                        onClick = onCopyUsername,
                        onLongClick = onCopyUsername,
                        onLongClickLabel = stringResource(R.string.passwords_copy_username),
                    )
                    PasswordRow(login.password, revealed, onReveal = { revealed = !revealed }, onCopy = onCopyPassword)
                    val totp = login.totp
                    if (totp != null) {
                        TotpRow(totp, onCopy = onCopyCode)
                    } else {
                        V5ListRow(
                            title = stringResource(R.string.passwords_totp_add),
                            titleColor = colors.accent,
                            onClick = { addingTotp = true },
                            modifier = Modifier.testTag(PasswordsTestTags.AddTotp),
                        )
                    }
                    V5ListRow(title = stringResource(R.string.passwords_site), value = login.origin)
                }
                V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                    if (login.totp != null) {
                        V5ListRow(
                            title = stringResource(R.string.passwords_totp_remove),
                            titleColor = colors.danger,
                            onClick = { confirmRemoveTotp = true },
                            modifier = Modifier.testTag(PasswordsTestTags.RemoveTotp),
                        )
                    }
                    V5ListRow(
                        title = stringResource(R.string.passwords_delete),
                        titleColor = colors.danger,
                        onClick = { confirmDelete = true },
                        modifier = Modifier.testTag(PasswordsTestTags.Delete),
                    )
                }
            }
        }
        TotpSheet(visible = addingTotp, onSave = { input -> onSetTotp(input).also { if (it) addingTotp = false } }) {
            addingTotp = false
        }
        V5PromptSheet(
            visible = confirmRemoveTotp,
            title = stringResource(R.string.passwords_totp_remove_title),
            message = stringResource(R.string.passwords_totp_remove_body, site),
            actions = listOf(
                V5PromptAction(stringResource(R.string.passwords_delete_action), {
                    confirmRemoveTotp = false
                    onRemoveTotp()
                }, V5PromptRole.Destructive),
            ),
            onDismiss = { confirmRemoveTotp = false },
        )
        V5PromptSheet(
            visible = confirmDelete,
            title = stringResource(R.string.passwords_delete_title),
            message = stringResource(R.string.passwords_delete_body, site),
            actions = listOf(
                V5PromptAction(stringResource(R.string.passwords_delete_action), {
                    confirmDelete = false
                    onDelete()
                }, V5PromptRole.Destructive),
            ),
            onDismiss = { confirmDelete = false },
        )
    }
}

/** «ц7»: a new password or an edited one — site, user name and password, «Save» in the bar. */
@Composable
internal fun PasswordEditV5(
    initial: VaultLogin?,
    error: PasswordEditError?,
    busy: Boolean,
    onSave: (site: String, username: String, password: String) -> Unit,
    onBack: () -> Unit,
) {
    var generating by remember { mutableStateOf(false) }
    var site by remember(initial?.id) { mutableStateOf(initial?.let { PasswordsRules.displaySite(it.origin) }.orEmpty()) }
    var username by remember(initial?.id) { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember(initial?.id) { mutableStateOf(initial?.password.orEmpty()) }
    val save = { if (!busy) onSave(site, username, password) }
    V5PromptTheme {
        val colors = V5Theme.colors
        Column(
            Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding(),
        ) {
            V5PageBar(title = stringResource(if (initial == null) R.string.passwords_add else R.string.passwords_edit), onBack = onBack) {
                TextButton(onClick = save, enabled = !busy, modifier = Modifier.testTag(PasswordsTestTags.Save)) {
                    Text(
                        stringResource(R.string.passwords_save),
                        style = V5Theme.type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
            ) {
                V5NameField(
                    value = site,
                    placeholder = stringResource(R.string.passwords_site_hint),
                    onValueChange = { site = it },
                    onDone = {},
                    maxLength = MAX_SITE_LENGTH,
                    capitalize = false,
                    keyboardType = KeyboardType.Uri,
                )
                V5NameField(
                    value = username,
                    placeholder = stringResource(R.string.passwords_username),
                    onValueChange = { username = it },
                    onDone = {},
                    maxLength = MAX_USERNAME_LENGTH,
                    capitalize = false,
                    keyboardType = KeyboardType.Email,
                )
                V5NameField(
                    value = password,
                    placeholder = stringResource(R.string.passwords_password),
                    onValueChange = { password = it },
                    onDone = save,
                    maxLength = MAX_PASSWORD_LENGTH,
                    password = true,
                )
                V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                    V5ListRow(
                        title = stringResource(R.string.passwords_generator_open),
                        titleColor = colors.accent,
                        onClick = { generating = true },
                        modifier = Modifier.testTag(PasswordsTestTags.Generate),
                    )
                }
                if (error != null) {
                    Text(
                        stringResource(error.messageRes()),
                        style = V5Theme.type.footnote,
                        color = colors.danger,
                        modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                    )
                }
            }
        }
        // The v5 generator sheet over the form; «Use» puts the password in the field.
        PasswordGeneratorSheetV5(
            visible = generating,
            subtitle = null,
            useLabel = stringResource(R.string.passwords_generator_use),
            useNeedsFingerprint = false,
            onUse = { generated ->
                password = generated
                generating = false
            },
            onDismiss = { generating = false },
        )
    }
}

@Composable
private fun PasswordRow(password: String, revealed: Boolean, onReveal: () -> Unit, onCopy: () -> Unit) {
    val colors = V5Theme.colors
    V5ListRow(
        title = stringResource(R.string.passwords_password),
        value = if (revealed) password else MASK,
        onClick = onCopy,
        onLongClick = onCopy,
        onLongClickLabel = stringResource(R.string.passwords_copy_password),
        modifier = Modifier.testTag(PasswordsTestTags.PasswordValue),
        trailing = V5RowTrailing.None,
    )
    V5ListRow(
        title = stringResource(if (revealed) R.string.passwords_hide else R.string.passwords_show),
        titleColor = colors.accent,
        onClick = onReveal,
        modifier = Modifier.testTag(PasswordsTestTags.Reveal),
    )
}

/** The two-factor code: grouped digits and seconds left; a tap copies it. It renews every period. */
@Composable
private fun TotpRow(totp: String, onCopy: (String) -> Unit) {
    val config = remember(totp) { TotpRules.parse(totp) } ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(config) {
        while (true) {
            now = System.currentTimeMillis()
            delay(TICK_MILLIS - now % TICK_MILLIS)
        }
    }
    val step = TotpRules.counter(now, config.period)
    val code = remember(config, step) { TotpCodes.code(config, now) }
    V5ListRow(
        title = stringResource(R.string.passwords_totp),
        subtitle = stringResource(R.string.passwords_totp_seconds_left, TotpRules.secondsLeft(now, config.period)),
        value = TotpRules.grouped(code),
        onClick = { onCopy(code) },
        onLongClick = { onCopy(code) },
        onLongClickLabel = stringResource(R.string.passwords_totp_copy),
        modifier = Modifier.testTag(PasswordsTestTags.TotpValue),
    )
}

/** A two-factor key from the site, as a v5 sheet: its `otpauth://` link or the key under its QR code. */
@Composable
private fun TotpSheet(visible: Boolean, onSave: (String) -> Boolean, onDismiss: () -> Unit) {
    var input by remember(visible) { mutableStateOf("") }
    var wrong by remember(visible) { mutableStateOf(false) }
    V5PromptSheet(
        visible = visible,
        title = stringResource(R.string.passwords_totp_add),
        message = stringResource(R.string.passwords_totp_add_body),
        actions = listOf(
            V5PromptAction(
                stringResource(R.string.passwords_save),
                { wrong = !onSave(input) },
                V5PromptRole.Primary,
                testTag = PasswordsTestTags.TotpSave,
                enabled = input.isNotBlank(),
            ),
        ),
        onDismiss = onDismiss,
    ) {
        V5NameField(
            value = input,
            placeholder = stringResource(R.string.passwords_totp_field),
            onValueChange = {
                input = it
                wrong = false
            },
            onDone = { wrong = !onSave(input) },
            maxLength = MAX_TOTP_INPUT_LENGTH,
            capitalize = false,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.testTag(PasswordsTestTags.TotpField),
        )
        if (wrong) {
            Text(
                stringResource(R.string.passwords_totp_wrong),
                style = V5Theme.type.footnote,
                color = V5Theme.colors.danger,
                modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
            )
        }
    }
}

/** «Delete passwords» / «Turn off passwords» asked as a v5 sheet before the vault is erased. */
@Composable
private fun EraseSheet(visible: Boolean, onErase: () -> Unit, onDismiss: () -> Unit) {
    V5PromptSheet(
        visible = visible,
        title = stringResource(R.string.passwords_erase_title),
        message = stringResource(R.string.passwords_erase_body),
        actions = listOf(
            V5PromptAction(
                stringResource(R.string.passwords_erase_action),
                {
                    onDismiss()
                    onErase()
                },
                V5PromptRole.Destructive,
                testTag = PasswordsTestTags.EraseConfirm,
            ),
        ),
        onDismiss = onDismiss,
    )
}

@Composable
private fun ListNotes(systemFillNote: Boolean, passkeyNote: PasskeyAvailability?) {
    val note = when {
        // System WebView fills sites through Android's autofill service, not through this vault.
        systemFillNote -> R.string.passwords_webview_note to PasswordsTestTags.SystemFillNote
        passkeyNote == PasskeyAvailability.NeedsAndroid14 -> R.string.passwords_passkeys_need_android_14 to PasswordsTestTags.PasskeyNote
        passkeyNote != null && passkeyNote != PasskeyAvailability.Available ->
            R.string.passwords_passkeys_no_credential_manager to PasswordsTestTags.PasskeyNote
        else -> null
    } ?: return
    Text(
        stringResource(note.first),
        style = V5Theme.type.footnote,
        color = V5Theme.colors.labelSecondary,
        modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4).testTag(note.second),
    )
}

@Composable
private fun EmptyNote(title: String, body: String) {
    val colors = V5Theme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4, vertical = V5Spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Text(title, style = V5Theme.type.headline, color = colors.label, textAlign = TextAlign.Center)
        Text(body, style = V5Theme.type.callout, color = colors.labelSecondary, textAlign = TextAlign.Center)
    }
}

private fun PasswordEditError.messageRes(): Int = when (this) {
    PasswordEditError.SiteInvalid -> R.string.passwords_site_invalid
    PasswordEditError.PasswordRequired -> R.string.passwords_password_required
    PasswordEditError.Duplicate -> R.string.passwords_duplicate
    PasswordEditError.Failed -> R.string.passwords_failed
}

private const val MASK = "••••••••••"
private const val MAX_QUERY_LENGTH = 200
private const val MAX_SITE_LENGTH = 512
private const val MAX_USERNAME_LENGTH = 1_024
private const val MAX_PASSWORD_LENGTH = 4_096
private const val MAX_TOTP_INPUT_LENGTH = 2_048
private const val TICK_MILLIS = 1_000L
