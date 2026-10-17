package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.translation.OnDeviceTranslationUiState
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationState
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguage
import dev.sk2andy.materialbrowser.browser.translation.TranslationPair
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateLanguage
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
}
