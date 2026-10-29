package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.LinkPeekPreviewStatus
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind

/** True when the card shows a message in place of the page. */
internal val LinkPeekPreviewStatus.coversPage: Boolean
    get() = this != LinkPeekPreviewStatus.Loading && this != LinkPeekPreviewStatus.Loaded

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
