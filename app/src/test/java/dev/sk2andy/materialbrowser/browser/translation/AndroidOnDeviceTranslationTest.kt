package dev.sk2andy.materialbrowser.browser.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidOnDeviceTranslationTest {
    @Test
    fun `the app's locales become the languages the user reads, first one first`() {
        assertEquals(listOf("ru-RU", "en-US"), OnDeviceTranslationLanguages.fromLanguageTags("ru-RU,en-US"))
        assertEquals(listOf("de"), OnDeviceTranslationLanguages.fromLanguageTags(" de , ,de"))
        assertEquals(emptyList<String>(), OnDeviceTranslationLanguages.fromLanguageTags(""))
    }

    @Test
    fun `the engine's models are not asked for until a screen needs them`() {
        var resolved = 0
        val models = LazyTranslationModels {
            resolved++
            TranslationModelsPort.Unavailable
        }
        assertEquals(0, resolved)

        var supported: Boolean? = null
        models.isSupportedOnDevice { supported = it }
        var size: Long? = 1
        models.downloadSize(TranslationPair("de", "ru")) { size = it }

        assertEquals(1, resolved)
        assertFalse(supported!!)
        assertEquals(null, size)
    }

    @Test
    fun `a tab's page reaches the controller through the lazy models and back out on close`() {
        var resolved = false
        val controller = OnDeviceTranslationController(
            LazyTranslationModels {
                resolved = true
                TranslationModelsPort.Unavailable
            },
        ) { listOf("ru") }

        controller.attach("tab", PageTranslationPort.Unavailable)
        controller.detach("tab")

        assertFalse(resolved)
        var answered: Long? = 1
        controller.downloadSize(TranslationPair("de", "ru")) { answered = it }
        assertTrue(resolved)
        assertEquals(null, answered)
    }

    @Test
    fun `language and site choices reach the engine through the lazy models`() {
        val calls = mutableListOf<String>()
        val engine = object : TranslationModelsPort by TranslationModelsPort.Unavailable {
            override fun languageSettings(onResult: (Map<String, TranslationLanguageSetting>?) -> Unit) {
                calls += "settings"
                onResult(mapOf("de" to TranslationLanguageSetting.Never))
            }

            override fun setLanguageSetting(
                languageCode: String,
                setting: TranslationLanguageSetting,
                onResult: (PageTranslationResult) -> Unit,
            ) {
                calls += "set $languageCode $setting"
                onResult(PageTranslationResult.Done)
            }

            override fun neverTranslateSites(onResult: (List<NeverTranslateSite>?) -> Unit) {
                calls += "sites"
                onResult(listOf(NeverTranslateSite("https://example.com", "example.com")))
            }

            override fun setNeverTranslateSite(origin: String, never: Boolean, onResult: (PageTranslationResult) -> Unit) {
                calls += "site $origin $never"
                onResult(PageTranslationResult.Done)
            }
        }
        val models = LazyTranslationModels { engine }

        models.languageSettings { }
        models.setLanguageSetting("de", TranslationLanguageSetting.Always) { }
        models.neverTranslateSites { }
        models.setNeverTranslateSite("https://example.com", never = false) { }

        assertEquals(listOf("settings", "set de Always", "sites", "site https://example.com false"), calls)
    }

    @Test
    fun `the sheet's language lists come from the engine's models`() {
        val languages = TranslationLanguages(
            from = listOf(TranslationLanguage("de", "German")),
            to = listOf(TranslationLanguage("ru", "Russian")),
        )
        val models = object : TranslationModelsPort by TranslationModelsPort.Unavailable {
            override fun languages(onResult: (TranslationLanguages?) -> Unit) = onResult(languages)
        }
        val controller = OnDeviceTranslationController(models) { listOf("ru") }

        var listed: TranslationLanguages? = null
        controller.languages { listed = it }

        assertEquals(languages, listed)
    }
}
