package dev.sk2andy.materialbrowser.ui

enum class SettingsDestination {
    Home,
    Search,
    TabsAndGestures,

    /** Under ui.v5: the less used half of «Tabs» (V15d-5). */
    TabsMore,
    AddressBarLongPressActions,
    AddressBarActions,
    MenuActions,
    LinkPeekActions,
    Appearance,
    Themes,
    Browser,

    /** Under ui.v5: the less used half of «Browser» (V15d-4). */
    BrowserMore,
    Downloads,
    Userscripts,
    ToppingCatalog,
    SiteCapsules,
    Sync,
    Passwords,
    ProtectionAndData,

    /** Under ui.v5: the less used half of «Protection and data» (V15d-3). */
    ProtectionMore,
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
            ProtectionMore -> ProtectionAndData
            BrowserMore -> Browser
            TabsMore -> TabsAndGestures
            else -> Home
        }
}
