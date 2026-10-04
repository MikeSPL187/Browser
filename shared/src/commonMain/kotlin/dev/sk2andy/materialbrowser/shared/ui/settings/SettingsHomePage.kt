package dev.sk2andy.materialbrowser.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsCardTokens
import dev.sk2andy.materialbrowser.ui.SettingsDestination

enum class SettingsHomeIcon {
    Search,
    Sync,
    TabsAndGestures,
    Appearance,
    Browser,
    Downloads,
    Userscripts,
    FirefoxExtensions,
    SiteCapsules,
    ProtectionAndData,
    DeveloperOptions,
    AboutLegal,
}

/**
 * The cards of the settings home, top to bottom (board W-Settings): protection and data first,
 * then how Vola looks and behaves, then what extends it, and the rest last.
 */
enum class SettingsHomeCard {
    Protection,
    Personalization,
    Features,
    About,
}

/**
 * One row of the settings home. [accent] colors its icon tile, the way a workspace's accent
 * colors its gem; each page keeps its own color so the eye finds it again.
 */
data class SettingsHomeItem(
    val card: SettingsHomeCard,
    val destination: SettingsDestination?,
    val icon: SettingsHomeIcon,
    val accent: WorkspaceAccent,
    val isFirefoxExtensionsAction: Boolean = false,
)

/** A row with its words in the interface language: the page's title and what is set there. */
data class SettingsHomeEntry(
    val item: SettingsHomeItem,
    val title: String,
    val summary: String?,
)

object SettingsHomeRules {
    fun items(
        hasFirefoxExtensions: Boolean,
        hasDeveloperOptions: Boolean = false,
    ): List<SettingsHomeItem> = buildList {
        add(item(SettingsHomeCard.Protection, SettingsDestination.ProtectionAndData, SettingsHomeIcon.ProtectionAndData, WorkspaceAccent.Green))
        add(item(SettingsHomeCard.Protection, SettingsDestination.Sync, SettingsHomeIcon.Sync, WorkspaceAccent.Blue))
        add(item(SettingsHomeCard.Personalization, SettingsDestination.Appearance, SettingsHomeIcon.Appearance, WorkspaceAccent.Rose))
        add(item(SettingsHomeCard.Personalization, SettingsDestination.TabsAndGestures, SettingsHomeIcon.TabsAndGestures, WorkspaceAccent.Teal))
        add(item(SettingsHomeCard.Personalization, SettingsDestination.Search, SettingsHomeIcon.Search, WorkspaceAccent.Violet))
        add(item(SettingsHomeCard.Features, SettingsDestination.Userscripts, SettingsHomeIcon.Userscripts, WorkspaceAccent.Graphite))
        if (hasFirefoxExtensions) {
            add(
                SettingsHomeItem(
                    card = SettingsHomeCard.Features,
                    destination = null,
                    icon = SettingsHomeIcon.FirefoxExtensions,
                    accent = WorkspaceAccent.Graphite,
                    isFirefoxExtensionsAction = true,
                ),
            )
        }
        add(item(SettingsHomeCard.Features, SettingsDestination.SiteCapsules, SettingsHomeIcon.SiteCapsules, WorkspaceAccent.Amber))
        add(item(SettingsHomeCard.Features, SettingsDestination.Downloads, SettingsHomeIcon.Downloads, WorkspaceAccent.Green))
        add(item(SettingsHomeCard.About, SettingsDestination.Browser, SettingsHomeIcon.Browser, WorkspaceAccent.Coral))
        if (hasDeveloperOptions) {
            add(item(SettingsHomeCard.About, SettingsDestination.DeveloperOptions, SettingsHomeIcon.DeveloperOptions, WorkspaceAccent.Graphite))
        }
        add(item(SettingsHomeCard.About, SettingsDestination.AboutLegal, SettingsHomeIcon.AboutLegal, WorkspaceAccent.Blue))
    }

    /** The items card by card, in order; no card is empty. */
    fun cards(items: List<SettingsHomeItem>): List<List<SettingsHomeItem>> =
        SettingsHomeCard.entries
            .map { card -> items.filter { item -> item.card == card } }
            .filter { rows -> rows.isNotEmpty() }

    private fun item(
        card: SettingsHomeCard,
        destination: SettingsDestination,
        icon: SettingsHomeIcon,
        accent: WorkspaceAccent,
    ): SettingsHomeItem = SettingsHomeItem(
        card = card,
        destination = destination,
        icon = icon,
        accent = accent,
    )
}

/**
 * The settings home (board W-Settings): [header] (such as «Make Vola your default»), then the
 * pages on cards. A long press on «About» unlocks the developer options.
 */
@Composable
fun SettingsHomePage(
    title: String,
    backContentDescription: String,
    entries: List<SettingsHomeEntry>,
    cardColor: Color,
    dividerColor: Color,
    tileColors: @Composable (WorkspaceAccent) -> SettingsTileColors,
    icon: @Composable (SettingsHomeIcon, Modifier, Color) -> Unit,
    unlockDeveloperOptionsLabel: String,
    onOpen: (SettingsHomeItem) -> Unit,
    onDismiss: () -> Unit,
    developerOptionsUnlocked: Boolean = false,
    onUnlockDeveloperOptions: (() -> Unit)? = null,
    isDestinationEnabled: (SettingsDestination) -> Boolean = { true },
    header: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    SettingsPage(
        title = title,
        backContentDescription = backContentDescription,
        onBack = onDismiss,
        actions = actions,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.cardGap)) {
            header()
            val cards = SettingsHomeRules.cards(entries.map(SettingsHomeEntry::item))
            cards.forEach { rows ->
                SettingsCard(containerColor = cardColor) {
                    rows.forEachIndexed { index, item ->
                        val entry = entries.first { it.item == item }
                        val destination = item.destination
                        val unlocks = destination == SettingsDestination.AboutLegal &&
                            !developerOptionsUnlocked &&
                            onUnlockDeveloperOptions != null
                        SettingsCardRow(
                            title = entry.title,
                            summary = entry.summary,
                            tileColors = tileColors(item.accent),
                            icon = { modifier, tint -> icon(item.icon, modifier, tint) },
                            dividerColor = dividerColor,
                            divider = index != rows.lastIndex,
                            enabled = item.isFirefoxExtensionsAction ||
                                destination?.let(isDestinationEnabled) == true,
                            onLongClickLabel = unlockDeveloperOptionsLabel.takeIf { unlocks },
                            onLongClick = onUnlockDeveloperOptions.takeIf { unlocks },
                            onClick = { onOpen(item) },
                        )
                    }
                }
            }
        }
    }
}
