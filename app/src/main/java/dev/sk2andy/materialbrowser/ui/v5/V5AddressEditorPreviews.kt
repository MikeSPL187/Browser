package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.browser.ClipboardOffer
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.data.AddressSuggestion
import dev.sk2andy.materialbrowser.data.AddressSuggestionSource
import dev.sk2andy.materialbrowser.recall.RecallMatch
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 address editor as canvas screen «c»: «пол» typed, light and dark; empty with a copied link; private. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val PreviewItems = listOf(
    AddressSuggestionItem.Navigation(
        AddressSuggestion("https://polar-notes.ru/march", "Полярные заметки — сияние в марте", openTabId = "t1"),
    ),
    AddressSuggestionItem.Navigation(
        AddressSuggestion("https://polar-notes.ru/week", "Прогноз сияний на неделю", openTabId = "t2"),
    ),
    AddressSuggestionItem.Navigation(
        AddressSuggestion("https://polka-knig.ru", "Полка — книжный клуб", source = AddressSuggestionSource.Favorite),
    ),
    AddressSuggestionItem.Navigation(
        AddressSuggestion("https://polar-notes.ru/noch", "Полярная ночь: когда заканчивается"),
    ),
    AddressSuggestionItem.Recall(
        RecallMatch("personal", "https://example.ru/aurora", "Сияние над Териберкой", "…полярное сияние видно с…", 1L, 1.0),
    ),
    AddressSuggestionItem.Search("полярное сияние прогноз"),
    AddressSuggestionItem.Search("полёты в Мурманск"),
)

@Composable
private fun EditorPreview(text: String, items: List<AddressSuggestionItem>, clipboard: ClipboardOffer? = null) {
    V5AddressEditor(
        state = V5EditorState(
            value = TextFieldValue(text, TextRange(text.length)),
            layout = V5SuggestionLayoutRules.layout(items, text),
            clipboard = clipboard,
        ),
        onValueChange = {},
        onSubmit = {},
        onSelect = {},
        onFill = {},
        onClipboard = {},
        onCancel = {},
    )
}

@Preview(name = "Editor · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5EditorLightPreview() {
    VolaV5Theme(dark = false) { EditorPreview("пол", PreviewItems) }
}

@Preview(name = "Editor · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5EditorDarkPreview() {
    VolaV5Theme(dark = true) { EditorPreview("пол", PreviewItems) }
}

@Preview(name = "Editor · empty, copied link", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5EditorEmptyPreview() {
    VolaV5Theme(dark = false) { EditorPreview("", PreviewItems.take(2), ClipboardOffer.Link) }
}

@Preview(name = "Editor · private", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5EditorPrivatePreview() {
    VolaV5Theme(privateBrowsing = true) { EditorPreview("пол", PreviewItems.drop(2)) }
}

@Preview(name = "Editor · light, largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5EditorLightPreviewLargeFont() {
    VolaV5Theme(dark = false) { EditorPreview("пол", PreviewItems) }
}
