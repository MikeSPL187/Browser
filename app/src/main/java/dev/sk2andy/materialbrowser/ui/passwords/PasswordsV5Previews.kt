package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin

/* The password manager under v5 (canvas «ц4», «Passwords»): locked, the list, a login, editing. Preview text is not localized. */

private fun previewLogin(id: String, site: String, user: String, totp: String? = null) = VaultLogin(
    id = id,
    origin = "https://$site",
    formActionOrigin = null,
    httpRealm = null,
    username = user,
    password = "correct-horse-battery",
    createdAtMillis = 0L,
    updatedAtMillis = 0L,
    lastUsedAtMillis = null,
    timesUsed = 3,
    totp = totp,
)

private val previewLogins = listOf(
    previewLogin("1", "north-guide.example", "anna@example.com", totp = "otpauth://totp/north?secret=JBSWY3DPEHPK3PXP"),
    previewLogin("2", "mail.example", "anna"),
    previewLogin("3", "shop.example", "anna.k"),
    previewLogin("4", "aurora-tours.example", "anna"),
)

@Composable
private fun PreviewScheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
}

@Preview(name = "Passwords v5 · locked", heightDp = 640)
@Composable
private fun PasswordsLockedV5Preview() = PreviewScheme(dark = false) {
    PasswordsLockedV5(busy = false, message = null, onUnlock = {}, onRecover = {}, onBack = {}, onErase = {})
}

@Preview(name = "Passwords v5 · list, light", heightDp = 720)
@Composable
private fun PasswordsListV5LightPreview() = PasswordsListPreview(dark = false)

@Preview(name = "Passwords v5 · list, dark", heightDp = 720)
@Composable
private fun PasswordsListV5DarkPreview() = PasswordsListPreview(dark = true)

@Preview(name = "Passwords v5 · list, largest font", heightDp = 1400, fontScale = 2f)
@Composable
private fun PasswordsListV5LargeFontPreview() = PasswordsListPreview(dark = false)

@Composable
private fun PasswordsListPreview(dark: Boolean) = PreviewScheme(dark) {
    PasswordsListV5(
        logins = previewLogins,
        onOpen = {},
        onAdd = {},
        onLock = {},
        onBack = {},
        systemFillNote = false,
        passkeyNote = null,
        health = PasswordsHealthCounts(weak = 2, reused = 1, breached = null, issues = 3),
        onHealth = {},
        onImport = {},
        busy = false,
        onTurnOff = {},
    )
}

@Preview(name = "Passwords v5 · login, dark", heightDp = 720)
@Composable
private fun PasswordDetailV5DarkPreview() = PreviewScheme(dark = true) {
    PasswordDetailV5(
        login = previewLogins.first(),
        onCopyUsername = {},
        onCopyPassword = {},
        onEdit = {},
        onDelete = {},
        onBack = {},
        onCopyCode = {},
        onSetTotp = { true },
        onRemoveTotp = {},
    )
}

@Preview(name = "Passwords v5 · edit", heightDp = 640)
@Composable
private fun PasswordEditV5Preview() = PreviewScheme(dark = false) {
    PasswordEditV5(initial = previewLogins[1], error = null, busy = false, onSave = { _, _, _ -> }, onBack = {})
}

@Preview(name = "Passwords v5 · first visit", heightDp = 720)
@Composable
private fun PasswordsIntroV5Preview() = PreviewScheme(dark = false) {
    PasswordsIntroV5(busy = false, message = null, onStart = {}, onBack = {})
}

@Preview(name = "Passwords v5 · recovery phrase, dark", heightDp = 720)
@Composable
private fun RecoveryPhraseV5DarkPreview() = PreviewScheme(dark = true) {
    RecoveryPhraseV5(
        words = "acid acorn acre acts afar affix aged agent agile aging agony ahead".split(' '),
        onDone = {},
        onBack = {},
    )
}

@Preview(name = "Passwords v5 · confirm, wrong", heightDp = 720)
@Composable
private fun RecoveryConfirmV5Preview() = PreviewScheme(dark = false) {
    RecoveryConfirmV5(positions = listOf(2, 6, 10), wrong = true, busy = false, onCheck = {}, onBack = {})
}

@Preview(name = "Passwords v5 · recover", heightDp = 640)
@Composable
private fun RecoveryEntryV5Preview() = PreviewScheme(dark = false) {
    RecoveryEntryV5(wrong = false, busy = false, onOpen = {}, onBack = {})
}
