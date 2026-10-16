package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.data.InactiveTabLifetime
import dev.sk2andy.materialbrowser.data.TabOverviewMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.v5.ProvideV5SettingsSkin

/**
 * Tabs and gestures (board W-SetTabs) with the hero overview, which has no bottom to start from,
 * so that switch stands dimmed.
 */
@VolaPreviews
@Composable
private fun TabSettingsHeroPreview() {
    MaterialBrowserTheme {
        TabsAndGesturesSettingsPage(
            inactiveTabLifetime = InactiveTabLifetime.Never,
            residentTabLimit = 8,
            tabOverviewMode = TabOverviewMode.Hero,
            tabStackFolderMode = TabOverviewMode.Grid,
            tabListStartsAtBottom = false,
            automaticTabSortingEnabled = true,
            isClosedTabUndoEnabled = true,
            dismissResistancePercent = 40,
            profilesEnabled = true,
            isAddressBarDockingEnabled = true,
            onInactiveTabLifetimeChanged = {},
            onResidentTabLimitChanged = {},
            onTabOverviewModeChanged = {},
            onTabStackFolderModeChanged = {},
            onTabListStartsAtBottomChanged = {},
            onAutomaticTabSortingEnabledChanged = {},
            onDismissResistancePercentChanged = {},
            onProfilesEnabledChanged = {},
            onAddressBarDockingEnabledChanged = {},
            onAddressBarActions = {},
            onBack = {},
        )
    }
}

/** V15d-5 under ui.v5: «Tabs» (with «More») and its More page, drawn by the v5 skin. */
@Composable
private fun TabSettingsV5Preview(layer: SettingsLayer) {
    MaterialBrowserTheme {
        ProvideV5SettingsSkin(enabled = true) {
            TabsAndGesturesSettingsPage(
                layer = layer,
                inactiveTabLifetime = InactiveTabLifetime.entries.first { it.maxAgeMillis != null },
                residentTabLimit = 8,
                tabOverviewMode = TabOverviewMode.Grid,
                tabStackFolderMode = TabOverviewMode.Grid,
                tabListStartsAtBottom = false,
                automaticTabSortingEnabled = false,
                isClosedTabUndoEnabled = true,
                dismissResistancePercent = 40,
                profilesEnabled = true,
                isAddressBarDockingEnabled = false,
                onInactiveTabLifetimeChanged = {},
                onResidentTabLimitChanged = {},
                onTabOverviewModeChanged = {},
                onTabStackFolderModeChanged = {},
                onTabListStartsAtBottomChanged = {},
                onAutomaticTabSortingEnabledChanged = {},
                onDismissResistancePercentChanged = {},
                onProfilesEnabledChanged = {},
                onAddressBarDockingEnabledChanged = {},
                onAddressBarActions = {},
                onBack = {},
            )
        }
    }
}

@VolaPreviews
@Composable
private fun TabSettingsV5MainPreview() {
    TabSettingsV5Preview(SettingsLayer.Main)
}

@VolaPreviews
@Composable
private fun TabSettingsV5MorePreview() {
    TabSettingsV5Preview(SettingsLayer.More)
}
