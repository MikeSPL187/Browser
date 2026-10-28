package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
import dev.sk2andy.materialbrowser.data.DownloadStatus
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme

/* The v5 downloads (canvas screen «р3»): one under way, one paused, and each kind of failure (DL-03). Not localized. */

private const val PREVIEW_HOUR = 3_600_000L

@Composable
private fun DownloadsPreview(mode: BrowserAppearanceMode) {
    val now = System.currentTimeMillis()
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = mode)) {
        DownloadsLibraryV5(
            downloads = listOf(
                DownloadEntry(1, "Карта Байкала.pdf", "https://north-guide.ru/map", DownloadStatus.Running, 18_600_000, 30_000_000, now, "application/pdf", supportsPause = true),
                DownloadEntry(2, "Паромы.mp4", "https://north-guide.ru/ferry", DownloadStatus.Paused, 4_000_000, 90_000_000, now, "video/mp4", supportsPause = true),
                DownloadEntry(3, "Снаряжение.pdf", "https://north-guide.ru/gear", DownloadStatus.Failed, 0, 0, now - PREVIEW_HOUR, "application/pdf", retry = DownloadRetry.Available),
                DownloadEntry(4, "Выписка.pdf", "https://bank.example/statements", DownloadStatus.Failed, 0, 0, now - PREVIEW_HOUR, "application/pdf", retry = DownloadRetry.Unavailable(DownloadRetryUnavailable.Blob, "https://bank.example/statements")),
                DownloadEntry(5, "Билет.pdf", "https://rail.example/ticket", DownloadStatus.Failed, 0, 0, now - PREVIEW_HOUR, "application/pdf", retry = DownloadRetry.Unavailable(DownloadRetryUnavailable.PrivateClosed)),
                DownloadEntry(6, "notes.zip", "https://polar-notes.ru/", DownloadStatus.Cancelled, 0, 0, now - PREVIEW_HOUR, "application/zip", retry = DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered)),
            ),
            isClearing = false,
            onOpen = {},
            onTogglePause = {},
            onCancel = {},
            onRetry = {},
            onOpenPage = {},
            onClearFinished = {},
            onClose = {},
        )
    }
}

@Preview(name = "Downloads v5 · failures, light", widthDp = 390, heightDp = 844)
@Composable
private fun DownloadsLibraryV5LightPreview() = DownloadsPreview(BrowserAppearanceMode.Light)

@Preview(name = "Downloads v5 · failures, dark", widthDp = 390, heightDp = 844)
@Composable
private fun DownloadsLibraryV5DarkPreview() = DownloadsPreview(BrowserAppearanceMode.Dark)

@Preview(name = "Downloads v5 · failures, light, largest font", widthDp = 390, heightDp = 844, fontScale = 2f)
@Composable
private fun DownloadsLibraryV5LargeFontPreview() = DownloadsPreview(BrowserAppearanceMode.Light)
