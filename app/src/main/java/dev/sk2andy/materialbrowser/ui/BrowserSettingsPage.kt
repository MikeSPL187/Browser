package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.BuildConfig
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import dev.sk2andy.materialbrowser.browser.ExternalAppLinkHandling
import dev.sk2andy.materialbrowser.browser.InlineMediaPlayerMode
import dev.sk2andy.materialbrowser.browser.PageTranslationProvider
import dev.sk2andy.materialbrowser.browser.StartupAddressFocusMode
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCard
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardHeader
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardLinkRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSwitchRow
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsCardTokens
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

internal object BrowserSettingsTestTags {
    const val StartupAnimation = "browser_settings_startup_animation"
    const val StartupAddressFocus = "browser_settings_startup_address_focus"
    const val OpenHomeOnStartup = "browser_settings_open_home_on_startup"
    const val ScrollBar = "browser_settings_scroll_bar"
    const val TranslationProvider = "browser_settings_translation_provider"
    const val ExternalLinkPreview = "browser_settings_external_link_preview"
    const val ExternalAppLinks = "browser_settings_external_app_links"
    const val BrowserEngine = "browser_settings_engine"
    const val InlineMediaPlayer = "browser_settings_inline_media_player"
}

/** Browser settings on cards (board W-Settings): general, startup, favorites, sites, links. */
@Composable
internal fun BrowserSettingsPage(
    browserEngineKind: AndroidBrowserEngineKind = AndroidBrowserEngineKind.GeckoView,
    pageTranslationProvider: PageTranslationProvider,
    isExternalLinkPreviewEnabled: Boolean = false,
    externalAppLinkHandling: ExternalAppLinkHandling = ExternalAppLinkHandling.Default,
    isFullImmersiveModeEnabled: Boolean,
    isStartupAnimationEnabled: Boolean,
    startupAddressFocusMode: StartupAddressFocusMode = StartupAddressFocusMode.Default,
    isOpenHomeOnStartupEnabled: Boolean = false,
    isScrollBarEnabled: Boolean,
    isVideoAutoplayBlocked: Boolean,
    isVideoAutoplayBlockingSupported: Boolean,
    inlineMediaPlayerMode: InlineMediaPlayerMode = InlineMediaPlayerMode.Default,
    isInlineMediaPlayerSupported: Boolean = true,
    isDefaultBrowser: Boolean,
    onBrowserEngineKindChanged: (AndroidBrowserEngineKind) -> Unit = {},
    onExternalLinkPreviewEnabledChanged: (Boolean) -> Unit = {},
    onExternalAppLinkHandlingChanged: (ExternalAppLinkHandling) -> Unit = {},
    onFullImmersiveModeEnabledChanged: (Boolean) -> Unit,
    onStartupAnimationEnabledChanged: (Boolean) -> Unit,
    onStartupAddressFocusModeChanged: (StartupAddressFocusMode) -> Unit = {},
    onImportFavoriteBookmarks: () -> Unit = {},
    onOpenHomeOnStartupEnabledChanged: (Boolean) -> Unit = {},
    onScrollBarEnabledChanged: (Boolean) -> Unit,
    onVideoAutoplayBlockedChanged: (Boolean) -> Unit,
    onInlineMediaPlayerModeChanged: (InlineMediaPlayerMode) -> Unit = {},
    onPageTranslationProviderChanged: (PageTranslationProvider) -> Unit,
    onOpenDefaultBrowserSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val cardColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh)
    val dividerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    SettingsPage(
        title = stringResource(R.string.settings_section_browser),
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.cardGap)) {
            SettingsCardHeader(stringResource(R.string.settings_browser_group_general))
            SettingsCard(containerColor = cardColor) {
                if (!BuildConfig.SYSTEM_WEBVIEW_ONLY) {
                    SettingsCardDropdownRow(
                        title = stringResource(R.string.settings_browser_engine_title),
                        selected = browserEngineKind,
                        options = AndroidBrowserEngineKind.entries,
                        label = { kind -> kind.displayName() },
                        summary = stringResource(
                            when (browserEngineKind) {
                                AndroidBrowserEngineKind.GeckoView ->
                                    R.string.settings_browser_engine_gecko_summary
                                AndroidBrowserEngineKind.SystemWebView ->
                                    R.string.settings_browser_engine_system_summary
                            },
                        ),
                        dividerColor = dividerColor,
                        divider = true,
                        onSelected = onBrowserEngineKindChanged,
                        modifier = Modifier.testTag(BrowserSettingsTestTags.BrowserEngine),
                    )
                }
                SettingsCardLinkRow(
                    title = stringResource(R.string.settings_default_browser),
                    summary = stringResource(
                        if (isDefaultBrowser) {
                            R.string.settings_default_browser_active
                        } else {
                            R.string.settings_make_default_browser
                        },
                    ),
                    dividerColor = dividerColor,
                    onClick = onOpenDefaultBrowserSettings,
                )
            }
            if (!BuildConfig.SYSTEM_WEBVIEW_ONLY) {
                // Switching the engine restarts Vola; said under the card, in the error color.
                Text(
                    text = stringResource(R.string.settings_browser_engine_restart_warning),
                    modifier = Modifier.padding(SettingsCardTokens.headerPadding),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SettingsCardHeader(stringResource(R.string.settings_browser_group_startup))
            SettingsCard(containerColor = cardColor) {
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_startup_animation_title),
                    summary = stringResource(R.string.settings_startup_animation_subtitle),
                    checked = isStartupAnimationEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    divider = true,
                    onCheckedChange = onStartupAnimationEnabledChanged,
                    modifier = Modifier.testTag(BrowserSettingsTestTags.StartupAnimation),
                )
                SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_startup_address_focus_title),
                    selected = startupAddressFocusMode,
                    options = StartupAddressFocusMode.entries,
                    label = { mode -> mode.displayName() },
                    summary = stringResource(R.string.settings_startup_address_focus_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onSelected = onStartupAddressFocusModeChanged,
                    modifier = Modifier.testTag(BrowserSettingsTestTags.StartupAddressFocus),
                )
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_open_home_on_startup_title),
                    summary = stringResource(R.string.settings_open_home_on_startup_subtitle),
                    checked = isOpenHomeOnStartupEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    onCheckedChange = onOpenHomeOnStartupEnabledChanged,
                    modifier = Modifier.testTag(BrowserSettingsTestTags.OpenHomeOnStartup),
                )
            }
            SettingsCardHeader(stringResource(R.string.settings_browser_group_favorites))
            SettingsCard(containerColor = cardColor) {
                SettingsCardLinkRow(
                    title = stringResource(R.string.settings_favorite_bookmark_import_title),
                    summary = stringResource(R.string.settings_favorite_bookmark_import_summary),
                    dividerColor = dividerColor,
                    onClick = onImportFavoriteBookmarks,
                )
            }
            SettingsCardHeader(stringResource(R.string.settings_browser_group_websites))
            SettingsCard(containerColor = cardColor) {
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_full_immersive_mode_title),
                    summary = stringResource(R.string.settings_full_immersive_mode_subtitle),
                    checked = isFullImmersiveModeEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    divider = true,
                    onCheckedChange = onFullImmersiveModeEnabledChanged,
                )
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_scroll_bar_title),
                    summary = stringResource(R.string.settings_scroll_bar_subtitle),
                    checked = isScrollBarEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    divider = true,
                    onCheckedChange = onScrollBarEnabledChanged,
                    modifier = Modifier.testTag(BrowserSettingsTestTags.ScrollBar),
                )
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_video_autoplay_title),
                    summary = stringResource(
                        if (isVideoAutoplayBlockingSupported) {
                            R.string.settings_video_autoplay_subtitle
                        } else {
                            R.string.settings_video_autoplay_unsupported
                        },
                    ),
                    checked = isVideoAutoplayBlocked,
                    summaryMaxLines = Int.MAX_VALUE,
                    enabled = isVideoAutoplayBlockingSupported,
                    dividerColor = dividerColor,
                    divider = true,
                    onCheckedChange = onVideoAutoplayBlockedChanged,
                )
                SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_inline_media_player_title),
                    selected = inlineMediaPlayerMode,
                    options = InlineMediaPlayerMode.entries,
                    label = { mode -> mode.displayName() },
                    summary = stringResource(
                        if (isInlineMediaPlayerSupported) {
                            R.string.settings_inline_media_player_subtitle
                        } else {
                            R.string.settings_inline_media_player_unsupported
                        },
                    ),
                    enabled = isInlineMediaPlayerSupported,
                    dividerColor = dividerColor,
                    divider = true,
                    onSelected = onInlineMediaPlayerModeChanged,
                    modifier = Modifier.testTag(BrowserSettingsTestTags.InlineMediaPlayer),
                )
                SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_translation_provider),
                    selected = pageTranslationProvider,
                    options = PageTranslationProvider.entries,
                    label = { provider -> provider.displayName },
                    summary = stringResource(translationSummary(pageTranslationProvider)),
                    dividerColor = dividerColor,
                    onSelected = onPageTranslationProviderChanged,
                    modifier = Modifier.testTag(BrowserSettingsTestTags.TranslationProvider),
                )
            }
            SettingsCardHeader(stringResource(R.string.settings_browser_group_links))
            SettingsCard(containerColor = cardColor) {
                SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_external_app_links_title),
                    selected = externalAppLinkHandling,
                    options = ExternalAppLinkHandling.entries,
                    label = { handling -> handling.displayName() },
                    summary = stringResource(R.string.settings_external_app_links_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onSelected = onExternalAppLinkHandlingChanged,
                    modifier = Modifier.testTag(BrowserSettingsTestTags.ExternalAppLinks),
                )
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_external_link_preview_title),
                    summary = stringResource(R.string.settings_external_link_preview_subtitle),
                    checked = isExternalLinkPreviewEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    onCheckedChange = onExternalLinkPreviewEnabledChanged,
                    modifier = Modifier.testTag(BrowserSettingsTestTags.ExternalLinkPreview),
                )
            }
        }
    }
}

/** What the chosen translation service does with the page. */
private fun translationSummary(provider: PageTranslationProvider): Int = when (provider) {
    PageTranslationProvider.Google -> R.string.settings_translation_provider_google_summary
    PageTranslationProvider.Yandex -> R.string.settings_translation_provider_summary
    PageTranslationProvider.Kagi -> R.string.settings_translation_provider_kagi_summary
}

@Composable
private fun AndroidBrowserEngineKind.displayName(): String = when (this) {
    AndroidBrowserEngineKind.GeckoView ->
        stringResource(R.string.settings_browser_engine_gecko)
    AndroidBrowserEngineKind.SystemWebView ->
        stringResource(R.string.settings_browser_engine_system)
}

@Composable
private fun StartupAddressFocusMode.displayName(): String = when (this) {
    StartupAddressFocusMode.WhenStartupAnimationDisabled ->
        stringResource(R.string.settings_startup_address_focus_current)
    StartupAddressFocusMode.Always ->
        stringResource(R.string.settings_startup_address_focus_always)
    StartupAddressFocusMode.Never ->
        stringResource(R.string.settings_startup_address_focus_never)
}

@Composable
private fun ExternalAppLinkHandling.displayName(): String = when (this) {
    ExternalAppLinkHandling.Automatic ->
        stringResource(R.string.settings_external_app_links_automatic)
    ExternalAppLinkHandling.AskEveryTime ->
        stringResource(R.string.settings_external_app_links_ask_every_time)
}

@Composable
private fun InlineMediaPlayerMode.displayName(): String = when (this) {
    InlineMediaPlayerMode.ButtonFullscreen ->
        stringResource(R.string.settings_inline_media_player_mode_button_fullscreen)
    InlineMediaPlayerMode.ButtonInlineAndFullscreen ->
        stringResource(R.string.settings_inline_media_player_mode_button_inline_fullscreen)
    InlineMediaPlayerMode.AlwaysForFullscreen ->
        stringResource(R.string.settings_inline_media_player_mode_always_fullscreen)
    InlineMediaPlayerMode.Automatic ->
        stringResource(R.string.settings_inline_media_player_mode_automatic)
}
