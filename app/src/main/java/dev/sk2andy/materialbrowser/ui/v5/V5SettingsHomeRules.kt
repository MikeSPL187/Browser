package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeItem
import dev.sk2andy.materialbrowser.ui.SettingsDestination
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

/*
 * How the v5 settings home (canvas screen «Settings», system-v5.md section 1.5) groups and colors
 * the pages that the current home lists, in the canvas order: the browser itself (General, Search,
 * Tabs), privacy (protection, passwords), then how it looks and reads (Appearance, Language and
 * translation, Downloads, Sync), and last what extends it and «About». Which pages show stays with
 * SettingsHomeRules (extensions only with Gecko, developer options once unlocked); the canvas
 * sections without a page of their own yet (Profiles, Private browsing, Clear data, Reading) wait
 * for their pages.
 */
object V5SettingsHomeRules {
    private val order: List<List<SettingsDestination?>> = listOf(
        listOf(
            SettingsDestination.Browser,
            SettingsDestination.Search,
            SettingsDestination.TabsAndGestures,
        ),
        listOf(
            SettingsDestination.ProtectionAndData,
            SettingsDestination.Passwords,
        ),
        listOf(
            SettingsDestination.Appearance,
            SettingsDestination.Translation,
            SettingsDestination.Downloads,
            SettingsDestination.Sync,
        ),
        // Null is the Firefox extensions action, which has no page of its own.
        listOf(SettingsDestination.Userscripts, null, SettingsDestination.AboutLegal, SettingsDestination.DeveloperOptions),
    )

    /** Pages of v4 features the owner left out of v5 (V28, section Г): site capsules. */
    private val leftOut = setOf(SettingsDestination.SiteCapsules)

    /** [items] in the v5 groups, empty groups dropped; a page v5 does not know joins the last group. */
    fun groups(items: List<SettingsHomeItem>): List<List<SettingsHomeItem>> {
        val shown = items.filterNot { it.destination in leftOut }
        val grouped = order.map { group ->
            group.mapNotNull { destination -> shown.firstOrNull { it.destination == destination } }
        }
        val known = grouped.flatten().toSet()
        val rest = shown.filterNot { it in known }
        return (grouped.dropLast(1) + listOf(grouped.last() + rest)).filter { it.isNotEmpty() }
    }

    /** The section tile: the color belongs to the section, not to the profile accent. */
    fun tile(item: SettingsHomeItem): V5Tile = when (item.destination) {
        SettingsDestination.Sync -> V5Tile.Sky
        SettingsDestination.Search,
        SettingsDestination.TabsAndGestures,
        SettingsDestination.Browser,
        -> V5Tile.Blue
        SettingsDestination.Translation -> V5Tile.Teal
        SettingsDestination.Appearance -> V5Tile.Pink
        SettingsDestination.ProtectionAndData -> V5Tile.Green
        SettingsDestination.Passwords -> V5Tile.Red
        SettingsDestination.Downloads -> V5Tile.Amber
        else -> V5Tile.Gray
    }
}
