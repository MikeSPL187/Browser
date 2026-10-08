package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaPasswords
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme

internal object PasswordsTestTags {
    const val Start = "passwords_start"
    const val PhraseDone = "passwords_phrase_done"
    const val ConfirmCheck = "passwords_confirm_check"
    const val Unlock = "passwords_unlock"
    const val Recover = "passwords_recover"
    const val RecoverField = "passwords_recover_field"
    const val RecoverOpen = "passwords_recover_open"
    const val List = "passwords_list"
    const val SystemFillNote = "passwords_system_fill_note"
    const val Generate = "passwords_generate"
    const val Health = "passwords_health"
    const val Import = "passwords_import"
    const val AddTotp = "passwords_totp_add"
    const val RemoveTotp = "passwords_totp_remove"
    const val TotpField = "passwords_totp_field"
    const val TotpSave = "passwords_totp_save"
    const val TotpValue = "passwords_totp_value"
    const val Search = "passwords_search"
    const val Add = "passwords_add"
    const val Lock = "passwords_lock"
    const val Reveal = "passwords_reveal"
    const val PasswordValue = "passwords_password_value"
    const val Delete = "passwords_delete"
    const val Erase = "passwords_erase"
    const val EraseConfirm = "passwords_erase_confirm"
    const val Save = "passwords_save"

    fun confirmWord(position: Int) = "passwords_confirm_word:$position"

    fun login(id: String) = "passwords_login:$id"
}

/** The shared frame of the setup and lock screens: a soft tile with an icon, a title, a few lines, actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordsHeroScaffold(
    icon: ImageVector,
    title: String,
    body: String?,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = VolaPasswords.sectionGap)
                    .size(VolaPasswords.heroSize)
                    .background(MaterialTheme.colorScheme.primaryContainer, VolaPasswords.heroShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(VolaPasswords.heroIconSize),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(VolaPasswords.heroGap))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = VolaPasswords.textMaxWidth).semantics { heading() },
            )
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = VolaPasswords.textMaxWidth)
                        .padding(top = VolaPasswords.formGap),
                )
            }
            Spacer(Modifier.height(VolaPasswords.heroGap))
            Column(
                modifier = Modifier.widthIn(max = VolaPasswords.textMaxWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VolaPasswords.formGap),
                content = content,
            )
            Spacer(Modifier.height(VolaPasswords.heroGap))
        }
    }
}

/** First visit: what the vault is, before anything is created. */
@Composable
internal fun PasswordsIntroScreen(busy: Boolean, message: String?, onStart: () -> Unit, onBack: () -> Unit) {
    PasswordsHeroScaffold(
        icon = VolaIcons.Key,
        title = stringResource(R.string.passwords_intro_title),
        body = null,
        onBack = onBack,
    ) {
        IntroPoint(VolaIcons.Lock, stringResource(R.string.passwords_intro_encrypted))
        IntroPoint(VolaIcons.Fingerprint, stringResource(R.string.passwords_intro_unlock))
        IntroPoint(VolaIcons.Key, stringResource(R.string.passwords_intro_phrase))
        message?.let { ErrorText(it) }
        PrimaryButton(stringResource(R.string.passwords_intro_action), busy, onStart, PasswordsTestTags.Start)
    }
}

@Composable
private fun IntroPoint(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(VolaPasswords.pointIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(VolaPasswords.pointGap))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The 12 words, numbered in reading order, two to a row. */
@Composable
internal fun RecoveryPhraseScreen(words: List<String>, onDone: () -> Unit, onBack: () -> Unit) {
    PasswordsHeroScaffold(
        icon = VolaIcons.Key,
        title = stringResource(R.string.passwords_phrase_title),
        body = stringResource(R.string.passwords_phrase_body),
        onBack = onBack,
    ) {
        Surface(
            shape = VolaPasswords.phraseCardShape,
            color = VolaTheme.extendedColors.card,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(VolaPasswords.phraseCardPadding),
                verticalArrangement = Arrangement.spacedBy(VolaPasswords.phraseWordGap),
            ) {
                val rows = (words.size + VolaPasswords.PHRASE_COLUMNS - 1) / VolaPasswords.PHRASE_COLUMNS
                // Down the first column, then the second: «1–6» on the left reads like a written list.
                repeat(rows) { row ->
                    Row {
                        repeat(VolaPasswords.PHRASE_COLUMNS) { column ->
                            val index = column * rows + row
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                if (index < words.size) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(VolaPasswords.phraseNumberWidth),
                                    )
                                    Text(
                                        text = words[index],
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton(stringResource(R.string.passwords_phrase_done), busy = false, onClick = onDone, tag = PasswordsTestTags.PhraseDone)
    }
}

/** Three words typed back from the note: the phrase is really written down before it is needed. */
@Composable
internal fun RecoveryConfirmScreen(
    positions: List<Int>,
    wrong: Boolean,
    busy: Boolean,
    onCheck: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val answers = remember(positions) { mutableStateListOf(*Array(positions.size) { "" }) }
    PasswordsHeroScaffold(
        icon = VolaIcons.Key,
        title = stringResource(R.string.passwords_confirm_title),
        body = stringResource(R.string.passwords_confirm_body),
        onBack = onBack,
    ) {
        positions.forEachIndexed { index, position ->
            OutlinedTextField(
                value = answers[index],
                onValueChange = { answers[index] = it.take(MAX_WORD_LENGTH) },
                label = { Text(stringResource(R.string.passwords_confirm_word, position + 1)) },
                singleLine = true,
                isError = wrong,
                keyboardOptions = SecretWordKeyboard,
                modifier = Modifier.fillMaxWidth().testTag(PasswordsTestTags.confirmWord(position)),
            )
        }
        if (wrong) ErrorText(stringResource(R.string.passwords_confirm_wrong))
        PrimaryButton(
            stringResource(R.string.passwords_confirm_action),
            busy = busy,
            onClick = { onCheck(answers.toList()) },
            tag = PasswordsTestTags.ConfirmCheck,
        )
    }
}

/**
 * The vault is closed: open it as the device's owner, or with the phrase when the device key is
 * gone. When both are lost, [onErase] deletes it, so the phone is not stuck with a vault nobody opens.
 */
@Composable
internal fun PasswordsLockedScreen(
    busy: Boolean,
    message: String?,
    onUnlock: () -> Unit,
    onRecover: () -> Unit,
    onBack: () -> Unit,
    onErase: () -> Unit = {},
) {
    PasswordsHeroScaffold(
        icon = VolaIcons.Lock,
        title = stringResource(R.string.passwords_locked_title),
        body = stringResource(R.string.passwords_locked_body),
        onBack = onBack,
    ) {
        message?.let { ErrorText(it) }
        PrimaryButton(stringResource(R.string.passwords_unlock), busy, onUnlock, PasswordsTestTags.Unlock, VolaIcons.Fingerprint)
        TextButton(
            onClick = onRecover,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag(PasswordsTestTags.Recover),
        ) {
            Text(stringResource(R.string.passwords_recover))
        }
        EraseVaultButton(label = stringResource(R.string.passwords_erase_locked), busy = busy, onConfirm = onErase)
    }
}

/**
 * «Delete passwords» / «Turn off passwords»: erases the vault, its index and its keys after a
 * confirmation; afterwards sites go back to the system password manager.
 */
@Composable
internal fun EraseVaultButton(label: String, busy: Boolean, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    var confirming by remember { mutableStateOf(false) }
    TextButton(
        onClick = { confirming = true },
        enabled = !busy,
        modifier = modifier.fillMaxWidth().testTag(PasswordsTestTags.Erase),
    ) {
        Text(label, color = MaterialTheme.colorScheme.error)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.passwords_erase_title)) },
            text = { Text(stringResource(R.string.passwords_erase_body)) },
            confirmButton = {
                TextButton(
                    onClick = { confirming = false; onConfirm() },
                    modifier = Modifier.testTag(PasswordsTestTags.EraseConfirm),
                ) {
                    Text(stringResource(R.string.passwords_erase_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
internal fun RecoveryEntryScreen(wrong: Boolean, busy: Boolean, onOpen: (String) -> Unit, onBack: () -> Unit) {
    var phrase by remember { mutableStateOf("") }
    PasswordsHeroScaffold(
        icon = VolaIcons.Key,
        title = stringResource(R.string.passwords_recover_title),
        body = null,
        onBack = onBack,
    ) {
        OutlinedTextField(
            value = phrase,
            onValueChange = { phrase = it.take(MAX_PHRASE_LENGTH) },
            label = { Text(stringResource(R.string.passwords_recover_hint)) },
            isError = wrong,
            minLines = 3,
            keyboardOptions = SecretWordKeyboard,
            modifier = Modifier.fillMaxWidth().testTag(PasswordsTestTags.RecoverField),
        )
        if (wrong) ErrorText(stringResource(R.string.passwords_recover_wrong))
        PrimaryButton(stringResource(R.string.passwords_recover_action), busy, { onOpen(phrase) }, PasswordsTestTags.RecoverOpen)
    }
}

@Composable
internal fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun PrimaryButton(
    text: String,
    busy: Boolean,
    onClick: () -> Unit,
    tag: String,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = VolaPasswords.buttonHeight).testTag(tag),
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(VolaPasswords.pointIconSize))
        } else {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(VolaPasswords.pointIconSize))
                Spacer(Modifier.width(VolaPasswords.buttonIconGap))
            }
            Text(text)
        }
    }
}

/** Phrase words are secrets: no suggestions or learning by the keyboard, no capitals. */
private val SecretWordKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Password,
    imeAction = ImeAction.Done,
)

private const val MAX_WORD_LENGTH = 16
private const val MAX_PHRASE_LENGTH = 256
