package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguage
import dev.sk2andy.materialbrowser.browser.translation.TranslationModel
import dev.sk2andy.materialbrowser.ui.v5.V5LanguagePack
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserTranslationSettingsRulesTest {
    private val name = { code: String -> "name-$code" }

    @Test
    fun modelsBecomeOnePackPerLanguageWithTheEngineNameElseTheSystemOne() {
        val models = listOf(
            TranslationModel(TranslationLanguage("de", "немецкий"), isDownloaded = true, sizeBytes = 32),
            TranslationModel(TranslationLanguage("de", "немецкий"), isDownloaded = true, sizeBytes = 32),
            TranslationModel(TranslationLanguage("fr", null), isDownloaded = false, sizeBytes = 31),
        )
        assertEquals(
            listOf(
                V5LanguagePack("de", "немецкий", 32, downloaded = true),
                V5LanguagePack("fr", "name-fr", 31, downloaded = false),
            ),
            BrowserTranslationSettingsRules.packs(models, emptySet(), name),
        )
    }

    @Test
    fun aPackDownloadingShowsSoUntilTheEngineHasIt() {
        val waiting = TranslationModel(TranslationLanguage("fr", "французский"), isDownloaded = false, sizeBytes = 31)
        val done = waiting.copy(isDownloaded = true)
        assertEquals(true, BrowserTranslationSettingsRules.packs(listOf(waiting), setOf("fr"), name).single().downloading)
        assertEquals(false, BrowserTranslationSettingsRules.packs(listOf(done), setOf("fr"), name).single().downloading)
        assertEquals(false, BrowserTranslationSettingsRules.packs(listOf(waiting), emptySet(), name).single().downloading)
    }
}
