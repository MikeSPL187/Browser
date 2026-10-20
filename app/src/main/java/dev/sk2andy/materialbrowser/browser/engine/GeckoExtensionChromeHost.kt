package dev.sk2andy.materialbrowser.browser.engine

internal interface GeckoExtensionChromeHost {
    fun currentSessionIdentity(): GeckoExtensionSessionIdentity?

    fun isCurrentSession(identity: GeckoExtensionSessionIdentity): Boolean

    /**
     * Creates Candy tab state around GeckoView's required unopened tabs.create session.
     *
     * Hosts must adopt [session] before returning so Candy and Gecko reference the same renderer.
     */
    fun createTab(
        request: GeckoExtensionCreateTabRequest,
        session: BrowserEnginePreparedSession,
    ): String?

    fun updateTab(request: GeckoExtensionUpdateTabRequest): Boolean

    fun closeTab(target: GeckoExtensionSessionIdentity): Boolean

    fun openPopup(
        popup: GeckoExtensionPopupIdentity,
        session: BrowserEnginePreparedSession,
        toggle: Boolean,
    ): Boolean

    fun closePopup(popup: GeckoExtensionPopupIdentity)

    fun openOptionsPage(
        extensionId: String,
        title: String,
        owner: GeckoExtensionSessionIdentity,
        url: String,
        openInTab: Boolean,
    ): String?

    fun onActionsChanged(actions: List<GeckoExtensionActionState>)

}
