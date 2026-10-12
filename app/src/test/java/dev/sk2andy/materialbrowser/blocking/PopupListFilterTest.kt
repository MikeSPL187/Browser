package dev.sk2andy.materialbrowser.blocking

import dev.sk2andy.materialbrowser.browser.PopupFilterDecision
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupListFilterTest {
    // A generous wait: a slow CI machine must not turn a parse delay into a missed block.
    private val filter = PopupListFilter(loadWaitMillis = 30_000) {
        File("src/blocking/assets/${PopupListFilter.ASSET}").readText()
    }

    @Test
    fun popupFromAListedOpenerIsBlocked() {
        assertEquals(
            PopupFilterDecision.Block,
            filter.decidePopup("https://landing.example/offer", "https://sextgem.com/video"),
        )
        assertEquals(
            PopupFilterDecision.NoMatch,
            filter.decidePopup("https://landing.example/offer", "https://news.example/story"),
        )
    }

    @Test
    fun popunderFromAListedOpenerIsBlocked() {
        assertEquals(
            PopupFilterDecision.Block,
            filter.decidePopunder("https://landing.example/offer", "https://cambro.tv/watch"),
        )
        assertEquals(
            PopupFilterDecision.NoMatch,
            filter.decidePopunder("https://landing.example/offer", "https://news.example/watch"),
        )
    }

    @Test
    fun windowOpenDefuserRulesAreLeftToThePage() {
        // `0xxx.ws` is listed only for the window.open defuser, which the extension applies.
        assertEquals(
            PopupFilterDecision.NoMatch,
            filter.decidePopup("https://landing.example/offer", "https://0xxx.ws/"),
        )
    }

    @Test
    fun aPopupBeforeTheListIsReadyGoesThroughWithoutWaiting() {
        val release = java.util.concurrent.CountDownLatch(1)
        val slow = PopupListFilter {
            release.await()
            File("src/blocking/assets/${PopupListFilter.ASSET}").readText()
        }
        val started = System.nanoTime()
        assertEquals(
            PopupFilterDecision.NoMatch,
            slow.decidePopup("https://landing.example/offer", "https://sextgem.com/video"),
        )
        assertTrue(System.nanoTime() - started < 100_000_000L)
        release.countDown()
    }

    @Test
    fun popupsOfBankAndPaymentSitesAreLetThrough() {
        val withSites = PopupListFilter(
            loadWaitMillis = 30_000,
            loadExemptSites = { File("src/blocking/gecko/${PopupListFilter.EXEMPT_SITES_ASSET}").readText() },
        ) {
            File("src/blocking/assets/${PopupListFilter.ASSET}").readText()
        }
        // A 3-D Secure window from a listed opener, and a bank's own popup.
        assertEquals(
            PopupFilterDecision.Allow,
            withSites.decidePopup("https://3ds.cardinalcommerce.com/acs", "https://sextgem.com/video"),
        )
        assertEquals(
            PopupFilterDecision.Allow,
            withSites.decidePopup("https://landing.example/offer", "https://online.sberbank.ru/"),
        )
        assertEquals(
            PopupFilterDecision.Allow,
            withSites.decidePopunder("https://www.paypal.com/checkout", "https://cambro.tv/watch"),
        )
        assertEquals(
            PopupFilterDecision.Block,
            withSites.decidePopup("https://landing.example/offer", "https://sextgem.com/video"),
        )
    }

    @Test
    fun unreadableListLetsPopupsThrough() {
        val broken = PopupListFilter(loadWaitMillis = 30_000) { error("missing") }
        assertEquals(
            PopupFilterDecision.NoMatch,
            broken.decidePopup("https://landing.example/offer", "https://sextgem.com/video"),
        )
    }
}
