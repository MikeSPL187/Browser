package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaDangerousSite
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

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
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < DARK_SURFACE_LUMINANCE
    val background = if (dark) colors.errorContainer else colors.error
    val content = if (dark) colors.onErrorContainer else colors.onError
    val secondary = content.copy(alpha = VolaDangerousSite.SECONDARY_TEXT_ALPHA)
    // The state page reads its colors from the theme: here the danger color is the surface.
    MaterialTheme(
        colorScheme = colors.copy(
            surface = background,
            onSurface = content,
            onSurfaceVariant = secondary,
            primary = content,
            onPrimary = background,
            primaryContainer = content,
            onPrimaryContainer = background,
        ),
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .testTag(DangerousSiteTestTags.Page),
            color = background,
            contentColor = content,
        ) {
            val imitatedHost = site.imitatedHost
            VolaStatePage(
                title = stringResource(
                    if (imitatedHost != null) R.string.dangerous_site_title else R.string.dangerous_site_listed_title,
                ),
                message = when {
                    imitatedHost != null ->
                        stringResource(R.string.dangerous_site_message, site.host, imitatedHost)
                    site.reportedBySafeBrowsing ->
                        stringResource(R.string.dangerous_site_safe_browsing_message, site.host)
                    else -> stringResource(R.string.dangerous_site_listed_message, site.host)
                },
                icon = {
                    VolaStatePageIcon(icon = VolaIcons.Dangerous, tone = VolaStatePageTone.Accent)
                },
                details = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = VolaDangerousSite.cardShape,
                        color = content.copy(alpha = VolaDangerousSite.CARD_ALPHA),
                        contentColor = content,
                    ) {
                        Row(
                            modifier = Modifier
                                .then(
                                    if (imitatedHost != null) {
                                        Modifier.clickable(role = Role.Button, onClick = onOpenRealSite)
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(VolaDangerousSite.cardPadding),
                            horizontalArrangement = Arrangement.spacedBy(VolaDangerousSite.rowGap),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (imitatedHost != null) VolaIcons.Verified else VolaIcons.ShieldLock,
                                contentDescription = null,
                                modifier = Modifier.size(VolaDangerousSite.rowIconSize),
                            )
                            Text(
                                text = when {
                                    imitatedHost != null ->
                                        stringResource(R.string.dangerous_site_real, imitatedHost)
                                    site.reportedBySafeBrowsing ->
                                        stringResource(R.string.dangerous_site_safe_browsing_source)
                                    else -> stringResource(R.string.dangerous_site_listed_local)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                },
            ) {
                VolaStatePagePrimaryButton(
                    text = stringResource(R.string.dangerous_site_back),
                    onClick = onBackToSafety,
                    modifier = Modifier.testTag(DangerousSiteTestTags.Back),
                )
                // The engine refuses a site Safe Browsing reported, so there is no way through.
                if (!site.reportedBySafeBrowsing) {
                    VolaStatePageTextButton(
                        text = stringResource(R.string.dangerous_site_open_anyway),
                        onClick = onOpenAnyway,
                        modifier = Modifier.testTag(DangerousSiteTestTags.OpenAnyway),
                    )
                }
            }
        }
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
