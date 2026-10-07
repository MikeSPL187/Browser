package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserSessionResidencyRules
import dev.sk2andy.materialbrowser.browser.LinkLongPressAction
import dev.sk2andy.materialbrowser.data.InactiveTabLifetime
import dev.sk2andy.materialbrowser.data.TabOverviewMode
import dev.sk2andy.materialbrowser.shared.browser.AddressBarLongPressAction
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCard
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardHeader
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardLinkRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSliderRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSwitchRow
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsCardTokens
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

internal object TabSettingsTestTags {
    const val StackFolderMode = "tab_settings_stack_folder_mode"
    const val ResidentTabLimit = "tab_settings_resident_limit"
    const val ListStartsAtBottom = "tab_settings_list_starts_at_bottom"
    const val AutomaticSorting = "tab_settings_automatic_sorting"
    const val ClosedTabUndo = "tab_settings_closed_tab_undo"
    const val AddressBarDocking = "tab_settings_address_bar_docking"
    const val AddressBarLongPressAction = "tab_settings_address_bar_long_press_action"
    const val LinkLongPressAction = "tab_settings_link_long_press_action"
    const val ArchiveInactiveTabs = "tab_settings_archive_inactive_tabs"
}

/** What the tabs and gestures page shows (board W-SetTabs). */
internal object TabSettingsRules {
    val DismissResistanceRange = 10..90
    const val DISMISS_RESISTANCE_STEP = 10

    /** The hero overview has no top or bottom to start from. */
    fun listCanStartAtBottom(mode: TabOverviewMode): Boolean = mode != TabOverviewMode.Hero
}

/** Tabs and gestures (board W-SetTabs): gestures first, then the overview, then tabs in memory. */
@Composable
internal fun TabsAndGesturesSettingsPage(
    inactiveTabLifetime: InactiveTabLifetime,
    residentTabLimit: Int,
    tabOverviewMode: TabOverviewMode,
    tabStackFolderMode: TabOverviewMode,
    tabListStartsAtBottom: Boolean,
    automaticTabSortingEnabled: Boolean,
    isClosedTabUndoEnabled: Boolean = false,
    dismissResistancePercent: Int,
    profilesEnabled: Boolean,
    isAddressBarDockingEnabled: Boolean,
    archiveInactiveTabs: Boolean = true,
    addressBarLongPressAction: AddressBarLongPressAction = AddressBarLongPressAction.Default,
    linkLongPressAction: LinkLongPressAction = LinkLongPressAction.LinkPeek,
    onInactiveTabLifetimeChanged: (InactiveTabLifetime) -> Unit,
    onArchiveInactiveTabsChanged: (Boolean) -> Unit = {},
    onResidentTabLimitChanged: (Int) -> Unit,
    onTabOverviewModeChanged: (TabOverviewMode) -> Unit,
    onTabStackFolderModeChanged: (TabOverviewMode) -> Unit,
    onTabListStartsAtBottomChanged: (Boolean) -> Unit,
    onAutomaticTabSortingEnabledChanged: (Boolean) -> Unit,
    onClosedTabUndoEnabledChanged: (Boolean) -> Unit = {},
    onDismissResistancePercentChanged: (Int) -> Unit,
    onProfilesEnabledChanged: (Boolean) -> Unit,
    onAddressBarDockingEnabledChanged: (Boolean) -> Unit,
    onAddressBarLongPressActions: () -> Unit = {},
    onLinkLongPressActionChanged: (LinkLongPressAction) -> Unit = {},
    onLinkPeekActions: () -> Unit = {},
    onAddressBarActions: () -> Unit,
    onMenuActions: () -> Unit = {},
    onBack: () -> Unit,
) {
    val cardColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh)
    val dividerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    var residentLimit by remember(residentTabLimit) { mutableIntStateOf(residentTabLimit) }
    var dismissResistance by remember(dismissResistancePercent) {
        mutableIntStateOf(dismissResistancePercent)
    }
    SettingsPage(
        title = stringResource(R.string.settings_tabs_gestures_title),
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.cardGap)) {
            SettingsCardHeader(stringResource(R.string.settings_section_gestures))
            SettingsCard(containerColor = cardColor) {
                SettingsCardLinkRow(
                    title = stringResource(R.string.settings_address_bar_long_press_title),
                    summary = stringResource(addressBarLongPressAction.labelRes()),
                    dividerColor = dividerColor,
                    divider = true,
                    onClick = onAddressBarLongPressActions,
                    modifier = Modifier.testTag(TabSettingsTestTags.AddressBarLongPressAction),
                )
                SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_link_long_press_action),
                    selected = linkLongPressAction,
                    options = LinkLongPressAction.entries,
                    label = { action -> stringResource(action.labelRes()) },
                    dividerColor = dividerColor,
                    divider = true,
                    onSelected = onLinkLongPressActionChanged,
                    modifier = Modifier.testTag(TabSettingsTestTags.LinkLongPressAction),
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.settings_link_peek_actions_title),
                    summary = stringResource(R.string.settings_link_peek_actions_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onClick = onLinkPeekActions,
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.settings_address_bar_actions_title),
                    summary = stringResource(R.string.settings_address_bar_actions_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onClick = onAddressBarActions,
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.settings_menu_actions_title),
                    summary = stringResource(R.string.settings_menu_actions_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onClick = onMenuActions,
                )
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_address_bar_docking_title),
                    summary = stringResource(R.string.settings_address_bar_docking_subtitle),
                    checked = isAddressBarDockingEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    divider = true,
                    onCheckedChange = onAddressBarDockingEnabledChanged,
                    modifier = Modifier.testTag(TabSettingsTestTags.AddressBarDocking),
                )
                val resistanceTitle = stringResource(R.string.settings_tab_dismiss_resistance)
                SettingsCardSliderRow(
                    title = resistanceTitle,
                    summary = stringResource(
                        R.string.settings_tab_dismiss_resistance_summary,
                        dismissResistance,
                    ),
                    dividerColor = dividerColor,
                ) {
                    SettingsCardSlider(
                        value = dismissResistancePercent,
                        range = TabSettingsRules.DismissResistanceRange,
                        step = TabSettingsRules.DISMISS_RESISTANCE_STEP,
                        label = resistanceTitle,
                        onValueChange = { dismissResistance = it },
                        onValueChangeFinished = onDismissResistancePercentChanged,
                    )
                }
            }
            SettingsCardHeader(stringResource(R.string.settings_tabs_group_overview))
            SettingsCard(containerColor = cardColor) {
                SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_tab_overview_mode),
                    selected = tabOverviewMode,
                    options = TabOverviewMode.entries,
                    label = { mode -> mode.displayName() },
                    dividerColor = dividerColor,
                    divider = true,
                    onSelected = onTabOverviewModeChanged,
                )
                if (TabStacksFeature.ENABLED) SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_tab_stack_folder_mode),
                    selected = tabStackFolderMode,
                    options = TabOverviewMode.entries,
                    label = { mode -> mode.displayName() },
                    dividerColor = dividerColor,
                    divider = true,
                    onSelected = onTabStackFolderModeChanged,
                    modifier = Modifier.testTag(TabSettingsTestTags.StackFolderMode),
                )
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_tab_list_starts_at_bottom_title),
                    summary = stringResource(R.string.settings_tab_list_starts_at_bottom_subtitle),
                    checked = tabListStartsAtBottom,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    divider = true,
                    enabled = TabSettingsRules.listCanStartAtBottom(tabOverviewMode),
                    onCheckedChange = onTabListStartsAtBottomChanged,
                    modifier = Modifier.testTag(TabSettingsTestTags.ListStartsAtBottom),
                )
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_automatic_tab_sorting_title),
                    summary = stringResource(R.string.settings_automatic_tab_sorting_subtitle),
                    checked = automaticTabSortingEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    divider = true,
                    onCheckedChange = onAutomaticTabSortingEnabledChanged,
                    modifier = Modifier.testTag(TabSettingsTestTags.AutomaticSorting),
                )
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_closed_tab_undo_title),
                    summary = stringResource(R.string.settings_closed_tab_undo_summary),
                    checked = isClosedTabUndoEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    onCheckedChange = onClosedTabUndoEnabledChanged,
                    modifier = Modifier.testTag(TabSettingsTestTags.ClosedTabUndo),
                )
            }
            SettingsCardHeader(stringResource(R.string.settings_section_tabs))
            SettingsCard(containerColor = cardColor) {
                val residentTitle = stringResource(R.string.settings_resident_tab_limit)
                SettingsCardSliderRow(
                    title = residentTitle,
                    summary = pluralStringResource(
                        R.plurals.settings_resident_tab_limit_summary,
                        residentLimit,
                        residentLimit,
                    ),
                    dividerColor = dividerColor,
                    divider = true,
                ) {
                    SettingsCardSlider(
                        value = residentTabLimit,
                        range = BrowserSessionResidencyRules.MIN_LIMIT..
                            BrowserSessionResidencyRules.MAX_LIMIT,
                        label = residentTitle,
                        onValueChange = { residentLimit = it },
                        onValueChangeFinished = onResidentTabLimitChanged,
                        modifier = Modifier.testTag(TabSettingsTestTags.ResidentTabLimit),
                    )
                }
                SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_auto_close_tabs),
                    selected = inactiveTabLifetime,
                    options = InactiveTabLifetime.entries,
                    label = { lifetime -> lifetime.displayName() },
                    dividerColor = dividerColor,
                    divider = true,
                    onSelected = onInactiveTabLifetimeChanged,
                )
                // Only the lifetimes counted in days can archive; the others close every tab on
                // leaving.
                if (inactiveTabLifetime.maxAgeMillis != null) {
                    SettingsCardSwitchRow(
                        title = stringResource(R.string.settings_archive_inactive_tabs_title),
                        summary = stringResource(R.string.settings_archive_inactive_tabs_summary),
                        checked = archiveInactiveTabs,
                        summaryMaxLines = Int.MAX_VALUE,
                        dividerColor = dividerColor,
                        divider = true,
                        onCheckedChange = onArchiveInactiveTabsChanged,
                        modifier = Modifier.testTag(TabSettingsTestTags.ArchiveInactiveTabs),
                    )
                }
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_profiles_title),
                    summary = stringResource(R.string.settings_profiles_subtitle),
                    checked = profilesEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    onCheckedChange = onProfilesEnabledChanged,
                )
            }
        }
    }
}

private fun LinkLongPressAction.labelRes(): Int = when (this) {
    LinkLongPressAction.LinkPeek -> R.string.link_peek_title
    LinkLongPressAction.CopyLink -> R.string.external_link_preview_copy_link
    LinkLongPressAction.Share -> R.string.action_share
    LinkLongPressAction.DownloadLink -> R.string.action_download_link
    LinkLongPressAction.OpenInNewTabInBackground -> R.string.action_open_link_background_tab
    LinkLongPressAction.OpenInNewTabInForeground -> R.string.action_open_link_foreground_tab
    LinkLongPressAction.OpenInPrivateTabInBackground ->
        R.string.action_open_link_private_background_tab
    LinkLongPressAction.OpenInPrivateTabInForeground ->
        R.string.action_open_link_private_foreground_tab
}
