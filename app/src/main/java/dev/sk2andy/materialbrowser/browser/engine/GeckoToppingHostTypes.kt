package dev.sk2andy.materialbrowser.browser.engine



internal interface GeckoToppingInteractionDelegate {
    fun onMenuCommandsChanged(
        tabId: String,
        commands: List<dev.sk2andy.materialbrowser.browser.userscript.UserScriptMenuCommand>,
    )

    fun onOpenTab(request: dev.sk2andy.materialbrowser.browser.userscript.UserScriptOpenTabRequest)

    companion object {
        val None = object : GeckoToppingInteractionDelegate {
            override fun onMenuCommandsChanged(
                tabId: String,
                commands: List<dev.sk2andy.materialbrowser.browser.userscript.UserScriptMenuCommand>,
            ) = Unit

            override fun onOpenTab(
                request: dev.sk2andy.materialbrowser.browser.userscript.UserScriptOpenTabRequest,
            ) = Unit
        }
    }
}

internal enum class GeckoToppingHostState {
    Initializing,
    Ready,
    Unavailable,
}
