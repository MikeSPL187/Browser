package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionGroupRules
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.data.AddressSuggestion
import dev.sk2andy.materialbrowser.data.AddressSuggestionSource
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
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

/** The Editing board: library, searches, open tabs here and in another workspace (П1). */
private val previewSuggestions = AddressSuggestionGroupRules.displayOrder(
    listOf(
        AddressSuggestionItem.Navigation(
            AddressSuggestion(
                url = "https://north-guide.ru/baikal/winter",
                title = "Байкал зимой: как выбрать маршрут",
                openTabId = "tab-1",
            ),
        ),
        AddressSuggestionItem.Navigation(
            AddressSuggestion(
                url = "https://ice-forecast.example.com/",
                title = "Прогноз льда на Байкале",
                openTabId = "tab-2",
                openTabProfileId = previewAnime.id,
            ),
        ),
        AddressSuggestionItem.Navigation(
            AddressSuggestion(
                url = "https://pogoda.example.ru/baikal",
                title = "Погода на Байкале в феврале",
                lastVisitedAt = 1L,
            ),
        ),
        AddressSuggestionItem.Navigation(
            AddressSuggestion(
                url = "https://routes.example.ru/ice",
                title = "Ледовые маршруты Байкала",
                source = AddressSuggestionSource.Favorite,
            ),
        ),
        AddressSuggestionItem.Search("байкал лёд где кататься"),
        AddressSuggestionItem.Search("байкал лёд толщина"),
    ),
)

@VolaPreviews
@Composable
private fun AddressEditorPreview() {
    AddressEditorPreviewContent()
}

