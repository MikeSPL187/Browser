package dev.sk2andy.materialbrowser.ui

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

@Composable
private fun GlancePreview(isPrivate: Boolean) {
    val context = LocalContext.current
    MaterialBrowserTheme {
        LinkPeekOverlay(
            url = "https://ice-forecast.example/south",
            progress = 0f,
            armed = false,
            createPreviewView = { onProgressChanged, _ ->
                onProgressChanged(100)
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
