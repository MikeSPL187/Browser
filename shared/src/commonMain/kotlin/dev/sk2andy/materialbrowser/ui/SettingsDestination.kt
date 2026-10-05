package dev.sk2andy.materialbrowser.ui

enum class SettingsDestination {
    Home,
    Search,
    TabsAndGestures,
    AddressBarLongPressActions,
    AddressBarActions,
    MenuActions,
    LinkPeekActions,
    Appearance,
    Themes,
    Browser,
    Downloads,
    Userscripts,
    ToppingCatalog,
    SiteCapsules,
    Sync,
    Passwords,
    ProtectionAndData,
    DeveloperOptions,
    AboutLegal,
    ;

    /** The page that back returns to: the page a subpage opens from, the home for the rest. */
    val parent: SettingsDestination
        get() = when (this) {
            AddressBarLongPressActions,
            AddressBarActions,
            MenuActions,
            LinkPeekActions,
            -> TabsAndGestures
            Themes -> Appearance
            ToppingCatalog -> Userscripts
            else -> Home
        }
}
