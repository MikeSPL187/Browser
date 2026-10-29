package dev.sk2andy.materialbrowser.ui.passwords

import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin

// The Passwords window is secure, so the screenshot tour cannot see it: these previews are its pictures.

private val previewLogins = listOf(
    previewLogin("1", "https://mail.example.com", "anna@example.com"),
    previewLogin("2", "https://bank.example.ru", "anna.k"),
    previewLogin("3", "https://tasks.example.com", "anna@work.example"),
    previewLogin("4", "https://forum.example.org", "snowfox"),
    previewLogin("5", "https://cloud.example.net", ""),
)

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
