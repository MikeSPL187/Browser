package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.LinkPeekPreviewStatus
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** Glance over the page (board W-Glance): handle, site, close, and the history note. */
@VolaPreviews
@Composable
private fun GlanceCardPreview() {
    GlancePreview(isPrivate = false)
}

/** From a private tab the preview is private too, and says so. */
@VolaPreviews
@Composable
private fun GlancePrivatePreview() {
    GlancePreview(isPrivate = true)
}

/** A lookalike link: nothing loads, the card warns and offers only a way back. */
@VolaPreviews
@Composable
private fun GlanceBlockedPreview() {
    GlancePreview(
        isPrivate = false,
        url = "https://paypa1.com/login",
        status = LinkPeekPreviewStatus.Blocked(
            BlockedSite(url = "https://paypa1.com/login", host = "paypa1.com", imitatedHost = "paypal.com"),
        ),
    )
}

/** The site isn't there: what went wrong, Retry and Close in place of the page. */
@VolaPreviews
@Composable
private fun GlanceFailedPreview() {
    GlancePreview(
        isPrivate = false,
        status = LinkPeekPreviewStatus.Failed(BrowserEngineFailureKind.UnknownHost),
    )
}

/** The preview's renderer stopped: Retry builds a fresh one. */
@VolaPreviews
@Composable
private fun GlanceCrashedPreview() {
    GlancePreview(isPrivate = false, status = LinkPeekPreviewStatus.Crashed)
}
