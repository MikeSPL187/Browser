package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5DangerousSite

internal object DangerousSiteTestTags {
    const val Page = "dangerous_site_page"
    const val Back = "dangerous_site_back"
    const val OpenAnyway = "dangerous_site_open_anyway"
}

/**
 * «This site pretends to be another» or «Known dangerous site» (board W-DangerousSite), in place of
 * a navigation that [dev.sk2andy.materialbrowser.browser.safety.DangerousSiteGuard] stopped before
 * anything loaded. The page is in the danger color; «Back to safety» leaves the tab where it was.
 */
@Composable
internal fun DangerousSitePage(
    site: BlockedSite,
    onBackToSafety: () -> Unit,
    onOpenAnyway: () -> Unit,
    onOpenRealSite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Under v5 the page is the canvas «Danger» screen (owner 17.10: the phone all v5); the current
    // interface paints the page red.
    VolaV5Theme(dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE) {
        V5DangerousSite(site, onBackToSafety, onOpenAnyway, onOpenRealSite, modifier)
    }
}

private const val DARK_SURFACE_LUMINANCE = 0.5f

@VolaPreviews
@Composable
private fun DangerousSitePagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        DangerousSitePage(
            site = BlockedSite(
                url = "https://bank-exarnple.ru/",
                host = "bank-exarnple.ru",
                imitatedHost = "bank.example.ru",
            ),
            onBackToSafety = {},
            onOpenAnyway = {},
            onOpenRealSite = {},
        )
    }
}

@VolaPreviews
@Composable
private fun ListedDangerousSitePagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        DangerousSitePage(
            site = BlockedSite(url = "https://login-bank-help.top/", host = "login-bank-help.top"),
            onBackToSafety = {},
            onOpenAnyway = {},
            onOpenRealSite = {},
        )
    }
}

@VolaPreviews
@Composable
private fun SafeBrowsingDangerousSitePagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        DangerousSitePage(
            site = BlockedSite(
                url = "https://login-bank-help.top/",
                host = "login-bank-help.top",
                reportedBySafeBrowsing = true,
            ),
            onBackToSafety = {},
            onOpenAnyway = {},
            onOpenRealSite = {},
        )
    }
}
