package dev.sk2andy.materialbrowser.ui

/** Which part of «Protection and data» a page shows (V15d-3). */
internal enum class ProtectionSettingsLayer {
    /** The current interface: everything on one page. */
    All,

    /** Under ui.v5: what most people set — protection, private tabs, history, clearing data. */
    Main,

    /** Under ui.v5, behind «More»: network privacy, tools, page memory and the app's data. */
    More,
    ;

    val main: Boolean get() = this != More
    val more: Boolean get() = this != Main
}

internal object ProtectionSettingsRules {
    /** One layer per screen under ui.v5 (system-v5.md section 10); the current interface keeps one page. */
    fun layer(uiV5: Boolean, destination: SettingsDestination): ProtectionSettingsLayer = when {
        !uiV5 -> ProtectionSettingsLayer.All
        destination == SettingsDestination.ProtectionMore -> ProtectionSettingsLayer.More
        else -> ProtectionSettingsLayer.Main
    }
}
