package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

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
    // Under v5 the page is one of v5's state screens (owner 17.10: the phone all v5): the danger is
    // in the glyph and in the red «Open anyway»; the current interface paints the page red.
    if (rememberDrawsV5()) {
        Box(modifier.fillMaxSize().testTag(DangerousSiteTestTags.Page)) {
            DangerousSiteContent(site, onBackToSafety, onOpenAnyway, onOpenRealSite, v5 = true)
        }
        return
    }
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
            DangerousSiteContent(site, onBackToSafety, onOpenAnyway, onOpenRealSite, v5 = false)
        }
    }
}

@Composable
private fun DangerousSiteContent(
    site: BlockedSite,
    onBackToSafety: () -> Unit,
    onOpenAnyway: () -> Unit,
    onOpenRealSite: () -> Unit,
    v5: Boolean,
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
            VolaStatePageIcon(
                icon = VolaIcons.Dangerous,
                tone = if (v5) VolaStatePageTone.Error else VolaStatePageTone.Accent,
            )
        },
        details = {
            val fact = when {
                imitatedHost != null -> stringResource(R.string.dangerous_site_real, imitatedHost)
                site.reportedBySafeBrowsing -> stringResource(R.string.dangerous_site_safe_browsing_source)
                else -> stringResource(R.string.dangerous_site_listed_local)
            }
            val factIcon = if (imitatedHost != null) VolaIcons.Verified else VolaIcons.ShieldLock
            val onFact = onOpenRealSite.takeIf { imitatedHost != null }
            if (v5) V5SiteFact(fact, factIcon, onFact) else SiteFact(fact, factIcon, onFact)
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
                destructive = true,
            )
        }
    }
}

/** The fact under the message on the red page: the real site to go to, or who listed this one. */
@Composable
private fun SiteFact(text: String, icon: ImageVector, onClick: (() -> Unit)?) {
    val content = LocalContentColor.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = VolaDangerousSite.cardShape,
        color = content.copy(alpha = VolaDangerousSite.CARD_ALPHA),
        contentColor = content,
    ) {
        Row(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(VolaDangerousSite.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(VolaDangerousSite.rowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(VolaDangerousSite.rowIconSize))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The same fact as a v5 row on its card. */
@Composable
private fun V5SiteFact(text: String, icon: ImageVector, onClick: (() -> Unit)?) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.m)
            .background(colors.surface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.iconSmall))
        Text(text, style = V5Theme.type.callout, color = colors.label, modifier = Modifier.weight(1f))
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
