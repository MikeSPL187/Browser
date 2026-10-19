package dev.sk2andy.materialbrowser.browser.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceTranslationControllerTest {
    private class FakePort : PageTranslationPort {
        var current: PageTranslationListener? = null
        var translated: Pair<TranslationPair, Boolean>? = null
        var answer: ((PageTranslationResult) -> Unit)? = null

        override fun setListener(listener: PageTranslationListener?) {
            current = listener
        }

        override fun translate(
            pair: TranslationPair,
            allowModelDownload: Boolean,
            onResult: (PageTranslationResult) -> Unit,
        ) {
            translated = pair to allowModelDownload
            answer = onResult
        }

        override fun restoreOriginal(onResult: (PageTranslationResult) -> Unit) = onResult(PageTranslationResult.Done)

        override fun neverTranslateSite(onResult: (Boolean?) -> Unit) = onResult(false)

        override fun setNeverTranslateSite(never: Boolean, onResult: (PageTranslationResult) -> Unit) =
            onResult(PageTranslationResult.Done)
    }

    private val english = PageTranslationState(pageLanguage = "en", pageLanguageSupported = true, userLanguage = "ru")
    private val pair = TranslationPair("en", "ru")
    private val port = FakePort()
    private val controller = OnDeviceTranslationController(TranslationModelsPort.Unavailable) { listOf("ru-RU") }

    @Test
    fun `the page's language from the engine makes the menu offer a pair`() {
        controller.attach("tab", port)
        port.current?.onStateChanged(english)

        assertEquals(pair, controller.suggestedPair("tab"))
        assertNull(controller.suggestedPair("other"))
    }

    @Test
    fun `translating waits for the engine and ends when the page changed`() {
        controller.attach("tab", port)
        port.current?.onStateChanged(english)

        controller.translate("tab", pair, allowModelDownload = true)
        assertEquals(pair to true, port.translated)
        assertTrue(controller.stateOf("tab").inProgress)

        port.current?.onStateChanged(english.copy(requested = pair, isTranslated = true))
        assertFalse(controller.stateOf("tab").inProgress)
        assertTrue(controller.stateOf("tab").isTranslated)
        port.answer?.invoke(PageTranslationResult.Done)
        assertNull(controller.stateOf("tab").error)
    }

    @Test
    fun `an older request's answer does not end the wait of the newer one`() {
        controller.attach("tab", port)
        controller.translate("tab", pair, allowModelDownload = true)
        val first = port.answer
        controller.translate("tab", TranslationPair("en", "de"), allowModelDownload = true)

        first?.invoke(PageTranslationResult.Failed(PageTranslationError.ModelDownloadFailed))

        assertTrue(controller.stateOf("tab").inProgress)
        assertNull(controller.stateOf("tab").error)
        port.answer?.invoke(PageTranslationResult.Done)
        assertFalse(controller.stateOf("tab").inProgress)
    }

    @Test
    fun `an answer from the page before a navigation does not reach the new page`() {
        controller.attach("tab", port)
        controller.translate("tab", pair, allowModelDownload = true)
        val oldAnswer = port.answer
        val newPort = FakePort()
        controller.attach("tab", newPort)
        controller.translate("tab", pair, allowModelDownload = true)

        oldAnswer?.invoke(PageTranslationResult.Failed(PageTranslationError.ModelDownloadFailed))

        assertTrue(controller.stateOf("tab").inProgress)
        assertNull(controller.stateOf("tab").error)
    }

    @Test
    fun `a refused download is reported, not hidden`() {
        controller.attach("tab", port)
        controller.translate("tab", pair, allowModelDownload = false)
        port.answer?.invoke(PageTranslationResult.Failed(PageTranslationError.ModelDownloadRequired))

        assertFalse(controller.stateOf("tab").inProgress)
        assertEquals(PageTranslationError.ModelDownloadRequired, controller.stateOf("tab").error)
    }

    @Test
    fun `the original clears the request, and a closed page forgets everything`() {
        controller.attach("tab", port)
        controller.translate("tab", pair, allowModelDownload = true)
        controller.showOriginal("tab")
        assertNull(controller.stateOf("tab").pair)

        controller.detach("tab")
        assertNull(port.current)
        assertEquals(OnDeviceTranslationUiState(), controller.stateOf("tab"))
    }

    @Test
    fun `a tab without a page cannot translate`() {
        var result: PageTranslationResult? = null
        controller.translate("none", pair, allowModelDownload = true) { result = it }

        assertEquals(PageTranslationResult.Unsupported, result)
    }
}
