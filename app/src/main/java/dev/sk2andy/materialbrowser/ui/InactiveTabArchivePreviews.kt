package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.InactiveTabLifetime
import dev.sk2andy.materialbrowser.data.SnoozedTab
import dev.sk2andy.materialbrowser.data.TabOverviewMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

private const val PREVIEW_NOW = 1_790_000_000_000L
private const val PREVIEW_DAY = 86_400_000L

private fun previewTab(id: String, title: String, url: String) =
    BrowserTab(id = id, lastAccessedAt = 0L, title = title, url = url)

@Composable
private fun ArchivePreviewFrame(content: @Composable () -> Unit) {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
        content = content,
    )
}

/** Snoozed tabs with an archived one: it says when it was put away, not when it returns. */
@VolaPreviews
@Composable
private fun SnoozedTabsWithArchivePreview() {
    ArchivePreviewFrame {
        SnoozedTabsScreen(
            snoozedTabs = listOf(
                SnoozedTab(
                    tab = previewTab("a", "Sprint review", "https://tasks.example.com/"),
                    wakeAtMillis = PREVIEW_NOW + PREVIEW_DAY,
                    createdAtMillis = PREVIEW_NOW,
                ),
                SnoozedTab(
                    tab = previewTab("b", "Lake Baikal", "https://travel.example.org/"),
                    wakeAtMillis = SnoozedTab.ARCHIVED_WAKE_AT_MILLIS,
                    createdAtMillis = PREVIEW_NOW - PREVIEW_DAY,
                ),
            ),
            profiles = listOf(BrowserProfile(id = "candy", emoji = "🏠", name = "Personal")),
            onBack = {},
            onReschedule = { _, _ -> true },
            onOpenNow = { true },
            onDelete = { true },
        )
    }
}

/** «Tabs and gestures» with a lifetime in days, which brings up the archive switch. */
@VolaPreviews
@Composable
private fun TabSettingsArchivePreview() {
    ArchivePreviewFrame {
        TabsAndGesturesSettingsPage(
            inactiveTabLifetime = InactiveTabLifetime.SevenDays,
            residentTabLimit = 10,
            tabOverviewMode = TabOverviewMode.Grid,
            tabStackFolderMode = TabOverviewMode.Grid,
            tabListStartsAtBottom = false,
            automaticTabSortingEnabled = false,
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
