package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.settings.SettingsRegistry
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeEntry
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeIcon
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeItem
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeRules
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomePage as SharedSettingsHomePage
import dev.sk2andy.materialbrowser.ui.theme.VolaSettingsHomeTokens
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

internal object SettingsHomeTestTags {
    const val DefaultBrowserBanner = "settings_home_default_browser"
    const val MakeDefault = "settings_home_make_default"
}

@Composable
internal fun SettingsHomePage(
    state: SettingsHomeState,
    isDefaultBrowser: Boolean,
    onOpenDefaultBrowserSettings: () -> Unit,
    onDestinationChanged: (SettingsDestination) -> Unit,
    onDismiss: () -> Unit,
    onOpenFirefoxExtensions: (() -> Unit)? = null,
    developerOptionsUnlocked: Boolean = false,
    onUnlockDeveloperOptions: (() -> Unit)? = null,
    hasTranslation: Boolean = false,
    profileCard: (@Composable () -> Unit)? = null,
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
    // V15c: under ui.v5 the same pages in the v5 grouped list; V15d draws the pages as v5 lists too.
    val uiV5 = rememberSettingsUiV5()
    val items = SettingsHomeRules.items(
        hasFirefoxExtensions = onOpenFirefoxExtensions != null,
        hasDeveloperOptions = developerOptionsUnlocked,
        // Translation on the phone has only a v5 page (V23).
        hasTranslation = hasTranslation && uiV5,
    )
    val entries = items.map { item -> settingsHomeEntry(item, state) }
    if (uiV5) {
        // The v5 interface has no frame style; under Appearance only the theme counts.
        val appearanceSummary = stringResource(SettingsHomeSummaryRules.theme(state.appearance.appearanceMode))
        // Its gestures are the v5 interface's own, so the page is just «Tabs».
        val tabsTitle = stringResource(R.string.settings_section_tabs)
        SettingsHomeV5(
            entries = entries.map { entry ->
                when (entry.item.destination) {
                    SettingsDestination.Appearance -> entry.copy(summary = appearanceSummary)
                    SettingsDestination.TabsAndGestures -> entry.copy(title = tabsTitle)
                    else -> entry
                }
            },
            isDefaultBrowser = isDefaultBrowser,
            onOpenDefaultBrowserSettings = onOpenDefaultBrowserSettings,
            onOpen = { item ->
                if (item.isFirefoxExtensionsAction) {
                    onOpenFirefoxExtensions?.invoke()
                } else {
                    item.destination?.let(onDestinationChanged)
                }
            },
            onSearch = { searching = true },
            onDismiss = onDismiss,
            developerOptionsUnlocked = developerOptionsUnlocked,
            onUnlockDeveloperOptions = onUnlockDeveloperOptions,
            profileCard = profileCard,
        )
        return
    }
    SharedSettingsHomePage(
        title = stringResource(R.string.settings_title),
        backContentDescription = stringResource(R.string.action_back),
        entries = entries,
        cardColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh),
        dividerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        tileColors = { accent -> settingsTileColors(accent) },
        icon = { icon, modifier, tint ->
            Icon(
                imageVector = settingsHomeIcon(icon),
                contentDescription = null,
                modifier = modifier,
                tint = tint,
            )
        },
        unlockDeveloperOptionsLabel = stringResource(R.string.developer_options_unlock_action),
        onOpen = { item ->
            if (item.isFirefoxExtensionsAction) {
                onOpenFirefoxExtensions?.invoke()
            } else {
                item.destination?.let(onDestinationChanged)
            }
        },
        onDismiss = onDismiss,
        developerOptionsUnlocked = developerOptionsUnlocked,
        onUnlockDeveloperOptions = onUnlockDeveloperOptions,
        header = {
            if (!isDefaultBrowser) {
                DefaultBrowserBanner(onMakeDefault = onOpenDefaultBrowserSettings)
            }
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

/** A row's words: a page's title and summary come from [SettingsRegistry]. */
@Composable
internal fun settingsHomeEntry(item: SettingsHomeItem, state: SettingsHomeState): SettingsHomeEntry {
    val destination = item.destination
    return when {
        item.isFirefoxExtensionsAction -> SettingsHomeEntry(
            item = item,
            title = stringResource(R.string.gecko_extensions_title),
            summary = stringResource(R.string.gecko_extensions_summary),
        )
        destination == SettingsDestination.DeveloperOptions -> SettingsHomeEntry(
            item = item,
            title = stringResource(R.string.developer_options_title),
            summary = stringResource(R.string.developer_options_summary),
        )
        // Only v5 with GeckoView has the page, so the registry and its search leave it out.
        destination == SettingsDestination.Translation -> SettingsHomeEntry(
            item = item,
            title = stringResource(R.string.v5_translation_settings_title),
            summary = stringResource(R.string.v5_translation_settings_summary),
        )
        destination != null -> SettingsHomeEntry(
            item = item,
            title = SettingsRegistry.page(destination)?.title?.let { stringResource(it) }.orEmpty(),
            summary = settingsHomeSummary(destination, state),
        )
        else -> SettingsHomeEntry(item = item, title = "", summary = null)
    }
}

@Composable
internal fun settingsHomeIcon(icon: SettingsHomeIcon): ImageVector = when (icon) {
    SettingsHomeIcon.Search -> VolaIcons.Search
    SettingsHomeIcon.Sync -> VolaIcons.Sync
    SettingsHomeIcon.Passwords -> VolaIcons.Key
    SettingsHomeIcon.TabsAndGestures -> VolaIcons.Tab
    SettingsHomeIcon.Appearance -> VolaIcons.Palette
    SettingsHomeIcon.Browser -> VolaIcons.Settings
    SettingsHomeIcon.Downloads -> VolaIcons.Download
    SettingsHomeIcon.Userscripts,
    SettingsHomeIcon.FirefoxExtensions,
    -> ImageVector.vectorResource(R.drawable.ic_symbol_extension)
    SettingsHomeIcon.SiteCapsules -> VolaIcons.Favorite
    SettingsHomeIcon.ProtectionAndData -> VolaIcons.Verified
    SettingsHomeIcon.DeveloperOptions -> VolaIcons.Build
    SettingsHomeIcon.AboutLegal -> VolaIcons.Info
    SettingsHomeIcon.Translation -> VolaIcons.Language
}

/**
 * «Make Vola your default» above the cards while Vola is not the default browser: the system's
 * default-apps screen opens, since Android lets only the user pick.
 */
@Composable
internal fun DefaultBrowserBanner(onMakeDefault: () -> Unit) {
    val stacked = SettingsHomeSummaryRules.stacksMakeDefault(LocalDensity.current.fontScale)
    val button: @Composable () -> Unit = {
        Button(
            onClick = onMakeDefault,
            modifier = Modifier.testTag(SettingsHomeTestTags.MakeDefault),
        ) {
            Text(stringResource(R.string.settings_home_default_action))
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsHomeTestTags.DefaultBrowserBanner),
        shape = VolaSettingsHomeTokens.bannerShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = VolaSettingsHomeTokens.bannerMinHeight)
                .padding(VolaSettingsHomeTokens.bannerPadding),
            verticalArrangement = Arrangement.spacedBy(VolaSettingsHomeTokens.bannerGap),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(VolaSettingsHomeTokens.bannerGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(VolaSettingsHomeTokens.logoTileSize)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            VolaSettingsHomeTokens.logoTileShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground_art),
                        contentDescription = null,
                        modifier = Modifier.size(VolaSettingsHomeTokens.logoSize),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(VolaSettingsHomeTokens.bannerTextGap),
                ) {
                    Text(
                        stringResource(R.string.settings_home_default_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.settings_home_default_summary),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                // A large font leaves too little room beside the text: the button goes under it.
                if (!stacked) button()
            }
            if (stacked) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) { button() }
            }
        }
    }
}
