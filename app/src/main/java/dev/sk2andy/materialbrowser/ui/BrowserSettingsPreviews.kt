package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.PageTranslationProvider
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
