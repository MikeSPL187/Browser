package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageTranslationRecoveryTest {
    private val source = "https://mt.cc/download/"
    private val googleResult = "https://mt-cc.translate.goog/download/"
    private val tabUrls = mutableMapOf("a" to source, "b" to "https://other.example/")
    private var selected = "a"
    private val generations = mutableMapOf("a" to 1, "b" to 1)
    private val sessions = mutableMapOf("a" to "session-a", "b" to "session-b")
    private val navigations = mutableListOf<String>()
    private val delayed = ArrayDeque<() -> Unit>()
    private val pageReads = ArrayDeque<(String?) -> Unit>()

    private val recovery = PageTranslationRecovery(
        object : PageTranslationRecovery.Host<String> {
            override val isDestroyed = false
            override val selectedTabId get() = selected
            override fun tabUrl(tabId: String) = tabUrls[tabId]
            override fun navigationGeneration(tabId: String) = generations.getOrDefault(tabId, 0)
            override fun targetLanguage() = "ru"
            override fun navigate(url: String) {
                navigations += url
            }
            override fun session(tabId: String) = sessions[tabId]
            override fun extractPage(session: String, onComplete: (String?) -> Unit) {
                pageReads.addLast(onComplete)
            }
            override fun postDelayed(task: () -> Unit, delayMillis: Long) {
                delayed.addLast(task)
            }
        },
    )

    private fun commitResult(tabId: String = "a") {
        tabUrls[tabId] = googleResult
        recovery.onEngineEvent(tabId, BrowserEngineEventType.NavigationCommitted, googleResult, 200)
    }

    private fun readPage(visible: Boolean) {
        delayed.removeFirst()()
        pageReads.removeFirst()("""{"hasVisibleContent":$visible,"blocks":[]}""")
    }

    @Test
    fun translatingOpensTheProviderPageInTheTargetLanguage() {
        recovery.translate("a", source, PageTranslationProvider.Google)

        assertTrue(navigations.single().startsWith("https://translate.google.com/translate?sl=auto&tl=ru&u="))
    }

    @Test
    fun aVisibleResultEndsTheWatchWithoutAnOffer() {
        recovery.translate("a", source, PageTranslationProvider.Google)
        commitResult()

        readPage(visible = true)

        assertNull(recovery.offer)
        assertTrue(delayed.isEmpty())
    }

    @Test
    fun anEmptyResultTwiceOffersAnotherProvider() {
        recovery.translate("a", source, PageTranslationProvider.Google)
        commitResult()

        readPage(visible = false)
        assertNull(recovery.offer)
        readPage(visible = false)

        val offer = recovery.offer!!
        assertEquals(PageTranslationRecoveryAction.TryYandex, offer.action)
        assertEquals(googleResult, offer.failedUrl)

        recovery.recover(offer.token)
        assertNull(recovery.offer)
        assertTrue(navigations.last().startsWith("https://translate.yandex.com/translate?url="))
    }

    @Test
    fun aProviderHttpErrorOffersRecoveryAtOnce() {
        recovery.translate("a", source, PageTranslationProvider.Google)

        recovery.onEngineEvent("a", BrowserEngineEventType.StateChanged, googleResult, 503)

        assertNotNull(recovery.offer)
    }

    @Test
    fun aFailureOnABackgroundTabOffersNothing() {
        recovery.translate("a", source, PageTranslationProvider.Google)
        selected = "b"

        recovery.onEngineEvent("a", BrowserEngineEventType.NavigationFailed, googleResult, null)

        assertNull(recovery.offer)
    }

    @Test
    fun leavingForAnotherSiteStopsWatching() {
        recovery.translate("a", source, PageTranslationProvider.Google)

        recovery.onEngineEvent("a", BrowserEngineEventType.NavigationStarted, "https://news.example/", null)
        recovery.onEngineEvent("a", BrowserEngineEventType.NavigationFailed, googleResult, null)

        assertNull(recovery.offer)
    }

    @Test
    fun aCheckAfterTheTabNavigatedIsDropped() {
        recovery.translate("a", source, PageTranslationProvider.Google)
        commitResult()
        generations["a"] = 2

        delayed.removeFirst()()

        assertTrue(pageReads.isEmpty())
    }

    @Test
    fun anOfferForAPageThatMovedOnDoesNothing() {
        recovery.translate("a", source, PageTranslationProvider.Google)
        recovery.onEngineEvent("a", BrowserEngineEventType.NavigationFailed, googleResult, null)
        val token = recovery.offer!!.token
        generations["a"] = 2
        navigations.clear()

        recovery.recover(token)

        assertTrue(navigations.isEmpty())
        assertNull(recovery.offer)
    }

    @Test
    fun dismissingOrLeavingTheTabClearsTheOffer() {
        recovery.translate("a", source, PageTranslationProvider.Google)
        recovery.onEngineEvent("a", BrowserEngineEventType.NavigationFailed, googleResult, null)
        recovery.dismiss(token = -1)
        assertNotNull(recovery.offer)

        recovery.dismissOfferFor("a")

        assertNull(recovery.offer)
    }
}
