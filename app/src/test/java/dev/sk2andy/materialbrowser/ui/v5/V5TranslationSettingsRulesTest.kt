package dev.sk2andy.materialbrowser.ui.v5

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V5TranslationSettingsRulesTest {
    private val russian = Locale.forLanguageTag("ru")
    private val packs = listOf(
        V5LanguagePack("fr", "французский", 31, downloaded = false),
        V5LanguagePack("en", "английский", 32, downloaded = true),
        V5LanguagePack("de", "немецкий", 30, downloaded = true),
    )

    @Test
    fun `downloaded packs come first, then by name`() {
        assertEquals(listOf("en", "de", "fr"), V5TranslationSettingsRules.ordered(packs).map { it.code })
        assertTrue(V5TranslationSettingsRules.ordered(null).isEmpty())
    }

    @Test
    fun `the header counts only what is on the phone`() {
        assertEquals(62L, V5TranslationSettingsRules.downloadedBytes(packs))
        assertEquals(0L, V5TranslationSettingsRules.downloadedBytes(null))
    }

    @Test
    fun `never translate names one language and counts the rest`() {
        assertNull(V5TranslationSettingsRules.neverSummary(emptyList(), russian))
        assertEquals("Английский", V5TranslationSettingsRules.neverSummary(listOf("английский"), russian))
        assertEquals("Английский +2", V5TranslationSettingsRules.neverSummary(listOf("английский", "немецкий", "польский"), russian))
    }

    @Test
    fun `a swipe past 40 percent of the row deletes it`() {
        assertTrue(V5SwipeToDeleteRules.deletes(offsetPx = -160f, widthPx = 400f))
        assertFalse(V5SwipeToDeleteRules.deletes(offsetPx = -100f, widthPx = 400f))
        assertFalse(V5SwipeToDeleteRules.deletes(offsetPx = -100f, widthPx = 0f))
    }
}
