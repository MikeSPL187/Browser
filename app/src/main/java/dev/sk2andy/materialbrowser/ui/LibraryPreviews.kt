package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.data.HistoryEntry
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

private val previewWork = BrowserProfile(
    id = "work",
    emoji = WorkspaceIcons.emojiFor("work").orEmpty(),
    name = "Работа",
    accent = WorkspaceAccent.Teal,
)

private val previewAnime = BrowserProfile(
    id = "anime",
    emoji = WorkspaceIcons.emojiFor("gaming").orEmpty(),
    name = "Аниме",
    accent = WorkspaceAccent.Coral,
)

private const val PREVIEW_HOUR = 3_600_000L

/** Board W-History: today and yesterday as cards, the workspace gem after the time. */
@VolaPreviews
@Composable
private fun HistoryScreenPreview() {
    val now = System.currentTimeMillis()
    MaterialBrowserTheme {
        HistoryScreen(
            profiles = listOf(previewWork, previewAnime),
            activeProfileId = previewWork.id,
            history = listOf(
                HistoryEntry("https://north-guide.ru/baikal", "Байкал зимой: как выбрать маршрут", now, previewWork.id),
                HistoryEntry("https://ice-forecast.example.ru/", "Прогноз толщины льда", now - PREVIEW_HOUR, previewWork.id),
                HistoryEntry("https://docs.example.com/plan", "План запуска на октябрь", now - 3 * PREVIEW_HOUR, previewWork.id),
                HistoryEntry("https://calendar.example.com/autumn", "Расписание сезона: осень", now - 26 * PREVIEW_HOUR, previewAnime.id),
            ),
            onDeleteEntries = {},
            onClearHistory = {},
            onOpenEntry = {},
            onBack = {},
        )
    }
}

/** Board W-States: an empty history. */
@VolaPreviews
@Composable
private fun HistoryEmptyPreview() {
    MaterialBrowserTheme {
        HistoryScreen(
            profiles = listOf(previewWork),
            activeProfileId = previewWork.id,
            history = emptyList(),
            onDeleteEntries = {},
            onClearHistory = {},
            onOpenEntry = {},
            onBack = {},
            onOpenNewTab = {},
        )
    }
}

/** Board W-Favorites: folder cards two in a row, then sites with no folder. */
@VolaPreviews
@Composable
private fun FavoritesScreenPreview() {
    val travel = FavoriteFolder("travel", "Путешествия")
    val reading = FavoriteFolder("reading", "Чтение")
    val work = FavoriteFolder("work", "Работа")
    val library = FavoriteLibrary(
        listOf(
            travel,
            reading,
            work,
            FavoriteEntry("https://north-guide.ru/routes", "Ледовые маршруты Байкала", 1, parentFolderId = travel.id),
            FavoriteEntry("https://ice-forecast.example.ru/", "Прогноз толщины льда", 2),
            FavoriteEntry("https://north-guide.ru/baikal", "Байкал зимой: как выбрать маршрут", 3),
            FavoriteEntry("https://fonts.example.org/", "Каталог шрифтов с кириллицей", 4),
        ),
    )
    MaterialBrowserTheme {
        FavoritesScreen(
            favorites = library.favorites,
            library = library,
            onDeleteFavorite = { _, _ -> },
            onUndoDelete = {},
            onOpenFavorite = {},
            onBack = {},
        )
    }
}

/** A folder with no icon of its own shows its first sites' favicons, two by two. */
@VolaPreviews
@Composable
private fun FavoritesFolderMosaicPreview() {
    val travel = FavoriteFolder("travel", "Путешествия")
    val reading = FavoriteFolder("reading", "Чтение")
    val sites = listOf(
        FavoriteEntry("https://north-guide.ru/routes", "Ледовые маршруты", 1, parentFolderId = travel.id),
        FavoriteEntry("https://ice-forecast.example.ru/", "Прогноз льда", 2, parentFolderId = travel.id),
        FavoriteEntry("https://maps.example.org/", "Карты", 3, parentFolderId = travel.id),
        FavoriteEntry("https://fonts.example.org/", "Каталог шрифтов", 4, parentFolderId = reading.id),
    )
    val colors = listOf(0xFF2F6B5F, 0xFF4A3A9E, 0xFF9A3A1E, 0xFF1E5E8C)
    val favicons = sites.zip(colors).associate { (site, color) ->
        site.url to Bitmap.createBitmap(PREVIEW_FAVICON_PX, PREVIEW_FAVICON_PX, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(color.toInt()) }
    }
    val library = FavoriteLibrary(listOf(travel, reading) + sites)
    MaterialBrowserTheme {
        FavoritesScreen(
            favorites = library.favorites,
            library = library,
            favicons = favicons,
            onDeleteFavorite = { _, _ -> },
            onUndoDelete = {},
            onOpenFavorite = {},
            onBack = {},
        )
    }
}

private const val PREVIEW_FAVICON_PX = 32

/** Board W-States: no favorites yet, with bookmarks from another browser one tap away. */
@VolaPreviews
@Composable
private fun FavoritesEmptyPreview() {
    MaterialBrowserTheme {
        FavoritesScreen(
            favorites = emptyList(),
            onDeleteFavorite = { _, _ -> },
            onUndoDelete = {},
            onOpenFavorite = {},
            onBack = {},
            sort = FavoritesSort.Name,
            onImportBookmarks = {},
        )
    }
}

/** Board W-Downloads: a file still downloading on top, then today's files by kind. */
@VolaPreviews
@Composable
private fun DownloadsScreenPreview() {
    val now = System.currentTimeMillis()
    MaterialBrowserTheme {
        DownloadsScreen(
            downloads = listOf(
                DownloadEntry(1, "Карта маршрутов Байкала.pdf", "https://north-guide.ru/map", DownloadStatus.Running, 18_600_000, 30_000_000, now, "application/pdf", supportsPause = true),
                DownloadEntry(2, "Снаряжение для льда.pdf", "https://north-guide.ru/gear", DownloadStatus.Successful, 2_100_000, 2_100_000, now - PREVIEW_HOUR, "application/pdf"),
                DownloadEntry(3, "olkhon-grotto.jpg", "", DownloadStatus.Successful, 3_400_000, 3_400_000, now - 2 * PREVIEW_HOUR, "image/jpeg"),
                DownloadEntry(4, "Отчёт по метрикам.xlsx", "", DownloadStatus.Successful, 86_000, 86_000, now - 3 * PREVIEW_HOUR, ""),
                DownloadEntry(5, "ice-report.exe", "https://ice.example/", DownloadStatus.Failed, 0, 0, now - 4 * PREVIEW_HOUR, ""),
            ),
            onClearFinished = {},
            onOpenDownload = {},
            onBack = {},
        )
    }
}
