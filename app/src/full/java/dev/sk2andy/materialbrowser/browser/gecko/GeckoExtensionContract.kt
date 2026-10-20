package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionActionKey
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionSessionIdentity
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtension
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionChangeListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionPermissionPrompt
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionChromeHost

/**
 * Narrow port implemented by WebExtensionController.
 *
 * installSignedXpi must call GeckoView's normal install API. It must never route untrusted input
 * through installBuiltIn: Gecko remains the authority that validates the XPI and Mozilla signature.
 * Install and update adapters translate WebExtension.InstallException codes into
 * GeckoExtensionInstallException before the error reaches manager state.
 */
internal interface GeckoExtensionRuntime {
    fun setPermissionPrompt(prompt: GeckoExtensionPermissionPrompt)

    fun setChangeListener(listener: GeckoExtensionChangeListener) = Unit

    fun setChromeHost(host: GeckoExtensionChromeHost?) = Unit

    fun attachChromeSession(
        session: org.mozilla.geckoview.GeckoSession,
        identity: GeckoExtensionSessionIdentity,
    ) = Unit

    fun detachChromeSession(session: org.mozilla.geckoview.GeckoSession) = Unit

    fun onSelectedChromeSessionChanged() = Unit

    fun clickChromeAction(key: GeckoExtensionActionKey): Boolean = false

    fun dismissChromePopup() = Unit

    suspend fun listInstalled(): List<GeckoExtension>

    suspend fun installSignedXpi(
        uri: String,
        installationMethod: String,
    ): GeckoExtension

    suspend fun enable(extensionId: String): GeckoExtension

    suspend fun disable(extensionId: String): GeckoExtension

    suspend fun update(extensionId: String): GeckoExtension

    suspend fun setAllowedInPrivateBrowsing(
        extensionId: String,
        allowed: Boolean,
    ): GeckoExtension

    suspend fun uninstall(extensionId: String)
}
