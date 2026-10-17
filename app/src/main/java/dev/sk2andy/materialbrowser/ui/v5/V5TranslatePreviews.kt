package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 translate sheet and translated row as canvas screens «т1» and «т2», light and dark. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val English = V5TranslateLanguage("en", "английский")
private val Russian = V5TranslateLanguage("ru", "русский")

private val FirstTranslation = V5TranslateSheetState(
    host = "northern-notes.com",
    from = English,
    to = Russian,
    fromOptions = listOf(English, V5TranslateLanguage("de", "немецкий"), V5TranslateLanguage("fr", "французский")),
    toOptions = listOf(English, Russian),
    alwaysTranslate = false,
    downloadBytes = 32L * 1024 * 1024,
)

private val NoActions = V5TranslateSheetActions(
    onFromChange = {},
    onToChange = {},
    onAlwaysTranslateChange = {},
    onTranslate = {},
)

@Composable
private fun SheetPreview(dark: Boolean, state: V5TranslateSheetState) {
    VolaV5Theme(dark = dark) {
        V5TranslateSheet(visible = true, state = state, actions = NoActions, onDismiss = {})
    }
}

@Preview(name = "Translate · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslateLightPreview() = SheetPreview(dark = false, state = FirstTranslation)

@Preview(name = "Translate · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslateDarkPreview() = SheetPreview(dark = true, state = FirstTranslation)

@Preview(name = "Translate · downloading · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslateBusyPreview() = SheetPreview(dark = true, state = FirstTranslation.copy(inProgress = true))

@Preview(name = "Translate · download failed · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslateErrorPreview() = SheetPreview(
    dark = false,
    state = FirstTranslation.copy(error = V5TranslateError.DownloadFailed),
)

@Composable
private fun RowPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Box(Modifier.background(V5Theme.colors.surface)) {
            V5TranslatedRow(state = V5TranslatedRowState("английский", "русский"), onOriginal = {}, onClose = {})
        }
    }
}

@Preview(name = "Translated row · light", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5TranslatedRowLightPreview() = RowPreview(dark = false)

@Preview(name = "Translated row · dark", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5TranslatedRowDarkPreview() = RowPreview(dark = true)
