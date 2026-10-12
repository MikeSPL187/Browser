package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The link preview as canvas screen «н1»: light, dark, loading in a private tab, and a failed page. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val noActions = V5LinkGlanceActions(
    onOpen = {},
    onOpenInBackground = {},
    onOpenInNewTab = {},
    onOpenInPrivate = {},
    onCopy = {},
    onShare = {},
    onDownloadLink = {},
    onDownloadImage = {},
    onRetry = {},
    onDismiss = {},
)

@Composable
private fun GlancePreview(
    dark: Boolean,
    privateTab: Boolean = false,
    progress: Float? = null,
    message: V5LinkGlanceMessage? = null,
) {
    VolaV5Theme(dark = dark, privateBrowsing = privateTab) {
        // A stand-in for the page under the card; the real one is the engine's own surface.
        Box(Modifier.fillMaxSize().background(V5Theme.colors.background)) {
            V5LinkGlance(
                state = V5LinkGlanceState(
                    url = "https://polar-notes.ru/prognoz?utm_source=feed",
                    progress = progress,
                    privateTab = privateTab,
                    message = message,
                    canOpenInPrivate = !privateTab,
                    canDownloadImage = false,
                ),
                actions = noActions,
                origin = null,
                onPageScale = {},
                entered = true,
            ) { _, modifier ->
                Column(modifier.background(V5Theme.colors.background).padding(V5Spacing.space4)) {
                    Text("Northern lights forecast", style = V5Theme.type.title, color = V5Theme.colors.label)
                    Text(
                        "Tonight the aurora is likely over the whole of the Kola Peninsula.",
                        style = V5Theme.type.body,
                        color = V5Theme.colors.labelSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Preview(name = "Link preview · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LinkGlanceLightPreview() {
    GlancePreview(dark = false)
}

@Preview(name = "Link preview · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LinkGlanceDarkPreview() {
    GlancePreview(dark = true)
}

@Preview(name = "Link preview · private, loading", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LinkGlancePrivatePreview() {
    GlancePreview(dark = true, privateTab = true, progress = 0.4f)
}

@Preview(name = "Link preview · failed", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LinkGlanceFailedPreview() {
    GlancePreview(
        dark = false,
        message = V5LinkGlanceMessage(
            title = "Preview unavailable",
            body = "polar-notes.ru didn't respond.",
            danger = false,
            retryable = true,
        ),
    )
}
