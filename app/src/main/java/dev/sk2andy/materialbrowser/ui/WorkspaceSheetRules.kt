package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserTab

/** What the workspace sheets show (boards W-WorkspaceSheet, W-WorkspaceSettings). */
internal object WorkspaceSheetRules {
    /** «6 tabs · 5 in Essentials»; the Essentials part only when there are any. */
    fun summary(tabs: String, essentials: String?): String =
        if (essentials == null) tabs else "$tabs · $essentials"

    /** The workspace's own tabs; private tabs belong to no workspace for counting. */
    fun tabCount(tabs: List<BrowserTab>, profileId: String): Int =
        tabs.count { tab -> tab.profileId == profileId && !tab.isIncognito }

    /** A new workspace starts with the first icon chosen, so «Create» works at once. */
    fun defaultIcon(icons: List<String>): String? = icons.firstOrNull()
}
