package dev.sk2andy.materialbrowser.ui

/** The quick actions in the top row of the tab actions sheet (board W-TabActions). */
internal enum class TabQuickAction {
    AddToEssentials,
    RemoveFromEssentials,
    Duplicate,
    Pin,
    Unpin,
    Share,
}

/** Everything else Candy's tab menu offered, behind «More» so nothing is more than two taps away. */
internal enum class TabMoreAction {
    SideBySide,
    AddBookmark,
    RemoveBookmark,
    MuteSite,
    UnmuteSite,
    OpenInApp,
    Print,
    Summarize,
    CandyTrail,
    SiteCapsule,
    CloseAll,
}

/** What the tab actions sheet knows about the tab it opened for. */
internal data class TabActionsFacts(
    /** A page on the web; false for a blank tab. */
    val isWebPage: Boolean,
    /** An http(s) page, which can become an Essential or a site capsule. */
    val isHttpPage: Boolean,
    val isIncognito: Boolean,
    val isPinned: Boolean,
    val isEssential: Boolean,
    val canAddEssential: Boolean,
    val isBookmarked: Boolean,
    val canToggleSiteMute: Boolean,
    val isSiteMuted: Boolean,
    val canDelete: Boolean,
    val canCloseAll: Boolean,
    val otherWorkspaceCount: Int,
    /** Another page than the selected one, which Split View can show next to it. */
    val canOpenSideBySide: Boolean = false,
)

internal object TabActionsRules {
    /** At most four, as on the board; Split View («Рядом») waits for Q30, so pinning takes its place. */
    fun quickActions(facts: TabActionsFacts): List<TabQuickAction> = buildList {
        if (facts.isHttpPage && !facts.isIncognito) {
            when {
                facts.isEssential -> add(TabQuickAction.RemoveFromEssentials)
                facts.canAddEssential -> add(TabQuickAction.AddToEssentials)
            }
        }
        if (facts.isWebPage) add(TabQuickAction.Duplicate)
        add(if (facts.isPinned) TabQuickAction.Unpin else TabQuickAction.Pin)
        if (facts.isWebPage) add(TabQuickAction.Share)
    }

    fun canMove(facts: TabActionsFacts): Boolean = facts.otherWorkspaceCount > 0

    /** Snoozed tabs are stored, so a private tab is never snoozed. */
    fun canSnooze(facts: TabActionsFacts): Boolean = !facts.isIncognito

    fun canClose(facts: TabActionsFacts): Boolean = facts.canDelete

    fun moreActions(facts: TabActionsFacts): List<TabMoreAction> = buildList {
        if (facts.canOpenSideBySide) add(TabMoreAction.SideBySide)
        if (facts.isWebPage && !facts.isIncognito) {
            add(if (facts.isBookmarked) TabMoreAction.RemoveBookmark else TabMoreAction.AddBookmark)
        }
        if (facts.canToggleSiteMute) {
            add(if (facts.isSiteMuted) TabMoreAction.UnmuteSite else TabMoreAction.MuteSite)
        }
        if (facts.isWebPage) {
            add(TabMoreAction.OpenInApp)
            add(TabMoreAction.Print)
            add(TabMoreAction.Summarize)
            add(TabMoreAction.CandyTrail)
        }
        if (facts.isHttpPage && !facts.isIncognito) add(TabMoreAction.SiteCapsule)
        if (facts.canCloseAll) add(TabMoreAction.CloseAll)
    }
}
