package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.LinkPeekPreviewStatus
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons

internal object LinkPeekPreviewMessageTags {
    const val Message = "link_peek_preview_message"
    const val Close = "link_peek_preview_message_close"
}

/** What the preview's engine reports back to the Glance card. */
internal class LinkPeekPreviewCallbacks(
    val onProgressChanged: (Int) -> Unit,
    val onCommittedUrlChanged: (String) -> Unit,
    val onStatusChanged: (LinkPeekPreviewStatus) -> Unit = {},
)

/** True when the card shows a message in place of the page. */
internal val LinkPeekPreviewStatus.coversPage: Boolean
    get() = this is LinkPeekPreviewStatus.Blocked

/**
 * In place of a preview that can't be shown. A dangerous site gets the tab's warning, but only
 * a way back: «Open anyway» stays on the tab's own warning page.
 */
@Composable
internal fun LinkPeekPreviewMessage(
    status: LinkPeekPreviewStatus,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (status !is LinkPeekPreviewStatus.Blocked) return
    val site = status.site
    val imitatedHost = site.imitatedHost
    Surface(
        modifier = modifier.testTag(LinkPeekPreviewMessageTags.Message),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
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
}
