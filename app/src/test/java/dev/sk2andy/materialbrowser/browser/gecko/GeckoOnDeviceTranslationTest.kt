package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineCapabilities
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationError
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationState
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguage
import dev.sk2andy.materialbrowser.browser.translation.TranslationModel
import dev.sk2andy.materialbrowser.browser.translation.TranslationPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.TranslationsController.Language
import org.mozilla.geckoview.TranslationsController.RuntimeTranslation
import org.mozilla.geckoview.TranslationsController.SessionTranslation
import org.mozilla.geckoview.TranslationsController.TranslationsException

class GeckoOnDeviceTranslationTest {
    @Test
    fun `a detected foreign page maps to an untranslated state`() {
        val state = SessionTranslation.TranslationState(
            null,
            null,
            SessionTranslation.DetectedLanguages("ru", true, "de"),
            false,
            false,
        )

        assertEquals(
            PageTranslationState(pageLanguage = "de", pageLanguageSupported = true, userLanguage = "ru"),
            GeckoTranslationMapping.state(state),
        )
    }

    @Test
    fun `a translated page keeps its pair, readiness and visible change`() {
        val state = SessionTranslation.TranslationState(
            SessionTranslation.TranslationPair("de", "ru"),
            null,
            SessionTranslation.DetectedLanguages("ru", true, "de"),
            true,
            true,
        )

        val mapped = GeckoTranslationMapping.state(state)

        assertEquals(TranslationPair("de", "ru"), mapped.requested)
        assertTrue(mapped.isEngineReady)
        assertTrue(mapped.isTranslated)
        assertFalse(mapped.hasError)
    }

    @Test
    fun `a half pair or an error string is not hidden`() {
        val state = SessionTranslation.TranslationState(
            SessionTranslation.TranslationPair("de", null),
            "engine-load-failure",
            null,
            false,
            false,
        )

        val mapped = GeckoTranslationMapping.state(state)

        assertNull(mapped.requested)
        assertNull(mapped.pageLanguage)
        assertTrue(mapped.hasError)
    }

    @Test
    fun `languages and models keep their codes, names and sizes`() {
        val german = Language("de", "German")
        val languages = GeckoTranslationMapping.languages(
            RuntimeTranslation.TranslationSupport(listOf(german), null),
        )
        val models = GeckoTranslationMapping.models(
            listOf(
                RuntimeTranslation.LanguageModel(german, true, 42_000_000L),
                RuntimeTranslation.LanguageModel(null, false, 1L),
            ),
        )

        assertEquals(listOf(TranslationLanguage("de", "German")), languages?.from)
        assertEquals(emptyList<TranslationLanguage>(), languages?.to)
        assertNull(GeckoTranslationMapping.languages(null))
        assertEquals(listOf(TranslationModel(TranslationLanguage("de", "German"), true, 42_000_000L)), models)
    }

    @Test
    fun `gecko errors map to what the user can act on`() {
        fun mapped(code: Int) = GeckoTranslationMapping.error(TranslationsException(code))

        assertEquals(PageTranslationError.ModelDownloadRequired, mapped(TranslationsException.ERROR_MODEL_DOWNLOAD_REQUIRED))
        assertEquals(PageTranslationError.ModelDownloadFailed, mapped(TranslationsException.ERROR_MODEL_COULD_NOT_DOWNLOAD))
        assertEquals(PageTranslationError.Unsupported, mapped(TranslationsException.ERROR_ENGINE_NOT_SUPPORTED))
        assertEquals(PageTranslationError.EngineDeactivated, mapped(TranslationsException.ERROR_ENGINE_DEACTIVATED))
        assertEquals(PageTranslationError.CouldNotTranslate, mapped(TranslationsException.ERROR_COULD_NOT_TRANSLATE))
        assertEquals(PageTranslationError.Unknown, mapped(TranslationsException.ERROR_UNKNOWN))
        assertEquals(PageTranslationError.Unknown, GeckoTranslationMapping.error(IllegalStateException()))
        assertEquals(PageTranslationError.Unknown, GeckoTranslationMapping.error(null))
    }

    @Test
    fun `only gecko offers on-device translation`() {
        assertTrue(AndroidBrowserEngineCapabilities.GeckoView.onDeviceTranslation)
        assertFalse(AndroidBrowserEngineCapabilities.SystemWebView.onDeviceTranslation)
    }
}
