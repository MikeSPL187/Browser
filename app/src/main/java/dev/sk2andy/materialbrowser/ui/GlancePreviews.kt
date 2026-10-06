package dev.sk2andy.materialbrowser.ui

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.LinkPeekPreviewStatus
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

@Composable
private fun GlancePreview(
    isPrivate: Boolean,
    url: String = "https://ice-forecast.example/south",
    status: LinkPeekPreviewStatus = LinkPeekPreviewStatus.Loaded,
) {
    val context = LocalContext.current
    MaterialBrowserTheme {
        LinkPeekOverlay(
            url = url,
            progress = 0f,
            armed = false,
            createPreviewView = { callbacks ->
                callbacks.onProgressChanged(100)
                callbacks.onStatusChanged(status)
                View(context)
            },
            releasePreviewView = {},
            onOpen = {},
            isPrivate = isPrivate,
            onDismiss = {},
        )
    }
}

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
