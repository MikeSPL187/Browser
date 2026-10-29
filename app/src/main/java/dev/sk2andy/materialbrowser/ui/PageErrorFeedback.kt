package dev.sk2andy.materialbrowser.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import dev.sk2andy.materialbrowser.ui.v5.V5WaveGlyph

internal object PageErrorFeedbackTestTags {
    const val Page = "page_error_page"
    const val Retry = "page_error_retry"
    const val RetryProgress = "page_error_retry_progress"
    const val Offline = "page_error_offline"
    const val CheckWifi = "page_error_check_wifi"
    const val CheckCellular = "page_error_check_cellular"
    const val Search = "page_error_search"
}

/** The page in place of a site that failed to load (boards W-States, W-Offline). */
@Composable
internal fun PageErrorFeedback(
    state: PageErrorFeedbackState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    url: String = "",
    onBack: (() -> Unit)? = null,
    /** Searches the typed words instead: a name that does not exist is rarely meant as an address. */
    onSearch: ((String) -> Unit)? = null,
) {
    // The page fades on V5Motion's springs, and only fades quickly with system animations off.
    val fade: FiniteAnimationSpec<Float> = when {
        V5Motion.animationsEnabled -> V5Motion.effects.spec()
        else -> V5Motion.reducedFade()
    }
    AnimatedVisibility(
        visible = state !is PageErrorFeedbackState.Hidden,
        modifier = modifier,
        enter = fadeIn(fade),
        exit = fadeOut(fade),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .testTag(PageErrorFeedbackTestTags.Page),
        ) {
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = {
                    fadeIn(fade) togetherWith fadeOut(fade)
                },
                label = "page problem",
                modifier = Modifier.fillMaxSize(),
            ) { current ->
                val host = PageErrorFeedbackRules.displayHost(url)
                when (current) {
                    PageErrorFeedbackState.NotFound -> ProblemPage(
                        icon = VolaIcons.LinkOff,
                        title = stringResource(R.string.page_error_not_found_title),
                        message = stringResource(R.string.page_error_not_found_message, host),
                        onRetry = onRetry,
                    )
                    PageErrorFeedbackState.InsecureConnection -> InsecureConnectionPage(
                        host = host,
                        onBack = onBack,
                        onRetry = onRetry,
                    )
                    PageErrorFeedbackState.UnknownHost -> if (onSearch != null && host.isNotBlank()) {
                        UnknownHostPage(host = host, onSearch = { onSearch(host) }, onRetry = onRetry)
                    } else {
                        ProblemPage(
                            icon = VolaIcons.SearchOff,
                            title = stringResource(R.string.error_page_unreachable),
                            message = stringResource(R.string.page_error_unknown_host_message, host),
                            onRetry = onRetry,
                        )
                    }
                    is PageErrorFeedbackState.Error -> ProblemPage(
                        icon = VolaIcons.PublicOff,
                        title = stringResource(R.string.page_error_unreachable_title),
                        message = stringResource(R.string.page_error_unreachable_message, host),
                        onRetry = onRetry,
                    )
                    is PageErrorFeedbackState.Offline -> OfflinePage(host = host, onRetry = onRetry)
                    PageErrorFeedbackState.Retrying -> RetryingPage()
                    PageErrorFeedbackState.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
private fun ProblemPage(
    icon: ImageVector,
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    VolaStatePage(
        title = title,
        message = message,
        icon = { VolaStatePageIcon(icon = icon) },
    ) {
        RetryButton(onRetry = onRetry)
    }
}

/**
 * No site has this name: what was typed was most likely a search («file.txt», a typo), so searching
 * it comes first; trying the same name again rarely helps, but stays for a network that failed.
 */
@Composable
private fun UnknownHostPage(host: String, onSearch: () -> Unit, onRetry: () -> Unit) {
    VolaStatePage(
        title = stringResource(R.string.error_page_unreachable),
        message = stringResource(R.string.page_error_unknown_host_message, host),
        icon = { VolaStatePageIcon(icon = VolaIcons.SearchOff) },
    ) {
        VolaStatePagePrimaryButton(
            text = stringResource(R.string.page_error_search_for, host),
            onClick = onSearch,
            icon = VolaIcons.Search,
            modifier = Modifier.testTag(PageErrorFeedbackTestTags.Search),
        )
        VolaStatePageTextButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetry,
            modifier = Modifier.testTag(PageErrorFeedbackTestTags.Retry),
        )
    }
}

/**
 * «Insecure connection» (board W-States): the way back comes first. There is deliberately no way
 * past a bad certificate.
 */
@Composable
private fun InsecureConnectionPage(host: String, onBack: (() -> Unit)?, onRetry: () -> Unit) {
    VolaStatePage(
        title = stringResource(R.string.page_error_insecure_title),
        message = stringResource(R.string.page_error_insecure_message, host),
        icon = { VolaStatePageIcon(icon = VolaIcons.GppBad, tone = VolaStatePageTone.Error) },
    ) {
        if (onBack == null) {
            RetryButton(onRetry = onRetry)
        } else {
            VolaStatePagePrimaryButton(
                text = stringResource(R.string.page_error_go_back),
                onClick = onBack,
                icon = VolaIcons.ArrowBack,
            )
            VolaStatePageTextButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                modifier = Modifier.testTag(PageErrorFeedbackTestTags.Retry),
            )
        }
    }
}

/**
 * No connection (board W-Offline): the page reloads by itself when the connection is back. The
 * glyph waits inside rings (canvas «Error»), the sentence names the site, and «What to check»
 * says how Wi-Fi and the mobile network stand, each opening its system panel.
 */
@Composable
private fun OfflinePage(host: String, onRetry: () -> Unit) {
    val context = LocalContext.current
    VolaStatePage(
        title = stringResource(R.string.page_error_offline_title),
        message = if (host.isNotBlank()) {
            stringResource(R.string.page_error_offline_body_host, host)
        } else {
            stringResource(R.string.page_error_offline_body)
        },
        icon = { VolaStatePageWaves(icon = VolaIcons.WifiOff) },
        modifier = Modifier.testTag(PageErrorFeedbackTestTags.Offline),
        announce = true,
        details = {
            NetworkChecksGroup(rememberNetworkChecks(), onOpen = { panel -> context.openPanel(panel) })
        },
    ) {
        RetryButton(onRetry = onRetry)
        // The rows of «What to check» open the system panels themselves.
    }
}

/** «What to check»: Wi-Fi and the mobile network with how each stands, as v5 list rows. */
@Composable
private fun NetworkChecksGroup(checks: NetworkChecks, onOpen: (String) -> Unit) {
    V5ListGroup(
        modifier = Modifier.padding(top = V5Spacing.space6),
        header = stringResource(R.string.page_error_check_title),
    ) {
        V5ListRow(
            title = stringResource(R.string.network_wifi),
            icon = V5RowIcon(VolaIcons.Wifi, tile = V5Tile.Blue),
            value = networkLinkText(checks.wifi),
            onClick = { onOpen(Settings.Panel.ACTION_WIFI) },
            modifier = Modifier.testTag(PageErrorFeedbackTestTags.CheckWifi),
        )
        V5ListRow(
            title = stringResource(R.string.network_cellular),
            icon = V5RowIcon(VolaIcons.SignalCellular, tile = V5Tile.Green),
            value = networkLinkText(checks.cellular),
            onClick = { onOpen(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) },
            modifier = Modifier.testTag(PageErrorFeedbackTestTags.CheckCellular),
        )
    }
}

@Composable
private fun networkLinkText(link: NetworkLink): String = stringResource(
    when (link) {
        NetworkLink.Connected -> R.string.network_link_connected
        NetworkLink.NoInternet -> R.string.network_link_no_internet
        NetworkLink.NotConnected -> R.string.network_link_not_connected
    },
)

@Composable
private fun RetryButton(onRetry: () -> Unit) {
    VolaStatePagePrimaryButton(
        text = stringResource(R.string.action_retry),
        onClick = onRetry,
        icon = VolaIcons.Refresh,
        modifier = Modifier.testTag(PageErrorFeedbackTestTags.Retry),
    )
}

@Composable
private fun RetryingPage() {
    VolaStatePage(
        title = stringResource(R.string.action_retrying),
        message = null,
        icon = { VolaStatePageProgress() },
        modifier = Modifier.testTag(PageErrorFeedbackTestTags.RetryProgress),
        announce = true,
    )
}

/** A system settings panel over the browser (internet or Wi-Fi); it closes back to the page. */
private fun Context.openPanel(action: String) {
    runCatching {
        startActivity(Intent(action))
    }
}

@VolaPreviews
@Composable
private fun OfflinePagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        PageErrorFeedback(state = PageErrorFeedbackState.Offline(), onRetry = {})
    }
}

@VolaPreviews
@Composable
private fun InsecureConnectionPagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        PageErrorFeedback(
            state = PageErrorFeedbackState.InsecureConnection,
            onRetry = {},
            url = "https://expired.badssl.com/",
            onBack = {},
        )
    }
}

@VolaPreviews
@Composable
private fun UnreachablePagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        PageErrorFeedback(
            state = PageErrorFeedbackState.Error("timeout"),
            onRetry = {},
            url = "https://north-guide.ru/routes",
        )
    }
}

@VolaPreviews
@Composable
private fun UnknownHostSearchPagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        PageErrorFeedback(
            state = PageErrorFeedbackState.UnknownHost,
            onRetry = {},
            url = "https://file.txt/",
            onSearch = {},
        )
    }
}

/** The v5 parts of «No connection» (canvas «Error»): the glyph in its rings and «What to check». */
@VolaPreviews
@Composable
private fun V5OfflineChecksPreview() {
    VolaV5Theme(dark = isSystemInDarkTheme()) {
        Column(
            Modifier.background(V5Theme.colors.background).padding(vertical = V5Spacing.space6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            V5WaveGlyph(VolaIcons.WifiOff)
            NetworkChecksGroup(
                NetworkChecks(wifi = NetworkLink.NotConnected, cellular = NetworkLink.NoInternet),
                onOpen = {},
            )
        }
    }
}
