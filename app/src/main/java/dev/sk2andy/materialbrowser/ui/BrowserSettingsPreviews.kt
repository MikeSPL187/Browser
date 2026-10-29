package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.PageTranslationProvider
import dev.sk2andy.materialbrowser.ui.v5.ProvideV5SettingsSkin
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** Browser settings on cards; video autoplay blocking is out of reach of this engine. */
@VolaPreviews
@Composable
private fun BrowserSettingsPreview() {
    MaterialBrowserTheme {
        BrowserSettingsPage(
            pageTranslationProvider = PageTranslationProvider.Kagi,
            isExternalLinkPreviewEnabled = true,
            isFullImmersiveModeEnabled = false,
            isStartupAnimationEnabled = true,
            isOpenHomeOnStartupEnabled = true,
            isScrollBarEnabled = false,
            isVideoAutoplayBlocked = false,
            isVideoAutoplayBlockingSupported = false,
            isDefaultBrowser = true,
            onFullImmersiveModeEnabledChanged = {},
            onStartupAnimationEnabledChanged = {},
            onScrollBarEnabledChanged = {},
            onVideoAutoplayBlockedChanged = {},
            onPageTranslationProviderChanged = {},
            onOpenDefaultBrowserSettings = {},
            onBack = {},
        )
    }
}

/** V15d-4 under ui.v5: the main layer (with «More») and the More page, drawn by the v5 skin. */
@Composable
private fun BrowserSettingsV5Preview(layer: SettingsLayer) {
    MaterialBrowserTheme {
        ProvideV5SettingsSkin() {
            BrowserSettingsPage(
                layer = layer,
                pageTranslationProvider = PageTranslationProvider.Kagi,
                isExternalLinkPreviewEnabled = true,
                isFullImmersiveModeEnabled = false,
                isStartupAnimationEnabled = true,
                isScrollBarEnabled = false,
                isVideoAutoplayBlocked = true,
                isVideoAutoplayBlockingSupported = true,
                isInlineMediaPlayerSupported = false,
                isDefaultBrowser = false,
                onFullImmersiveModeEnabledChanged = {},
                onStartupAnimationEnabledChanged = {},
                onScrollBarEnabledChanged = {},
                onVideoAutoplayBlockedChanged = {},
                onPageTranslationProviderChanged = {},
                onOpenDefaultBrowserSettings = {},
                onBack = {},
            )
        }
    }
}

@VolaPreviews
@Composable
private fun BrowserSettingsV5MainPreview() {
    BrowserSettingsV5Preview(SettingsLayer.Main)
}

@VolaPreviews
@Composable
private fun BrowserSettingsV5MorePreview() {
    BrowserSettingsV5Preview(SettingsLayer.More)
}
