package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

// The Passwords window is secure, so the screenshot tour cannot see it: these previews are its pictures.

private val previewLogins = listOf(
    previewLogin("1", "https://mail.example.com", "anna@example.com"),
    previewLogin("2", "https://bank.example.ru", "anna.k"),
    previewLogin("3", "https://tasks.example.com", "anna@work.example"),
    previewLogin("4", "https://forum.example.org", "snowfox"),
    previewLogin("5", "https://cloud.example.net", ""),
)

/** Board W-Passwords. */
@VolaPreviews
@Composable
private fun PasswordsListPreview() {
    MaterialBrowserTheme {
        PasswordsListScreen(logins = previewLogins, onOpen = {}, onAdd = {}, onLock = {}, onBack = {})
    }
}

@VolaPreviews
@Composable
private fun PasswordsEmptyPreview() {
    MaterialBrowserTheme {
        PasswordsListScreen(logins = emptyList(), onOpen = {}, onAdd = {}, onLock = {}, onBack = {}, systemFillNote = true)
    }
}

/** Board W-PasswordDetail. */
@VolaPreviews
@Composable
private fun PasswordDetailPreview() {
    MaterialBrowserTheme {
        PasswordDetailScreen(
            login = previewLogins.first(),
            onCopyUsername = {},
            onCopyPassword = {},
            onEdit = {},
            onDelete = {},
            onBack = {},
        )
    }
}

@VolaPreviews
@Composable
private fun PasswordEditPreview() {
    MaterialBrowserTheme {
        PasswordEditScreen(
            initial = null,
            error = PasswordEditError.SiteInvalid,
            busy = false,
            onSave = { _, _, _ -> },
            onBack = {},
        )
    }
}

@VolaPreviews
@Composable
private fun PasswordsIntroPreview() {
    MaterialBrowserTheme {
        PasswordsIntroScreen(busy = false, message = null, onStart = {}, onBack = {})
    }
}

@VolaPreviews
@Composable
private fun RecoveryPhrasePreview() {
    MaterialBrowserTheme {
        RecoveryPhraseScreen(
            words = "acid acorn acre acts afar affix aged agent agile aging agony ahead".split(' '),
            onDone = {},
            onBack = {},
        )
    }
}

@VolaPreviews
@Composable
private fun RecoveryConfirmPreview() {
    MaterialBrowserTheme {
        RecoveryConfirmScreen(positions = listOf(1, 6, 10), wrong = true, busy = false, onCheck = {}, onBack = {})
    }
}

/** Board W-Locked, for passwords. */
@VolaPreviews
@Composable
private fun PasswordsLockedPreview() {
    MaterialBrowserTheme {
        PasswordsLockedScreen(busy = false, message = null, onUnlock = {}, onRecover = {}, onBack = {})
    }
}

private fun previewLogin(id: String, origin: String, username: String) = VaultLogin(
    id = id,
    origin = origin,
    formActionOrigin = null,
    httpRealm = null,
    username = username,
    password = "correct horse battery staple",
    createdAtMillis = 0,
    updatedAtMillis = 0,
    lastUsedAtMillis = null,
    timesUsed = 0,
)
