package dev.sk2andy.materialbrowser.browser.credentials.vault

/**
 * Whether the browser saves and offers logins from its own vault. Off until the Passwords screen
 * lands (Q20): a vault nobody can open, browse or move logins out of would only hide passwords.
 * Until then logins keep going to the system Credential Manager, as before.
 */
internal object CredentialVaultFeature {
    const val ENABLED = false
}
