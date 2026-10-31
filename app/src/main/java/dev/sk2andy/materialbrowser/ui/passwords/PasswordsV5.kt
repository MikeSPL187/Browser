package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Elevation
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
import android.text.format.DateUtils
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules

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
    /** The fingerprint was accepted and the vault is opening: the button shows it. */
    unlocking: Boolean = false,
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
                    busy = unlocking,
                    busyLabel = stringResource(R.string.passwords_unlocking),
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

/** What the password check found, as the list's card shows it; [breached] is null until leaks were checked. */
@Immutable
internal data class PasswordsHealthCounts(val weak: Int, val reused: Int, val breached: Int?, val issues: Int)

/**
 * «ц5» and canvas «Passwords»: search, the password check as a card with its three numbers, then
 * every password under its site's first letter, and where the vault lives at the end.
 */
@Composable
internal fun PasswordsListV5(
    logins: List<VaultLogin>,
    onOpen: (VaultLogin) -> Unit,
    onAdd: () -> Unit,
    onLock: () -> Unit,
    onBack: () -> Unit,
    systemFillNote: Boolean,
    passkeyNote: PasskeyAvailability?,
    health: PasswordsHealthCounts,
    onHealth: () -> Unit,
    onImport: () -> Unit,
    busy: Boolean,
    onTurnOff: () -> Unit,
    /** «Export passwords» after the warning; null hides the row. */
    onExport: (() -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var erasing by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
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
                    if (logins.isNotEmpty()) HealthCard(health, onHealth)
                    V5ListGroup {
                        V5ListRow(
                            title = stringResource(R.string.passwords_import_entry),
                            icon = V5RowIcon(VolaIcons.UploadFile, V5Tile.Blue),
                            trailing = V5RowTrailing.Chevron,
                            onClick = onImport,
                            modifier = Modifier.testTag(PasswordsTestTags.Import),
                        )
                        if (onExport != null && logins.isNotEmpty()) {
                            V5ListRow(
                                title = stringResource(R.string.passwords_export_entry),
                                icon = V5RowIcon(VolaIcons.Download, V5Tile.Gray),
                                trailing = V5RowTrailing.Chevron,
                                enabled = !busy,
                                onClick = { exporting = true },
                                modifier = Modifier.testTag(PasswordsTestTags.Export),
                            )
                        }
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
                    Text(
                        stringResource(R.string.passwords_all_count, visible.size),
                        style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.labelSecondary,
                        modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                    )
                    // Under each site's first letter, as the canvas and Safari's list.
                    PasswordsRules.byLetter(visible).forEach { (letter, group) ->
                        V5ListGroup(header = letter, dividerInset = V5ListDefaults.dividerInsetText) {
                            group.forEach { login ->
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
                }
                if (query.isBlank()) {
                    // Where the passwords live (canvas «Passwords»): a KDBX file on this phone.
                    Row(
                        Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(VolaIcons.Lock, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
                        Text(stringResource(R.string.v5_passwords_vault_note), style = V5Theme.type.footnote, color = colors.labelSecondary)
                    }
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
        // As Chrome and Safari: the file is plain text, said before anything is written.
        V5PromptSheet(
            visible = exporting && !busy,
            title = stringResource(R.string.passwords_export_title),
            message = stringResource(R.string.passwords_export_body),
            actions = listOf(
                V5PromptAction(stringResource(R.string.passwords_export_action), {
                    exporting = false
                    onExport?.invoke()
                }, V5PromptRole.Primary),
            ),
            onDismiss = { exporting = false },
        )
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
    /** Opens the login's site in a tab, as Safari's and Chrome's password page link to it. */
    onOpenSite: (() -> Unit)? = null,
) {
    // Shown only while this page is on screen: never restored after the app was in the background.
    var revealed by remember(login.id) { mutableStateOf(false) }
    var addingTotp by remember { mutableStateOf(false) }
    var confirmRemoveTotp by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val site = PasswordsRules.displaySite(login.origin)
    // «Edited 12 Oct 2026» under the last group, as Safari dates a password.
    val context = LocalContext.current
    val edited = stringResource(
        R.string.passwords_edited,
        DateUtils.formatDateTime(context, login.updatedAtMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR),
    ).takeIf { login.updatedAtMillis > 0 }
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
                    V5ListRow(
                        title = stringResource(R.string.passwords_site),
                        value = login.origin,
                        trailing = if (onOpenSite != null) V5RowTrailing.Chevron else V5RowTrailing.None,
                        onClick = onOpenSite,
                        modifier = Modifier.testTag(PasswordsTestTags.OpenSite),
                    )
                }
                // Safari's and Chrome's «Notes»: the user's own words, edited with the rest of the login.
                val note = login.note
                if (note != null) {
                    V5ListGroup(header = stringResource(R.string.passwords_note), footer = edited) {
                        Text(
                            note,
                            style = V5Theme.type.body,
                            color = colors.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onEdit)
                                .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3)
                                .testTag(PasswordsTestTags.Note),
                        )
                    }
                }
                V5ListGroup(
                    dividerInset = V5ListDefaults.dividerInsetText,
                    footer = edited.takeIf { login.note == null },
                ) {
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
    onSave: (site: String, username: String, password: String, note: String) -> Unit,
    onBack: () -> Unit,
) {
    var generating by remember { mutableStateOf(false) }
    var note by remember(initial?.id) { mutableStateOf(initial?.note.orEmpty()) }
    var site by remember(initial?.id) { mutableStateOf(initial?.let { PasswordsRules.displaySite(it.origin) }.orEmpty()) }
    var username by remember(initial?.id) { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember(initial?.id) { mutableStateOf(initial?.password.orEmpty()) }
    val save = { if (!busy) onSave(site, username, password, note) }
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
                    label = stringResource(R.string.passwords_site),
                    onValueChange = { site = it },
                    onDone = {},
                    maxLength = MAX_SITE_LENGTH,
                    capitalize = false,
                    keyboardType = KeyboardType.Uri,
                )
                V5NameField(
                    value = username,
                    placeholder = stringResource(R.string.passwords_username),
                    label = stringResource(R.string.passwords_username),
                    onValueChange = { username = it },
                    onDone = {},
                    maxLength = MAX_USERNAME_LENGTH,
                    capitalize = false,
                    keyboardType = KeyboardType.Email,
                )
                V5NameField(
                    value = password,
                    placeholder = stringResource(R.string.passwords_password),
                    label = stringResource(R.string.passwords_password),
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
                V5NameField(
                    value = note,
                    placeholder = stringResource(R.string.passwords_note_hint),
                    label = stringResource(R.string.passwords_note),
                    onValueChange = { note = it },
                    onDone = {},
                    maxLength = CredentialVaultRules.MAX_NOTE_LENGTH,
                    multiline = true,
                    modifier = Modifier.testTag(PasswordsTestTags.NoteField),
                )
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

/** The password check as a card: three numbers in a row, the leaks a dash until they were checked. */
@Composable
private fun HealthCard(health: PasswordsHealthCounts, onHealth: () -> Unit) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.m
    val openLabel = stringResource(R.string.passwords_health_title)
    Column(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .v5Elevation(V5Elevation.e1, shape)
            .clip(shape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onHealth)
            .semantics(mergeDescendants = true) {}
            .padding(V5Spacing.space4)
            .testTag(PasswordsTestTags.Health),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
            Box(
                Modifier.size(V5Sizes.rowTile).clip(V5Theme.shapes.xs).background(V5Tile.Orange.color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VolaIcons.GppMaybe, contentDescription = null, tint = V5Tile.Orange.glyph, modifier = Modifier.size(V5Sizes.iconSmall))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.passwords_health_entry), style = V5Theme.type.headline, color = colors.label)
                Text(
                    if (health.issues > 0) {
                        pluralStringResource(R.plurals.passwords_health_entry_issues, health.issues, health.issues)
                    } else {
                        stringResource(R.string.passwords_health_entry_fine)
                    },
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                )
            }
            Icon(VolaIcons.KeyboardArrowRight, contentDescription = null, tint = colors.labelTertiary, modifier = Modifier.size(V5Sizes.iconSmall))
        }
        Row(Modifier.fillMaxWidth()) {
            HealthNumber(health.weak.toString(), stringResource(R.string.v5_passwords_weak), Modifier.weight(1f))
            HealthNumber(health.reused.toString(), stringResource(R.string.v5_passwords_reused), Modifier.weight(1f))
            HealthNumber(
                health.breached?.toString() ?: LEAKS_UNCHECKED,
                stringResource(R.string.v5_passwords_leaks),
                Modifier.weight(1f),
                spoken = stringResource(R.string.v5_passwords_leaks_unchecked).takeIf { health.breached == null },
            )
        }
    }
}

@Composable
private fun HealthNumber(number: String, label: String, modifier: Modifier, spoken: String? = null) {
    val colors = V5Theme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            number,
            style = V5Theme.type.title,
            color = colors.label,
            modifier = if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier,
        )
        Text(label, style = V5Theme.type.footnote, color = colors.labelSecondary, textAlign = TextAlign.Center)
    }
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
private const val LEAKS_UNCHECKED = "—"
private const val MAX_QUERY_LENGTH = 200
private const val MAX_SITE_LENGTH = 512
private const val MAX_USERNAME_LENGTH = 1_024
private const val MAX_PASSWORD_LENGTH = 4_096
private const val MAX_TOTP_INPUT_LENGTH = 2_048
private const val TICK_MILLIS = 1_000L
