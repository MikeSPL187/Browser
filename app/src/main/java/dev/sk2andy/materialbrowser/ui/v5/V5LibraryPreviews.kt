package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 library as canvas screens «р1»–«р3» and an empty segment, light and dark. Not localized. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val History = V5LibraryState(
    segment = V5LibrarySegment.History,
    query = "",
    sections = listOf(
        V5LibrarySection(
            "Сегодня",
            listOf(
                V5LibraryRow("1", "Северное сияние в марте", "polar-notes.ru", V5LibraryLeading.Site("П", V5Tile.Teal), V5LibraryTrailing.Value("12:24")),
                V5LibraryRow("2", "Прогноз сияний на неделю", "polar-notes.ru/prognoz", V5LibraryLeading.Site("П", V5Tile.Teal), V5LibraryTrailing.Value("12:20")),
                V5LibraryRow("3", "Поезд до Мурманска", "rzd-tickets.ru", V5LibraryLeading.Site("Р", V5Tile.Green), V5LibraryTrailing.Value("11:02")),
            ),
        ),
        V5LibrarySection(
            "Вчера",
            listOf(
                V5LibraryRow("4", "Пирог с брусникой на кефире", "recepty-doma.ru", V5LibraryLeading.Site("Р", V5Tile.Amber), V5LibraryTrailing.Value("21:40")),
            ),
        ),
    ),
    emptyMessage = "",
    actions = V5LibraryActions(end = "Очистить…", endDestructive = true),
)

private val Favorites = V5LibraryState(
    segment = V5LibrarySegment.Favorites,
    query = "",
    sections = listOf(
        V5LibrarySection(
            null,
            listOf(
                V5LibraryRow("f1", "Поездка на север", "", V5LibraryLeading.Folder, V5LibraryTrailing.Count(6), deletable = false),
                V5LibraryRow("f2", "Рецепты", "", V5LibraryLeading.Folder, V5LibraryTrailing.Count(14), deletable = false),
            ),
        ),
        V5LibrarySection(
            null,
            listOf(
                V5LibraryRow("s1", "Полярные заметки", "polar-notes.ru", V5LibraryLeading.Site("П", V5Tile.Teal)),
                V5LibraryRow("s2", "Схема метро", "metro.example", V5LibraryLeading.Site("М", V5Tile.Purple)),
            ),
        ),
    ),
    emptyMessage = "",
    actions = V5LibraryActions(start = "Новая папка", end = "Изменить"),
)

private val Downloads = V5LibraryState(
    segment = V5LibrarySegment.Downloads,
    query = "",
    sections = listOf(
        V5LibrarySection(
            null,
            listOf(
                V5LibraryRow("d1", "gid-po-severu.pdf", "12,4 из 20 МБ · осталось 8 с", V5LibraryLeading.File(VolaIcons.PictureAsPdf, V5Tile.Red), V5LibraryTrailing.Progress(0.62f), deletable = false),
                V5LibraryRow("d2", "karta-oblakov.png", "Не загрузилось · 2,3 МБ", V5LibraryLeading.File(VolaIcons.Error, V5Tile.Orange), V5LibraryTrailing.Retry),
                V5LibraryRow("d3", "siyanie-teriberka.jpg", "3,1 МБ · polar-notes.ru · вчера", V5LibraryLeading.File(VolaIcons.Image, V5Tile.Blue)),
            ),
        ),
    ),
    emptyMessage = "",
)

@Composable
private fun LibraryPreview(state: V5LibraryState, dark: Boolean) {
    VolaV5Theme(dark = dark) {
        V5Library(state, onSegmentChange = {}, onQueryChange = {}, onOpen = {}, onDelete = {}, onRetry = {}, onClose = {})
    }
}

@Preview(name = "Library v5 · history", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LibraryHistoryPreview() = LibraryPreview(History, dark = false)

@Preview(name = "Library v5 · history, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LibraryHistoryDarkPreview() = LibraryPreview(History, dark = true)

@Preview(name = "Library v5 · favorites", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LibraryFavoritesPreview() = LibraryPreview(Favorites, dark = false)

@Preview(name = "Library v5 · downloads, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LibraryDownloadsDarkPreview() = LibraryPreview(Downloads, dark = true)

@Preview(name = "Library v5 · empty history", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LibraryEmptyPreview() = LibraryPreview(
    History.copy(sections = emptyList(), emptyMessage = "Здесь появятся сайты, которые вы откроете. Частные вкладки сюда не попадают.", actions = V5LibraryActions()),
    dark = false,
)

@Preview(name = "Library v5 · favorites, folder open, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5LibraryFolderDarkPreview() = LibraryPreview(
    Favorites.copy(
        sections = listOf(Favorites.sections.last()),
        folder = "Поездка на север",
        actions = V5LibraryActions(start = "Новая папка"),
    ),
    dark = true,
)
