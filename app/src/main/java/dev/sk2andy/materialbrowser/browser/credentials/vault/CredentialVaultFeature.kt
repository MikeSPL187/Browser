package dev.sk2andy.materialbrowser.browser.credentials.vault

/**
 * Whether GeckoView keeps logins in the browser's own vault (Q20b). On: Gecko's login storage is the
 * vault and its silent autofill is off. Until someone sets up Passwords, the vault is empty and the
 * prompts still go to the system Credential Manager ([dev.sk2andy.materialbrowser.browser
 * .credentials.VaultCredentialPromptHost]), so nothing changes for them.
 */
internal object CredentialVaultFeature {
    const val ENABLED = true
}
