package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.data.InactiveTabLifetime
import dev.sk2andy.materialbrowser.data.TabOverviewMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

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
