package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.translation.OnDeviceTranslationUiState
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationState
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguage
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguageSetting
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguages
import dev.sk2andy.materialbrowser.browser.translation.TranslationPair
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateLanguage
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateLanguagesStatus
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserTranslateRulesTest {
    private val deRu = TranslationPair("de", "ru")
    private val name = { code: String -> "name-$code" }

    @Test
    fun onDeviceSheetOnlyForWebPagesOnAnEngineThatTranslatesOnThePhone() {
        assertTrue(BrowserTranslateRules.usesOnDevice(true, "https://example.de/"))
        assertTrue(BrowserTranslateRules.usesOnDevice(true, "http://example.de/"))
        assertFalse(BrowserTranslateRules.usesOnDevice(true, "about:blank"))
        assertFalse(BrowserTranslateRules.usesOnDevice(true, "file:///sdcard/a.html"))
        assertFalse(BrowserTranslateRules.usesOnDevice(false, "https://example.de/"))
    }

    @Test
    fun sheetOpensWithTheLastPairThenThePreferredTargetThenTheSuggestion() {
        val page = PageTranslationState(pageLanguage = "fr", userLanguage = "en")
        val last = OnDeviceTranslationUiState(page = page, pair = deRu)
        assertEquals(BrowserTranslateRules.Codes("de", "ru"), BrowserTranslateRules.initialPair(last, null, "es", "ru"))

        val fresh = OnDeviceTranslationUiState(page = page)
        assertEquals(
            BrowserTranslateRules.Codes("it", "es"),
            BrowserTranslateRules.initialPair(fresh, TranslationPair("it", "es"), null, "ru"),
        )
        assertEquals(
            BrowserTranslateRules.Codes("it", "uk"),
            BrowserTranslateRules.initialPair(fresh, TranslationPair("it", "es"), "uk", "ru"),
        )
        assertEquals(BrowserTranslateRules.Codes("fr", "en"), BrowserTranslateRules.initialPair(fresh, null, null, "ru"))
        assertEquals(
            BrowserTranslateRules.Codes(null, "ru"),
            BrowserTranslateRules.initialPair(OnDeviceTranslationUiState(), null, null, "ru"),
        )
    }

    @Test
    fun rowShowsOnlyWhileTranslatedAndNotTheClosedPair() {
        val translated = OnDeviceTranslationUiState(page = PageTranslationState(isTranslated = true), pair = deRu)
        assertEquals(deRu, BrowserTranslateRules.rowPair(translated, hidden = null))
        assertNull(BrowserTranslateRules.rowPair(translated, hidden = deRu))
        assertEquals(deRu, BrowserTranslateRules.rowPair(translated, hidden = TranslationPair("fr", "ru")))
        assertNull(BrowserTranslateRules.rowPair(translated.copy(page = PageTranslationState()), hidden = null))

        val byEngine = OnDeviceTranslationUiState(page = PageTranslationState(requested = deRu, isTranslated = true))
        assertEquals(deRu, BrowserTranslateRules.rowPair(byEngine, hidden = null))
    }

    @Test
    fun languagesUseTheEngineNameElseTheSystemOne() {
        assertEquals(
            V5TranslateLanguage("de", "немецкий"),
            BrowserTranslateRules.option(TranslationLanguage("de", "немецкий"), name),
        )
        assertEquals(V5TranslateLanguage("de", "name-de"), BrowserTranslateRules.option(TranslationLanguage("de", null), name))
        assertEquals(V5TranslateLanguage("de", "name-de"), BrowserTranslateRules.option(TranslationLanguage("de", " "), name))
    }

    @Test
    fun pickedLanguageMissingFromTheListStillShows() {
        val options = listOf(V5TranslateLanguage("de", "немецкий"))
        assertEquals(options.first(), BrowserTranslateRules.pick("de", options, name))
        assertEquals(V5TranslateLanguage("xx", "name-xx"), BrowserTranslateRules.pick("xx", options, name))
    }

    @Test
    fun languageNameComesFromTheSystemInTheAppLanguage() {
        assertEquals("German", BrowserTranslateRules.languageName("de", Locale.ENGLISH))
        assertEquals("", BrowserTranslateRules.languageName("", Locale.ENGLISH))
    }

    @Test
    fun menuOffersTranslationUnlessThePageLanguageIsNever() {
        val settings = mapOf("de" to TranslationLanguageSetting.Never, "fr" to TranslationLanguageSetting.Always)
        assertFalse(BrowserTranslateRules.offers("de", settings))
        assertTrue(BrowserTranslateRules.offers("fr", settings))
        assertTrue(BrowserTranslateRules.offers(null, settings))
        assertTrue(BrowserTranslateRules.offers("de", null))
    }

    @Test
    fun alwaysShowsWhenSetOrWhenThePacksAreOnThePhone() {
        val always = mapOf("de" to TranslationLanguageSetting.Always)
        assertEquals(true, BrowserTranslateRules.always("de", always, downloadBytes = 40_000_000L))
        assertEquals(false, BrowserTranslateRules.always("fr", always, downloadBytes = 0L))
        assertNull(BrowserTranslateRules.always("fr", always, downloadBytes = 40_000_000L))
        assertNull(BrowserTranslateRules.always("fr", always, downloadBytes = null))
        assertNull(BrowserTranslateRules.always("de", settings = null, downloadBytes = 0L))
        assertNull(BrowserTranslateRules.always(null, always, downloadBytes = 0L))
    }

    @Test
    fun anErrorShowsOnlyForThePairItWasAskedFor() {
        val asked = TranslationPair("en", "ru")
        assertTrue(BrowserTranslateRules.sameRequest(asked, "en", "ru"))
        assertFalse(BrowserTranslateRules.sameRequest(asked, "en", "de"))
        assertFalse(BrowserTranslateRules.sameRequest(null, "en", "ru"))
    }

    @Test
    fun theLanguagesListsAreLoadingUntilTheEngineAnswers() {
        assertEquals(V5TranslateLanguagesStatus.Loading, BrowserTranslateRules.languagesStatus(null, failed = false))
        assertEquals(V5TranslateLanguagesStatus.Failed, BrowserTranslateRules.languagesStatus(null, failed = true))
        assertEquals(
            V5TranslateLanguagesStatus.Ready,
            BrowserTranslateRules.languagesStatus(TranslationLanguages(emptyList(), emptyList()), failed = false),
        )
    }
}
