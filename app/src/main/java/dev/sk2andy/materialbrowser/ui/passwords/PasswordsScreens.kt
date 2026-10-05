package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.LibraryCardSlice
import dev.sk2andy.materialbrowser.ui.LibraryRow
import dev.sk2andy.materialbrowser.ui.LibraryRowPosition
import dev.sk2andy.materialbrowser.ui.LibraryRules
import dev.sk2andy.materialbrowser.ui.LibrarySearchBar
import dev.sk2andy.materialbrowser.ui.LibrarySectionLabel
import dev.sk2andy.materialbrowser.ui.LibrarySiteTile
import dev.sk2andy.materialbrowser.ui.VolaStateMessage
import dev.sk2andy.materialbrowser.ui.VolaStateTone
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary
import dev.sk2andy.materialbrowser.ui.theme.VolaPasswords
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource

/** Why a hand-made or edited login was not saved. */
internal enum class PasswordEditError { SiteInvalid, PasswordRequired, Duplicate, Failed }

/** Board W-Passwords: search, then every login as a row with its site's letter and the user name. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordsListScreen(
    logins: List<VaultLogin>,
    onOpen: (VaultLogin) -> Unit,
    onAdd: () -> Unit,
    onLock: () -> Unit,
    onBack: () -> Unit,
    systemFillNote: Boolean = false,
    healthIssues: Int? = null,
    onHealth: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(logins, query) { PasswordsRules.filter(logins, query) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.passwords_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onLock, modifier = Modifier.testTag(PasswordsTestTags.Lock)) {
                        Icon(VolaIcons.Lock, contentDescription = stringResource(R.string.passwords_lock))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, modifier = Modifier.testTag(PasswordsTestTags.Add)) {
                Icon(VolaIcons.Add, contentDescription = stringResource(R.string.passwords_add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().testTag(PasswordsTestTags.List),
            contentPadding = PaddingValues(bottom = VolaLibrary.fabClearance),
        ) {
            item(key = "search") {
                LibrarySearchBar(
                    query = query,
                    placeholder = stringResource(R.string.passwords_search),
                    clearContentDescription = stringResource(R.string.passwords_clear_search),
                    testTag = PasswordsTestTags.Search,
                    onQueryChange = { query = it.take(MAX_QUERY_LENGTH) },
                )
            }
            if (healthIssues != null && logins.isNotEmpty()) {
                item(key = "health") {
                    LibraryCardSlice(
                        position = LibraryRules.position(0, 1),
                        modifier = Modifier.padding(top = VolaLibrary.sectionGap),
                    ) {
                        LibraryRow(
                            title = stringResource(R.string.passwords_health_entry),
                            detail = if (healthIssues > 0) {
                                pluralStringResource(R.plurals.passwords_health_entry_issues, healthIssues, healthIssues)
                            } else {
                                stringResource(R.string.passwords_health_entry_fine)
                            },
                            detailColor = if (healthIssues > 0) MaterialTheme.colorScheme.error else null,
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(VolaLibrary.tileSize)
                                        .clip(VolaLibrary.tileShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        VolaIcons.GppMaybe,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            },
                            onClick = onHealth,
                            modifier = Modifier.testTag(PasswordsTestTags.Health),
                        )
                    }
                }
            }
            if (systemFillNote) {
                // System WebView fills sites through Android's autofill service, not through this vault.
                item(key = "system-fill-note") {
                    Text(
                        text = stringResource(R.string.passwords_webview_note),
                        modifier = Modifier
                            .padding(horizontal = VolaLibrary.sidePadding)
                            .padding(top = VolaLibrary.sectionGap)
                            .testTag(PasswordsTestTags.SystemFillNote),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (visible.isEmpty()) {
                item(key = "empty") {
                    val searching = query.isNotBlank()
                    VolaStateMessage(
                        icon = rememberVectorPainter(VolaIcons.Key),
                        title = stringResource(if (searching) R.string.passwords_no_results else R.string.passwords_empty_title),
                        message = stringResource(if (searching) R.string.passwords_no_results_body else R.string.passwords_empty_body),
                        tone = if (searching) VolaStateTone.Neutral else VolaStateTone.Empty,
                        modifier = Modifier.padding(horizontal = VolaLibrary.sidePadding).padding(top = VolaLibrary.sectionGap),
                    )
                }
            } else {
                item(key = "count") {
                    LibrarySectionLabel(
                        text = stringResource(R.string.passwords_all_count, visible.size),
                        modifier = Modifier.padding(top = VolaLibrary.sectionGap),
                    )
                }
                itemsIndexed(visible, key = { _, login -> login.id }) { index, login ->
                    LibraryCardSlice(position = LibraryRules.position(index, visible.size)) {
                        val site = PasswordsRules.displaySite(login.origin)
                        LibraryRow(
                            title = site,
                            detail = login.username.ifEmpty { stringResource(R.string.passwords_no_username) },
                            leading = { LibrarySiteTile(label = site, colorKey = login.origin) },
                            onClick = { onOpen(login) },
                            modifier = Modifier.testTag(PasswordsTestTags.login(login.id)),
                        )
                    }
                }
            }
        }
    }
}

/** Board W-PasswordDetail: one login; the password shows only on request and copies to a self-clearing clipboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordDetailScreen(
    login: VaultLogin,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    // Shown only while this page is on screen: never restored after the app was in the background.
    var revealed by remember(login.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val site = PasswordsRules.displaySite(login.origin)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(VolaIcons.Edit, contentDescription = stringResource(R.string.passwords_edit))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VolaPasswords.sidePadding),
            verticalArrangement = Arrangement.spacedBy(VolaPasswords.sectionGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LibrarySiteTile(label = site, colorKey = login.origin)
                Column(modifier = Modifier.padding(start = VolaPasswords.detailHeaderGap)) {
                    Text(
                        text = site,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            Column {
                LibraryCardSlice(position = LibraryRowPosition.First) {
                    PasswordField(
                        label = stringResource(R.string.passwords_username),
                        value = login.username.ifEmpty { stringResource(R.string.passwords_no_username) },
                    ) {
                        if (login.username.isNotEmpty()) {
                            IconButton(onClick = onCopyUsername) {
                                Icon(painterResource(R.drawable.ic_content_copy), contentDescription = stringResource(R.string.passwords_copy_username))
                            }
                        }
                    }
                }
                LibraryCardSlice(position = LibraryRowPosition.Middle) {
                    PasswordField(
                        label = stringResource(R.string.passwords_password),
                        value = if (revealed) login.password else MASK,
                        monospace = true,
                        valueTag = PasswordsTestTags.PasswordValue,
                    ) {
                        IconButton(onClick = { revealed = !revealed }, modifier = Modifier.testTag(PasswordsTestTags.Reveal)) {
                            Icon(
                                if (revealed) VolaIcons.VisibilityOff else VolaIcons.Visibility,
                                contentDescription = stringResource(if (revealed) R.string.passwords_hide else R.string.passwords_show),
                            )
                        }
                        IconButton(onClick = onCopyPassword) {
                            Icon(painterResource(R.drawable.ic_content_copy), contentDescription = stringResource(R.string.passwords_copy_password))
                        }
                    }
                }
                LibraryCardSlice(position = LibraryRowPosition.Last) {
                    PasswordField(label = stringResource(R.string.passwords_site), value = login.origin)
                }
            }
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().testTag(PasswordsTestTags.Delete),
            ) {
                Icon(VolaIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = stringResource(R.string.passwords_delete),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = VolaPasswords.buttonIconGap),
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.passwords_delete_title)) },
            text = { Text(stringResource(R.string.passwords_delete_body, site)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.passwords_delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    monospace: Boolean = false,
    valueTag: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(VolaPasswords.fieldPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VolaPasswords.fieldLabelGap)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = if (monospace) FontFamily.Monospace else null,
                modifier = valueTag?.let { Modifier.testTag(it) } ?: Modifier,
            )
        }
        actions()
    }
}

/** Adds a login by hand, or edits one: site, user name and password. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordEditScreen(
    initial: VaultLogin?,
    error: PasswordEditError?,
    busy: Boolean,
    onSave: (site: String, username: String, password: String) -> Unit,
    onBack: () -> Unit,
) {
    var site by remember(initial?.id) { mutableStateOf(initial?.let { PasswordsRules.displaySite(it.origin) }.orEmpty()) }
    var username by remember(initial?.id) { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember(initial?.id) { mutableStateOf(initial?.password.orEmpty()) }
    var visible by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }
    if (generating) {
        GeneratorSheet(
            onUse = { generated ->
                password = generated
                visible = true
                generating = false
            },
            onDismiss = { generating = false },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (initial == null) R.string.passwords_add else R.string.passwords_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VolaPasswords.sidePadding),
            verticalArrangement = Arrangement.spacedBy(VolaPasswords.formGap),
        ) {
            OutlinedTextField(
                value = site,
                onValueChange = { site = it.take(MAX_SITE_LENGTH) },
                label = { Text(stringResource(R.string.passwords_site)) },
                placeholder = { Text(stringResource(R.string.passwords_site_hint)) },
                singleLine = true,
                isError = error == PasswordEditError.SiteInvalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(MAX_USERNAME_LENGTH) },
                label = { Text(stringResource(R.string.passwords_username)) },
                singleLine = true,
                isError = error == PasswordEditError.Duplicate,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(MAX_PASSWORD_LENGTH) },
                label = { Text(stringResource(R.string.passwords_password)) },
                singleLine = true,
                isError = error == PasswordEditError.PasswordRequired,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = { generating = true },
                            modifier = Modifier.testTag(PasswordsTestTags.Generate),
                        ) {
                            Icon(VolaIcons.Refresh, contentDescription = stringResource(R.string.passwords_generator_open))
                        }
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) VolaIcons.VisibilityOff else VolaIcons.Visibility,
                                contentDescription = stringResource(if (visible) R.string.passwords_hide else R.string.passwords_show),
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { ErrorText(stringResource(it.message)) }
            PrimaryButton(
                stringResource(R.string.passwords_save),
                busy = busy,
                onClick = { onSave(site, username, password) },
                tag = PasswordsTestTags.Save,
            )
        }
    }
}

/** The generator over the edit screen; «Use» puts the password in the field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratorSheet(onUse: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        PasswordGeneratorContent(
            subtitle = null,
            useLabel = stringResource(R.string.passwords_generator_use),
            useNeedsFingerprint = false,
            onUse = onUse,
        )
    }
}

private val PasswordEditError.message: Int
    get() = when (this) {
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
