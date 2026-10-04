package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeIcon
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeItem
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeLabel
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeResources
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeRules
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomePage as SharedSettingsHomePage
import dev.sk2andy.materialbrowser.ui.theme.VolaSettingsHome

/** What the home's rows summarize live (board W-Settings); null falls back to the static text. */
internal data class SettingsHomeLiveState(
    val appearance: AppearanceSettings? = null,
    val searchEngine: SearchEngine? = null,
    val searchSuggestionsOn: Boolean = false,
    val httpsOnlyMode: HttpsOnlyMode? = null,
    val tabCount: Int = 0,
    val userscriptCount: Int = 0,
    val capsuleCount: Int = 0,
    val isDefaultBrowser: Boolean = true,
)

@Composable
internal fun SettingsHomePage(
    downloadSummary: String,
    onDestinationChanged: (SettingsDestination) -> Unit,
    onDismiss: () -> Unit,
    onOpenFirefoxExtensions: (() -> Unit)? = null,
    developerOptionsUnlocked: Boolean = false,
    onUnlockDeveloperOptions: (() -> Unit)? = null,
    live: SettingsHomeLiveState = SettingsHomeLiveState(),
    onMakeDefault: (() -> Unit)? = null,
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    if (searching) {
        SettingsSearchPage(
            onOpen = { result ->
                searching = false
                onDestinationChanged(result.destination)
            },
            onBack = { searching = false },
        )
        return
    }
    SharedSettingsHomePage(
        downloadSummary = downloadSummary,
        resources = AndroidSettingsHomeResources,
        style = VolaSettingsHome.style(),
        icon = { icon, modifier, tint ->
            AndroidSettingsHomeIcon(
                icon = icon,
                modifier = modifier,
                tint = tint,
            )
        },
        onDestinationChanged = onDestinationChanged,
        onDismiss = onDismiss,
        onOpenFirefoxExtensions = onOpenFirefoxExtensions,
        developerOptionsUnlocked = developerOptionsUnlocked,
        onUnlockDeveloperOptions = onUnlockDeveloperOptions,
        liveSummary = { item -> liveSummary(item, live) },
        onMakeDefault = onMakeDefault.takeIf { !live.isDefaultBrowser },
        brandMark = { modifier ->
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground_art),
                contentDescription = null,
                modifier = modifier,
            )
        },
        actions = {
            IconButton(
                onClick = { searching = true },
                modifier = Modifier.testTag(SettingsSearchTestTags.Open),
            ) {
                Icon(
                    VolaIcons.Search,
                    contentDescription = stringResource(R.string.settings_search_open),
                )
            }
        },
    )
}

@Composable
private fun liveSummary(item: SettingsHomeItem, live: SettingsHomeLiveState): String? {
    return when (item.destination) {
        SettingsDestination.Appearance -> live.appearance?.let { appearance ->
            SettingsHomeRules.joinSummary(
                listOf(
                    stringResource(
                        when (appearance.chromeStyle) {
                            BrowserChromeStyle.Frame -> R.string.settings_chrome_style_frame
                            BrowserChromeStyle.Air -> R.string.settings_chrome_style_air
                        },
                    ),
                    stringResource(
                        when (appearance.appearanceMode) {
                            BrowserAppearanceMode.System -> R.string.settings_home_theme_auto
                            BrowserAppearanceMode.Light -> R.string.settings_home_theme_light
                            BrowserAppearanceMode.Dark -> R.string.settings_home_theme_dark
                        },
                    ),
                ),
            )
        }
        SettingsDestination.ProtectionAndData -> when (live.httpsOnlyMode) {
            HttpsOnlyMode.Always -> stringResource(R.string.settings_home_https_always)
            HttpsOnlyMode.PrivateTabs -> stringResource(R.string.settings_home_https_private)
            HttpsOnlyMode.Off, null -> null
        }
        SettingsDestination.Search -> live.searchEngine?.let { engine ->
            SettingsHomeRules.joinSummary(
                listOf(
                    engine.displayName,
                    stringResource(R.string.settings_home_search_suggestions)
                        .takeIf { live.searchSuggestionsOn },
                ),
            )
        }
        SettingsDestination.TabsAndGestures -> live.tabCount.takeIf { it > 0 }?.let { count ->
            pluralStringResource(R.plurals.settings_home_open_tabs, count, count)
        }
        SettingsDestination.Userscripts -> live.userscriptCount.takeIf { it > 0 }?.let { count ->
            pluralStringResource(R.plurals.settings_home_userscripts_count, count, count)
        }
        SettingsDestination.SiteCapsules -> live.capsuleCount.takeIf { it > 0 }?.let { count ->
            pluralStringResource(R.plurals.settings_home_capsules_count, count, count)
        }
        SettingsDestination.Browser -> stringResource(R.string.settings_default_browser_active)
            .takeIf { live.isDefaultBrowser }
        else -> null
    }
}

private object AndroidSettingsHomeResources : SettingsHomeResources {
    @Composable
    override fun text(label: SettingsHomeLabel): String = stringResource(
        when (label) {
            SettingsHomeLabel.Title -> R.string.settings_title
            SettingsHomeLabel.Back -> R.string.action_back
            SettingsHomeLabel.SearchTitle -> R.string.settings_section_search
            SettingsHomeLabel.SearchSummary -> R.string.settings_home_search_summary
            SettingsHomeLabel.SyncTitle -> R.string.sync_settings_title
            SettingsHomeLabel.SyncSummary -> R.string.settings_home_sync_summary
            SettingsHomeLabel.TabsAndGesturesTitle -> R.string.settings_tabs_gestures_title
            SettingsHomeLabel.TabsAndGesturesSummary ->
                R.string.settings_home_tabs_gestures_summary
            SettingsHomeLabel.AppearanceTitle -> R.string.settings_appearance_title
            SettingsHomeLabel.AppearanceSummary -> R.string.settings_home_appearance_summary
            SettingsHomeLabel.BrowserTitle -> R.string.settings_section_browser
            SettingsHomeLabel.BrowserSummary -> R.string.settings_home_browser_summary
            SettingsHomeLabel.DownloadsTitle -> R.string.settings_downloads_title
            SettingsHomeLabel.UserscriptsTitle -> R.string.userscript_title
            SettingsHomeLabel.UserscriptsSummary -> R.string.settings_home_userscripts_summary
            SettingsHomeLabel.FirefoxExtensionsTitle -> R.string.gecko_extensions_title
            SettingsHomeLabel.FirefoxExtensionsSummary -> R.string.gecko_extensions_summary
            SettingsHomeLabel.SiteCapsulesTitle -> R.string.capsule_settings_title
            SettingsHomeLabel.SiteCapsulesSummary -> R.string.settings_home_capsules_summary
            SettingsHomeLabel.ProtectionAndDataTitle ->
                R.string.settings_protection_data_title
            SettingsHomeLabel.ProtectionAndDataSummary ->
                R.string.settings_home_protection_summary
            SettingsHomeLabel.DeveloperOptionsTitle -> R.string.developer_options_title
            SettingsHomeLabel.DeveloperOptionsSummary -> R.string.developer_options_summary
            SettingsHomeLabel.UnlockDeveloperOptions ->
                R.string.developer_options_unlock_action
            SettingsHomeLabel.AboutLegalTitle -> R.string.settings_section_about_legal
            SettingsHomeLabel.AboutLegalSummary -> R.string.settings_home_about_summary
            SettingsHomeLabel.MakeDefaultTitle -> R.string.settings_home_make_default_title
            SettingsHomeLabel.MakeDefaultSummary -> R.string.settings_home_make_default_summary
            SettingsHomeLabel.MakeDefaultAction -> R.string.settings_home_make_default_action
        },
    )
}

@Composable
private fun AndroidSettingsHomeIcon(
    icon: SettingsHomeIcon,
    modifier: Modifier,
    tint: Color,
) {
    val vector = when (icon) {
        SettingsHomeIcon.Search -> VolaIcons.Search
        SettingsHomeIcon.Sync -> VolaIcons.Sync
        SettingsHomeIcon.TabsAndGestures -> VolaIcons.Tab
        SettingsHomeIcon.Appearance -> VolaIcons.Palette
        SettingsHomeIcon.Browser -> VolaIcons.Settings
        SettingsHomeIcon.Downloads -> ImageVector.vectorResource(R.drawable.ic_reader_download)
        SettingsHomeIcon.Userscripts,
        SettingsHomeIcon.FirefoxExtensions,
        -> ImageVector.vectorResource(R.drawable.ic_symbol_extension)
        SettingsHomeIcon.SiteCapsules -> VolaIcons.Favorite
        SettingsHomeIcon.ProtectionAndData -> VolaIcons.Lock
        SettingsHomeIcon.DeveloperOptions -> VolaIcons.Build
        SettingsHomeIcon.AboutLegal -> VolaIcons.Info
    }
    Icon(
        imageVector = vector,
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}
