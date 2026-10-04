package dev.sk2andy.materialbrowser.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

internal object PageErrorFeedbackTestTags {
    const val Page = "page_error_page"
    const val Retry = "page_error_retry"
    const val RetryProgress = "page_error_retry_progress"
    const val Offline = "page_error_offline"
    const val NetworkSettings = "page_error_network_settings"
}

/** The page in place of a site that failed to load (boards W-States, W-Offline). */
@Composable
internal fun PageErrorFeedback(
    state: PageErrorFeedbackState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    url: String = "",
    onBack: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = state !is PageErrorFeedbackState.Hidden,
        modifier = modifier,
        enter = fadeIn(VolaMotion.effects()),
        exit = fadeOut(VolaMotion.effects()),
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
                    fadeIn(VolaMotion.effects()) togetherWith fadeOut(VolaMotion.effects())
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
                    PageErrorFeedbackState.UnknownHost -> ProblemPage(
                        icon = VolaIcons.SearchOff,
                        title = stringResource(R.string.error_page_unreachable),
                        message = stringResource(R.string.page_error_unknown_host_message, host),
                        onRetry = onRetry,
                    )
                    is PageErrorFeedbackState.Error -> ProblemPage(
                        icon = VolaIcons.PublicOff,
                        title = stringResource(R.string.page_error_unreachable_title),
                        message = stringResource(R.string.page_error_unreachable_message, host),
                        onRetry = onRetry,
                    )
                    is PageErrorFeedbackState.Offline -> OfflinePage(onRetry = onRetry)
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

/** No connection (board W-Offline): the page reloads by itself when the connection is back. */
@Composable
private fun OfflinePage(onRetry: () -> Unit) {
    val context = LocalContext.current
    VolaStatePage(
        title = stringResource(R.string.page_error_offline_title),
        message = stringResource(R.string.page_error_offline_body),
        icon = { VolaStatePageIcon(icon = VolaIcons.WifiOff) },
        modifier = Modifier.testTag(PageErrorFeedbackTestTags.Offline),
        announce = true,
    ) {
        RetryButton(onRetry = onRetry)
        VolaStatePageSecondaryButton(
            text = stringResource(R.string.page_error_network_settings),
            onClick = { context.openNetworkSettings() },
            modifier = Modifier.testTag(PageErrorFeedbackTestTags.NetworkSettings),
        )
    }
}

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

/** The system's internet panel over the browser; it closes back to the page. */
private fun Context.openNetworkSettings() {
    runCatching {
        startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
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
