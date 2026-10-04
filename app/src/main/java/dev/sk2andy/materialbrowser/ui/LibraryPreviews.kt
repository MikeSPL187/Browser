package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
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
