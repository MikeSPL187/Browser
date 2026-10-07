package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.ProfileLockTrigger
import dev.sk2andy.materialbrowser.browser.ProfileProtection
import dev.sk2andy.materialbrowser.browser.ProfileProtectionRules

/** What the workspace sheets show (boards W-WorkspaceSheet, W-WorkspaceSettings). */
internal object WorkspaceSheetRules {
    /** «6 tabs · 5 in Essentials»; the Essentials part only when there are any. */
    fun summary(tabs: String, essentials: String?): String =
        if (essentials == null) tabs else "$tabs · $essentials"

    /** The workspace's own tabs; private tabs belong to no workspace for counting. */
    fun tabCount(tabs: List<BrowserTab>, profileId: String): Int =
        tabs.count { tab -> tab.profileId == profileId && !tab.isIncognito }

    /**
     * The collapsed icon grid of «New workspace»: the first [limit] icons, with the chosen one
     * always among them (in the last place when it lies further down).
     */
    fun collapsedIcons(icons: List<String>, selected: String?, limit: Int): List<String> {
        val first = icons.take(limit.coerceAtLeast(1))
        if (selected == null || selected in first || selected !in icons) return first
        return first.dropLast(1) + selected
    }

    /** A new workspace starts with the first icon chosen, so «Create» works at once. */
    fun defaultIcon(icons: List<String>): String? = icons.firstOrNull()

    /** The protection draft of «New workspace» as saved instance state: «trigger:minutes». */
    fun savedProtectionDraft(protection: ProfileProtection?): String? =
        protection?.let { "${it.lockTrigger.wireValue}:${it.cooldownMinutes}" }

    fun restoredProtectionDraft(saved: String?): ProfileProtection? {
        val parts = saved?.split(':', limit = 2)?.takeIf { it.size == 2 } ?: return null
        val trigger = ProfileLockTrigger.fromWireValue(parts[0]) ?: return null
        val minutes = parts[1].toIntOrNull() ?: ProfileProtectionRules.DEFAULT_COOLDOWN_MINUTES
        return ProfileProtectionRules.normalize(ProfileProtection(trigger, minutes))
    }
}
