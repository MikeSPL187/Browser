package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeItem
import dev.sk2andy.materialbrowser.ui.SettingsDestination
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

/*
 * How the v5 settings home (canvas screen «ж», system-v5.md section 1.5) groups and colors the
 * pages that the current home lists. Which pages show stays with SettingsHomeRules (extensions
 * only with Gecko, developer options once unlocked); v5 only orders them into Safari-like groups.
 */
object V5SettingsHomeRules {
    private val order: List<List<SettingsDestination?>> = listOf(
        listOf(SettingsDestination.Sync),
        listOf(
            SettingsDestination.Search,
            SettingsDestination.TabsAndGestures,
            SettingsDestination.Appearance,
            SettingsDestination.Browser,
            SettingsDestination.Translation,
        ),
        listOf(
            SettingsDestination.ProtectionAndData,
            SettingsDestination.SiteCapsules,
            SettingsDestination.Passwords,
        ),
        // Null is the Firefox extensions action, which has no page of its own.
        listOf(SettingsDestination.Userscripts, null, SettingsDestination.Downloads),
        listOf(SettingsDestination.AboutLegal, SettingsDestination.DeveloperOptions),
    )

    /** The group whose last row the «tint the bar» switch follows. */
    const val APPEARANCE_GROUP = 1

    /** [items] in the v5 groups, empty groups dropped; a page v5 does not know joins the last group. */
    fun groups(items: List<SettingsHomeItem>): List<List<SettingsHomeItem>> {
        val grouped = order.map { group ->
            group.mapNotNull { destination -> items.firstOrNull { it.destination == destination } }
        }
        val known = grouped.flatten().toSet()
        val rest = items.filterNot { it in known }
        return (grouped.dropLast(1) + listOf(grouped.last() + rest)).filter { it.isNotEmpty() }
    }

    /** The section tile: the color belongs to the section, not to the profile accent. */
    fun tile(item: SettingsHomeItem): V5Tile = when (item.destination) {
        SettingsDestination.Sync -> V5Tile.Sky
        SettingsDestination.Search,
        SettingsDestination.TabsAndGestures,
        SettingsDestination.Browser,
        SettingsDestination.Translation,
        -> V5Tile.Blue
        SettingsDestination.Appearance -> V5Tile.Pink
        SettingsDestination.ProtectionAndData,
        SettingsDestination.SiteCapsules,
        -> V5Tile.Green
        SettingsDestination.Passwords -> V5Tile.Red
        SettingsDestination.Downloads -> V5Tile.Amber
        else -> V5Tile.Gray
    }
}
