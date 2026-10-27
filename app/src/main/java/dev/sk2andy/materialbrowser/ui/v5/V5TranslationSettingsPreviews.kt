package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* Settings → Translation as canvas screen «т3», light and dark. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844
private const val MEGABYTE = 1024L * 1024

private val Sample = V5TranslationSettingsState(
    offerInMenu = true,
    translateInto = "русский",
    neverTranslate = listOf("английский"),
    packs = listOf(
        V5LanguagePack("en", "английский", 32 * MEGABYTE, downloaded = true),
        V5LanguagePack("de", "немецкий", 32 * MEGABYTE, downloaded = true),
        V5LanguagePack("fr", "французский", 31 * MEGABYTE, downloaded = false),
        V5LanguagePack("es", "испанский", 30 * MEGABYTE, downloaded = false, downloading = true),
    ),
    translateIntoCode = "ru",
    neverOptions = listOf(
        V5TranslateLanguage("en", "английский"),
        V5TranslateLanguage("de", "немецкий"),
        V5TranslateLanguage("fr", "французский"),
    ),
    neverCodes = setOf("en"),
    intoOptions = listOf(
        V5TranslateLanguage("en", "английский"),
        V5TranslateLanguage("de", "немецкий"),
        V5TranslateLanguage("ru", "русский"),
    ),
)

private val NoActions = V5TranslationSettingsActions(
    onBack = {},
    onOfferChange = {},
    onTranslateInto = {},
    onNeverTranslate = { _, _ -> },
    onDownload = {},
    onDelete = {},
)

@Preview(name = "Translation settings · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslationSettingsLightPreview() {
    VolaV5Theme(dark = false) { V5TranslationSettings(Sample, NoActions) }
}

@Preview(name = "Translation settings · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslationSettingsDarkPreview() {
    VolaV5Theme(dark = true) { V5TranslationSettings(Sample, NoActions) }
}

@Preview(name = "Translate into · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslationIntoLightPreview() {
    VolaV5Theme(dark = false) { V5TranslationIntoPage(Sample, onBack = {}, onChoose = {}) }
}

@Preview(name = "Translate into · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslationIntoDarkPreview() {
    VolaV5Theme(dark = true) { V5TranslationIntoPage(Sample, onBack = {}, onChoose = {}) }
}

@Preview(name = "Never translate · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslationNeverLightPreview() {
    VolaV5Theme(dark = false) { V5TranslationNeverPage(Sample, onBack = {}, onChange = { _, _ -> }) }
}

@Preview(name = "Never translate · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslationNeverDarkPreview() {
    VolaV5Theme(dark = true) { V5TranslationNeverPage(Sample, onBack = {}, onChange = { _, _ -> }) }
}

@Preview(name = "Translation settings · light, largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5TranslationSettingsLightPreviewLargeFont() {
    VolaV5Theme(dark = false) { V5TranslationSettings(Sample, NoActions) }
}
