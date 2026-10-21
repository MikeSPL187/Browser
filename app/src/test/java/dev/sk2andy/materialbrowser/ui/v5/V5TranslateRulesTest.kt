package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.browser.translation.PageTranslationError
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V5TranslateRulesTest {
    private val english = V5TranslateLanguage("en", "английский")
    private val russian = V5TranslateLanguage("ru", "русский")
    private val state = V5TranslateSheetState(
        host = "northern-notes.com",
        from = english,
        to = russian,
        fromOptions = listOf(english),
        toOptions = listOf(russian),
        alwaysTranslate = false,
        downloadBytes = 0,
    )

    @Test
    fun `translate needs two different known languages and nothing under way`() {
        assertTrue(V5TranslateRules.canTranslate(state))
        assertFalse(V5TranslateRules.canTranslate(state.copy(inProgress = true)))
        assertFalse(V5TranslateRules.canTranslate(state.copy(from = null)))
        assertFalse(V5TranslateRules.canTranslate(state.copy(to = english)))
    }

    @Test
    fun `the note names a download only when the pack is missing`() {
        assertTrue(V5TranslateRules.needsDownload(32L * 1024 * 1024))
        assertFalse(V5TranslateRules.needsDownload(0))
        assertFalse(V5TranslateRules.needsDownload(null))
    }

    @Test
    fun `engine errors fold into what the sheet can say`() {
        assertNull(V5TranslateRules.error(null))
        assertNull(V5TranslateRules.error(PageTranslationError.ModelDownloadRequired))
        assertEquals(V5TranslateError.DownloadFailed, V5TranslateRules.error(PageTranslationError.ModelDownloadFailed))
        assertEquals(
            V5TranslateError.LanguageUnsupported,
            V5TranslateRules.error(PageTranslationError.LanguageNotSupported),
        )
        assertEquals(V5TranslateError.CouldNotTranslate, V5TranslateRules.error(PageTranslationError.CouldNotTranslate))
    }

    @Test
    fun `a language standing alone starts with a capital`() {
        assertEquals("Английский", V5TranslateRules.displayName("английский", Locale.forLanguageTag("ru")))
        assertEquals("English", V5TranslateRules.displayName("English", Locale.ENGLISH))
    }

    @Test
    fun `the language search finds by name ignoring case and keeps all for a blank query`() {
        val names = listOf("English", "Español", "Русский")
        assertEquals(listOf("Español"), V5TranslateRules.matching(names, " esp") { it })
        assertEquals(listOf("Русский"), V5TranslateRules.matching(names, "рус") { it })
        assertEquals(names, V5TranslateRules.matching(names, "  ") { it })
    }
}
