package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.LinkPeekPreviewStatus
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons

internal object LinkPeekPreviewMessageTags {
    const val Message = "link_peek_preview_message"
    const val Close = "link_peek_preview_message_close"
    const val Retry = "link_peek_preview_message_retry"
}

/** What the preview's engine reports back to the Glance card. */
internal class LinkPeekPreviewCallbacks(
    val onProgressChanged: (Int) -> Unit,
    val onCommittedUrlChanged: (String) -> Unit,
    val onStatusChanged: (LinkPeekPreviewStatus) -> Unit = {},
    val onTitleChanged: (String?) -> Unit = {},
)

/** True when the card shows a message in place of the page. */
internal val LinkPeekPreviewStatus.coversPage: Boolean
    get() = this != LinkPeekPreviewStatus.Loading && this != LinkPeekPreviewStatus.Loaded

/**
 * In place of a preview that can't be shown. A failed or crashed page offers Retry, which builds
 * a fresh preview; nothing here goes past a certificate, HTTPS-only or Safe Browsing refusal. A
 * dangerous site gets the tab's warning, but only a way back: «Open anyway» stays on the tab's
 * own warning page.
 */
@Composable
internal fun LinkPeekPreviewMessage(
    status: LinkPeekPreviewStatus,
    host: String,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!status.coversPage) return
    Surface(
        modifier = modifier.testTag(LinkPeekPreviewMessageTags.Message),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        when (status) {
            is LinkPeekPreviewStatus.Blocked -> BlockedPreviewMessage(status, host, onClose)
            is LinkPeekPreviewStatus.Failed -> FailedPreviewMessage(status, host, onRetry, onClose)
            LinkPeekPreviewStatus.Crashed -> {
                val text = requireNotNull(linkPeekMessageText(status, host))
                RetryablePreviewMessage(
                    title = text.title,
                    message = text.body,
                    icon = VolaIcons.Error,
                    onRetry = onRetry,
                    onClose = onClose,
                )
            }
            LinkPeekPreviewStatus.Loading, LinkPeekPreviewStatus.Loaded -> Unit
        }
    }
}

/** The words for a preview that can't be shown, shared by the v4 card and the v5 one (V21). */
internal data class LinkPeekMessageText(
    val title: String,
    val body: String,
    val danger: Boolean,
    val retryable: Boolean,
)

/** What to say in place of the page, or null while the page loads or shows. */
@Composable
internal fun linkPeekMessageText(status: LinkPeekPreviewStatus, host: String): LinkPeekMessageText? =
    when (status) {
        is LinkPeekPreviewStatus.Blocked -> {
            val site = status.site
            val imitatedHost = site.imitatedHost
            LinkPeekMessageText(
                title = stringResource(
                    if (imitatedHost != null) R.string.dangerous_site_title else R.string.dangerous_site_listed_title,
                ),
                body = when {
                    imitatedHost != null ->
                        stringResource(R.string.dangerous_site_message, site.host, imitatedHost)
                    site.reportedBySafeBrowsing ->
                        stringResource(R.string.dangerous_site_safe_browsing_message, site.host)
                    else -> stringResource(R.string.dangerous_site_listed_message, site.host)
                },
                danger = true,
                retryable = false,
            )
        }
        is LinkPeekPreviewStatus.Failed -> {
            val (title, body) = when (status.kind) {
                BrowserEngineFailureKind.Offline ->
                    stringResource(R.string.page_error_offline_title) to
                        stringResource(R.string.glance_failed_message, host)
                BrowserEngineFailureKind.UnknownHost ->
                    stringResource(R.string.error_page_unreachable) to
                        stringResource(R.string.page_error_unknown_host_message, host)
                BrowserEngineFailureKind.InsecureConnection,
                BrowserEngineFailureKind.CertificateWarning,
                ->
                    stringResource(R.string.page_error_insecure_title) to
                        stringResource(R.string.page_error_insecure_message, host)
                BrowserEngineFailureKind.HttpsOnly ->
                    stringResource(R.string.glance_https_only_title) to
                        stringResource(R.string.glance_https_only_message, host)
                else ->
                    stringResource(R.string.glance_failed_title) to
                        stringResource(R.string.glance_failed_message, host)
            }
            LinkPeekMessageText(title, body, danger = false, retryable = true)
        }
        LinkPeekPreviewStatus.Crashed -> LinkPeekMessageText(
            title = stringResource(R.string.glance_crashed_title),
            body = stringResource(R.string.glance_crashed_message),
            danger = false,
            retryable = true,
        )
        LinkPeekPreviewStatus.Loading, LinkPeekPreviewStatus.Loaded -> null
    }

@Composable
private fun FailedPreviewMessage(
    status: LinkPeekPreviewStatus.Failed,
    host: String,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val text = requireNotNull(linkPeekMessageText(status, host))
    RetryablePreviewMessage(
        title = text.title,
        message = text.body,
        icon = when (status.kind) {
            BrowserEngineFailureKind.Offline -> VolaIcons.WifiOff
            BrowserEngineFailureKind.InsecureConnection,
            BrowserEngineFailureKind.CertificateWarning,
            BrowserEngineFailureKind.HttpsOnly,
            -> VolaIcons.GppBad
            else -> VolaIcons.PublicOff
        },
        onRetry = onRetry,
        onClose = onClose,
    )
}

@Composable
private fun RetryablePreviewMessage(
    title: String,
    message: String,
    icon: ImageVector,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    VolaStatePage(
        title = title,
        message = message,
        icon = { VolaStatePageIcon(icon = icon) },
        announce = true,
    ) {
        VolaStatePagePrimaryButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetry,
            modifier = Modifier.testTag(LinkPeekPreviewMessageTags.Retry),
            icon = VolaIcons.Refresh,
        )
        VolaStatePageTextButton(
            text = stringResource(R.string.glance_close),
            onClick = onClose,
            modifier = Modifier.testTag(LinkPeekPreviewMessageTags.Close),
        )
    }
}

@Composable
private fun BlockedPreviewMessage(status: LinkPeekPreviewStatus.Blocked, host: String, onClose: () -> Unit) {
    val text = requireNotNull(linkPeekMessageText(status, host))
    VolaStatePage(
        title = text.title,
        message = text.body,
        icon = { VolaStatePageIcon(icon = VolaIcons.Dangerous, tone = VolaStatePageTone.Error) },
        announce = true,
    ) {
        VolaStatePagePrimaryButton(
            text = stringResource(R.string.glance_close),
            onClick = onClose,
            modifier = Modifier.testTag(LinkPeekPreviewMessageTags.Close),
        )
    }
}
