package dev.sk2andy.materialbrowser.shared.browser

/**
 * Platform-neutral identity and ordering for the browser menu.
 *
 * The first view stays short (S4a): a toolbar, the page tiles ending in «More», then the library.
 * «More» opens the rest of the tab and Vola actions, plus user-script commands, in the same
 * sheet. Per-site switches live in the site information sheet, not here.
 *
 * Android renders these ordered sections with the commonMain menu composable. iOS projects the
 * same state and actions into its native Liquid Glass menu so the Apple chrome can morph without
 * duplicating browser behavior. Firefox extensions are the only Android-only addition.
 */
enum class BrowserFeatureMenuSection {
    Toolbar,
    Page,
    Toppings,
    More,
    Browser,
}

enum class BrowserFeatureMenuItemKind {
    Command,
    Toggle,
    Navigation,
}

enum class BrowserFeatureMenuAction {
    Back,
    Forward,
    Reload,
    Stop,
    ToggleFavorite,
    TogglePinned,
    ShowTabs,
    NewTab,
    DuplicateTab,
    CloseTab,
    ParkAddressBarRight,
    OpenReader,
    TranslatePage,
    ToggleSplitView,
    FindInPage,
    Share,
    OpenExternal,
    Print,
    ToggleDesktopView,
    ToggleCompactMode,
    ToggleDomainMute,
    OpenCandyTrail,
    AddSiteCapsule,
    Summarize,
    SnoozeTab,
    DockAddressBar,
    OpenSnoozedTabs,
    OpenFavorites,
    OpenDownloads,
    OpenHistory,
    OpenSettings,
    OpenFirefoxExtensions,
    OpenPasswords,
    OpenMore,
    InvokeToppingCommand,
}

enum class BrowserFeatureMenuLabelKey {
    Back,
    Forward,
    Reload,
    StopLoading,
    AddFavorite,
    RemoveFavorite,
    PinTab,
    UnpinTab,
    Tabs,
    NewTab,
    DuplicateTab,
    CloseTab,
    ParkAddressBarRight,
    Reader,
    Translate,
    SplitView,
    FindInPage,
    Share,
    OpenExternal,
    Print,
    DesktopView,
    CompactMode,
    MuteDomain,
    UnmuteDomain,
    CandyTrail,
    AddSiteCapsule,
    Summarize,
    SnoozeTab,
    DockAddressBar,
    SnoozedTabs,
    Favorites,
    Downloads,
    History,
    Settings,
    FirefoxExtensions,
    Passwords,
    More,
    ToppingsCommand,
}

data class BrowserFeatureMenuItem(
    val stableId: String,
    val action: BrowserFeatureMenuAction,
    val labelKey: BrowserFeatureMenuLabelKey,
    val section: BrowserFeatureMenuSection,
    val kind: BrowserFeatureMenuItemKind,
    val enabled: Boolean,
    val checked: Boolean? = null,
    val dynamicLabel: String? = null,
    val supportingText: String? = null,
    val toppingScriptId: String? = null,
    val toppingCommandId: String? = null,
    val toppingDocumentId: String? = null,
)

data class BrowserToppingMenuCommand(
    val scriptId: String,
    val commandId: String,
    val caption: String,
    val scriptName: String,
    val documentId: String = "",
)

data class BrowserFeatureMenuState(
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val hasPage: Boolean = false,
    val canCloseTab: Boolean = true,
    val canToggleFavorite: Boolean = false,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val canOpenReader: Boolean = false,
    val canTranslatePage: Boolean = false,
    /** Split View open or not; null when the menu cannot switch it. */
    val isSplitView: Boolean? = null,
    val canUseDocumentActions: Boolean = false,
    val canToggleDesktopView: Boolean = false,
    /** Compact Mode on or off; null when the menu cannot switch it. */
    val isCompactMode: Boolean? = null,
    val isDesktopView: Boolean = false,
    val canToggleDomainMute: Boolean = false,
    val isDomainMuted: Boolean = false,
    val canAddSiteCapsule: Boolean = false,
    val canSnooze: Boolean = false,
    val canDockAddressBar: Boolean = true,
    val overflowPageActions: List<BrowserFeatureMenuAction> = emptyList(),
    val toppingCommands: List<BrowserToppingMenuCommand> = emptyList(),
)

data class BrowserFeatureMenuCapabilities(
    val supportsFirefoxExtensions: Boolean = false,
)

object BrowserFeatureMenuRules {
    fun items(
        state: BrowserFeatureMenuState,
        capabilities: BrowserFeatureMenuCapabilities = BrowserFeatureMenuCapabilities(),
    ): List<BrowserFeatureMenuItem> = buildList {
        toolbarItems(state).forEach(::add)
        pageItems(state).forEach(::add)
        toppingItems(state).forEach(::add)
        moreItems(state).forEach(::add)
        browserItems(capabilities).forEach(::add)
    }

    private fun toolbarItems(state: BrowserFeatureMenuState) = listOf(
        command(
            BrowserFeatureMenuAction.Back,
            BrowserFeatureMenuLabelKey.Back,
            BrowserFeatureMenuSection.Toolbar,
            state.canGoBack,
        ),
        command(
            BrowserFeatureMenuAction.Forward,
            BrowserFeatureMenuLabelKey.Forward,
            BrowserFeatureMenuSection.Toolbar,
            state.canGoForward,
        ),
        command(
            if (state.isLoading) BrowserFeatureMenuAction.Stop else BrowserFeatureMenuAction.Reload,
            if (state.isLoading) {
                BrowserFeatureMenuLabelKey.StopLoading
            } else {
                BrowserFeatureMenuLabelKey.Reload
            },
            BrowserFeatureMenuSection.Toolbar,
            state.isLoading || state.hasPage,
        ),
        command(
            BrowserFeatureMenuAction.ToggleFavorite,
            if (state.isFavorite) {
                BrowserFeatureMenuLabelKey.RemoveFavorite
            } else {
                BrowserFeatureMenuLabelKey.AddFavorite
            },
            BrowserFeatureMenuSection.Toolbar,
            state.canToggleFavorite,
            checked = state.isFavorite,
        ),
        command(
            BrowserFeatureMenuAction.Share,
            BrowserFeatureMenuLabelKey.Share,
            BrowserFeatureMenuSection.Toolbar,
            state.hasPage,
        ),
    )

    /** The tiles: what a page is opened for, ending in «More». */
    private fun pageItems(state: BrowserFeatureMenuState) = buildList {
        if (BrowserFeatureMenuAction.ShowTabs in state.overflowPageActions) {
            add(command(BrowserFeatureMenuAction.ShowTabs, BrowserFeatureMenuLabelKey.Tabs))
        }
        add(command(BrowserFeatureMenuAction.NewTab, BrowserFeatureMenuLabelKey.NewTab))
        add(
            command(
                BrowserFeatureMenuAction.FindInPage,
                BrowserFeatureMenuLabelKey.FindInPage,
                enabled = state.canUseDocumentActions,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.OpenReader,
                BrowserFeatureMenuLabelKey.Reader,
                enabled = state.canOpenReader,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.TranslatePage,
                BrowserFeatureMenuLabelKey.Translate,
                enabled = state.canTranslatePage,
            ),
        )
        state.isSplitView?.let { split ->
            add(
                toggle(
                    BrowserFeatureMenuAction.ToggleSplitView,
                    BrowserFeatureMenuLabelKey.SplitView,
                    true,
                    split,
                ),
            )
        }
        state.isCompactMode?.let { compact ->
            add(
                toggle(
                    BrowserFeatureMenuAction.ToggleCompactMode,
                    BrowserFeatureMenuLabelKey.CompactMode,
                    true,
                    compact,
                ),
            )
        }
        add(
            toggle(
                BrowserFeatureMenuAction.ToggleDesktopView,
                BrowserFeatureMenuLabelKey.DesktopView,
                state.canToggleDesktopView,
                state.isDesktopView,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.Print,
                BrowserFeatureMenuLabelKey.Print,
                enabled = state.canUseDocumentActions,
            ),
        )
        add(command(BrowserFeatureMenuAction.OpenMore, BrowserFeatureMenuLabelKey.More))
    }

    private fun toppingItems(state: BrowserFeatureMenuState) = state.toppingCommands.map { command ->
        BrowserFeatureMenuItem(
            stableId = buildString {
                append("topping:${command.scriptId}:${command.commandId}")
                if (command.documentId.isNotEmpty()) append(":${command.documentId}")
            },
            action = BrowserFeatureMenuAction.InvokeToppingCommand,
            labelKey = BrowserFeatureMenuLabelKey.ToppingsCommand,
            section = BrowserFeatureMenuSection.Toppings,
            kind = BrowserFeatureMenuItemKind.Command,
            enabled = true,
            dynamicLabel = command.caption,
            supportingText = command.scriptName,
            toppingScriptId = command.scriptId,
            toppingCommandId = command.commandId,
            toppingDocumentId = command.documentId,
        )
    }

    /** The «More» view: the rest of the tab and Vola actions. */
    private fun moreItems(state: BrowserFeatureMenuState) = buildList {
        val more = BrowserFeatureMenuSection.More
        add(
            command(
                BrowserFeatureMenuAction.DuplicateTab,
                BrowserFeatureMenuLabelKey.DuplicateTab,
                more,
                state.hasPage,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.TogglePinned,
                if (state.isPinned) BrowserFeatureMenuLabelKey.UnpinTab else BrowserFeatureMenuLabelKey.PinTab,
                more,
                checked = state.isPinned,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.CloseTab,
                BrowserFeatureMenuLabelKey.CloseTab,
                more,
                state.canCloseTab,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.OpenExternal,
                BrowserFeatureMenuLabelKey.OpenExternal,
                more,
                state.hasPage,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.SnoozeTab,
                BrowserFeatureMenuLabelKey.SnoozeTab,
                more,
                state.canSnooze,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.OpenCandyTrail,
                BrowserFeatureMenuLabelKey.CandyTrail,
                more,
                state.hasPage,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.AddSiteCapsule,
                BrowserFeatureMenuLabelKey.AddSiteCapsule,
                more,
                state.canAddSiteCapsule,
            ),
        )
        add(
            command(
                BrowserFeatureMenuAction.Summarize,
                BrowserFeatureMenuLabelKey.Summarize,
                more,
                state.hasPage,
            ),
        )
        add(
            toggle(
                BrowserFeatureMenuAction.ToggleDomainMute,
                if (state.isDomainMuted) {
                    BrowserFeatureMenuLabelKey.UnmuteDomain
                } else {
                    BrowserFeatureMenuLabelKey.MuteDomain
                },
                state.canToggleDomainMute,
                state.isDomainMuted,
                more,
            ),
        )
        if (BrowserFeatureMenuAction.ParkAddressBarRight in state.overflowPageActions) {
            add(
                command(
                    BrowserFeatureMenuAction.ParkAddressBarRight,
                    BrowserFeatureMenuLabelKey.ParkAddressBarRight,
                    more,
                    state.canDockAddressBar,
                ),
            )
        } else if (state.canDockAddressBar) {
            add(command(BrowserFeatureMenuAction.DockAddressBar, BrowserFeatureMenuLabelKey.DockAddressBar, more))
        }
        add(navigation(BrowserFeatureMenuAction.OpenSnoozedTabs, BrowserFeatureMenuLabelKey.SnoozedTabs, more))
    }

    private fun browserItems(capabilities: BrowserFeatureMenuCapabilities) = buildList {
        add(navigation(BrowserFeatureMenuAction.OpenDownloads, BrowserFeatureMenuLabelKey.Downloads))
        add(navigation(BrowserFeatureMenuAction.OpenHistory, BrowserFeatureMenuLabelKey.History))
        add(navigation(BrowserFeatureMenuAction.OpenFavorites, BrowserFeatureMenuLabelKey.Favorites))
        add(navigation(BrowserFeatureMenuAction.OpenPasswords, BrowserFeatureMenuLabelKey.Passwords))
        if (capabilities.supportsFirefoxExtensions) {
            add(
                navigation(
                    BrowserFeatureMenuAction.OpenFirefoxExtensions,
                    BrowserFeatureMenuLabelKey.FirefoxExtensions,
                ),
            )
        }
        add(navigation(BrowserFeatureMenuAction.OpenSettings, BrowserFeatureMenuLabelKey.Settings))
    }

    private fun command(
        action: BrowserFeatureMenuAction,
        labelKey: BrowserFeatureMenuLabelKey,
        section: BrowserFeatureMenuSection = BrowserFeatureMenuSection.Page,
        enabled: Boolean = true,
        checked: Boolean? = null,
    ) = BrowserFeatureMenuItem(
        stableId = action.name,
        action = action,
        labelKey = labelKey,
        section = section,
        kind = BrowserFeatureMenuItemKind.Command,
        enabled = enabled,
        checked = checked,
    )

    private fun toggle(
        action: BrowserFeatureMenuAction,
        labelKey: BrowserFeatureMenuLabelKey,
        enabled: Boolean,
        checked: Boolean,
        section: BrowserFeatureMenuSection = BrowserFeatureMenuSection.Page,
    ) = BrowserFeatureMenuItem(
        stableId = action.name,
        action = action,
        labelKey = labelKey,
        section = section,
        kind = BrowserFeatureMenuItemKind.Toggle,
        enabled = enabled,
        checked = checked,
    )

    private fun navigation(
        action: BrowserFeatureMenuAction,
        labelKey: BrowserFeatureMenuLabelKey,
        section: BrowserFeatureMenuSection = BrowserFeatureMenuSection.Browser,
    ) = BrowserFeatureMenuItem(
        stableId = action.name,
        action = action,
        labelKey = labelKey,
        section = section,
        kind = BrowserFeatureMenuItemKind.Navigation,
        enabled = true,
    )
}
