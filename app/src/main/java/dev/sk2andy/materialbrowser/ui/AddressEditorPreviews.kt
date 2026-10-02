package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ClipboardOffer
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionGroupRules
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.data.AddressSuggestion
import dev.sk2andy.materialbrowser.data.AddressSuggestionSource
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

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

@Composable
private fun AddressEditorPreviewContent() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
        workspaceAccent = previewWork.accent,
    ) {
        Column(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(vertical = VolaSpacing.x2),
        ) {
            AddressEditorHeader(workspace = previewWork, newTab = true, privateTab = false)
            AddressSuggestions(
                suggestions = previewSuggestions,
                query = "байкал лёд",
                highlightedIndex = -1,
                onHighlight = {},
                onSelect = {},
                onFill = {},
                rootHeightPx = 2_000f,
                bottomBarTopPx = remember { mutableFloatStateOf(2_000f) },
                workspaces = mapOf(previewAnime.id to previewAnime),
                clipboardOffer = ClipboardOffer.Link,
            )
        }
    }
}

@VolaPreviews
@Composable
private fun AddressEditorPreview() {
    AddressEditorPreviewContent()
}

@VolaPreviews
@Composable
private fun AddressClipboardChipPreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
    ) {
        Column(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(VolaSpacing.x4),
        ) {
            AddressClipboardChip(offer = ClipboardOffer.Text, onClick = {})
        }
    }
}
