package dev.sk2andy.materialbrowser.browser.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceTranslationRulesTest {
    private val german = PageTranslationState(pageLanguage = "de", pageLanguageSupported = true, userLanguage = "ru")
    private val languages = TranslationLanguages(
        from = listOf(TranslationLanguage("de", "German"), TranslationLanguage("en", "English")),
        to = listOf(
            TranslationLanguage("en", "English"),
            TranslationLanguage("ru", "Russian"),
            TranslationLanguage("pt", "Portuguese"),
            TranslationLanguage("zh-Hans", "Chinese (Simplified)"),
        ),
    )

    @Test
    fun `a page in a foreign language is offered into the language the user reads`() {
        assertEquals(TranslationPair("de", "ru"), OnDeviceTranslationRules.suggestedPair(german, listOf("en-US")))
    }

    @Test
    fun `without the engine's guess the first app language is the target, matched to a model`() {
        val state = german.copy(userLanguage = null)

        assertEquals(
            TranslationPair("de", "pt"),
            OnDeviceTranslationRules.suggestedPair(state, listOf("pt-BR", "en"), languages),
        )
    }

    @Test
    fun `a page in any language the user reads is not offered`() {
        assertNull(OnDeviceTranslationRules.suggestedPair(german.copy(pageLanguage = "ru-RU"), emptyList()))
        assertNull(OnDeviceTranslationRules.suggestedPair(german, listOf("en", "de-AT")))
    }

    @Test
    fun `an unknown or unsupported page language has no pair`() {
        assertNull(OnDeviceTranslationRules.suggestedPair(german.copy(pageLanguage = null), listOf("en")))
        assertNull(OnDeviceTranslationRules.suggestedPair(german.copy(pageLanguageSupported = false), listOf("en")))
    }

    @Test
    fun `a target without a model gives no pair`() {
        val state = german.copy(userLanguage = "eo")

        assertNull(OnDeviceTranslationRules.suggestedPair(state, emptyList(), languages))
    }

    @Test
    fun `the offer waits for a pair and stays away from translated pages and never sites`() {
        val pair = TranslationPair("de", "ru")

        assertTrue(OnDeviceTranslationRules.shouldOffer(german, pair, neverTranslateSite = false))
        assertFalse(OnDeviceTranslationRules.shouldOffer(german, null, neverTranslateSite = false))
        assertFalse(OnDeviceTranslationRules.shouldOffer(german, pair, neverTranslateSite = true))
        assertFalse(OnDeviceTranslationRules.shouldOffer(german.copy(requested = pair), pair, neverTranslateSite = false))
        assertFalse(OnDeviceTranslationRules.shouldOffer(german.copy(isTranslated = true), pair, neverTranslateSite = false))
    }

    @Test
    fun `tags match exactly, then by script, then by base language`() {
        assertEquals("ru", OnDeviceTranslationRules.matchSupported("ru", languages.to))
        assertEquals("ru", OnDeviceTranslationRules.matchSupported("ru_RU", languages.to))
        assertEquals("zh-Hans", OnDeviceTranslationRules.matchSupported("zh-hans-CN", languages.to))
        assertEquals("zh-Hans", OnDeviceTranslationRules.matchSupported("zh", languages.to))
        assertNull(OnDeviceTranslationRules.matchSupported("zh-Hant", languages.to))
        assertNull(OnDeviceTranslationRules.matchSupported("", languages.to))
    }

    @Test
    fun `the same reader reads regions of a language but not another script`() {
        assertTrue(OnDeviceTranslationRules.sameLanguage("en-GB", "en"))
        assertTrue(OnDeviceTranslationRules.sameLanguage("zh", "zh-Hant"))
        assertFalse(OnDeviceTranslationRules.sameLanguage("zh-Hans", "zh-Hant"))
        assertFalse(OnDeviceTranslationRules.sameLanguage("de", "da"))
        assertFalse(OnDeviceTranslationRules.sameLanguage("??", "en"))
    }

    @Test
    fun `only a missing model asks for a download`() {
        assertTrue(OnDeviceTranslationRules.needsDownload(42_000_000))
        assertFalse(OnDeviceTranslationRules.needsDownload(0))
        assertFalse(OnDeviceTranslationRules.needsDownload(null))
    }

    @Test
    fun `the unavailable engine answers every call without translating`() {
        val results = mutableListOf<PageTranslationResult>()

        PageTranslationPort.Unavailable.translate(TranslationPair("de", "ru"), allowModelDownload = true) { results += it }
        TranslationModelsPort.Unavailable.download("de") { results += it }

        assertFalse(PageTranslationPort.Unavailable.isAvailable)
        assertFalse(TranslationModelsPort.Unavailable.isAvailable)
        assertEquals(List(2) { PageTranslationResult.Failed(PageTranslationError.Unsupported) }, results)
    }
}
