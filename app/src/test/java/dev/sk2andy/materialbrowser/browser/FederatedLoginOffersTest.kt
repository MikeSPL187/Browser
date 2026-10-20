package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FederatedLoginOffersTest {
    private val googleClient = "https://accounts.google.com/gsi/client"
    private val tabs = mutableListOf(
        BrowserTab(id = "a", lastAccessedAt = 1, profileId = "home", url = "https://shop.example/login"),
        BrowserTab(id = "p", lastAccessedAt = 2, profileId = "home", url = "https://shop.example/", isIncognito = true),
    )
    private var selected = "a"
    private var blocksCookies = true
    private var loginAllowed = false
    private val generations = mutableMapOf("a" to 1, "p" to 1)
    private val mainThread = ArrayDeque<() -> Unit>()
    private val allowed = mutableListOf<Pair<String, Boolean>>()

    private val offers = FederatedLoginOffers(
        object : FederatedLoginOffers.Host {
            override val isDestroyed = false
            override val selectedTabId get() = selected
            override val blocksThirdPartyCookies get() = blocksCookies
            override fun tab(tabId: String) = tabs.firstOrNull { it.id == tabId }
            override fun pageUrl(tabId: String): String? = null
            override fun navigationGeneration(tabId: String) = generations[tabId]
            override fun isAllowed(tab: BrowserTab, pageUrl: String?) = loginAllowed
            override fun postToMainThread(task: () -> Unit) {
                mainThread.addLast(task)
            }
            override fun allow(tabId: String, persistently: Boolean) {
                allowed += tabId to persistently
            }
        },
    )

    private fun request(tabId: String = "a", url: String = googleClient, generation: Int = 1) {
        offers.onSubresourceRequest(tabId, url, "shop.example", "home", generation)
        while (mainThread.isNotEmpty()) mainThread.removeFirst()()
    }

    @Test
    fun aLoginProviderOnTheSelectedPageIsOfferedOncePerSite() {
        request()
        val first = offers.offer!!
        assertEquals(FederatedLoginProvider.Google, first.provider)

        offers.dismissFor("a")
        generations["a"] = 2
        request(generation = 2)
        assertNull(offers.offer)
    }

    @Test
    fun nothingIsOfferedWhenLoginsPassCookiesPassOrTheRequestIsUnrelated() {
        loginAllowed = true
        request()
        assertNull(offers.offer)

        loginAllowed = false
        request(url = "https://cdn.example/app.js")
        assertNull(offers.offer)

        blocksCookies = false
        request()
        assertNull(offers.offer)
    }

    @Test
    fun aBackgroundTabOrAStaleLoadOffersNothing() {
        selected = "p"
        request(tabId = "a")
        assertNull(offers.offer)

        selected = "a"
        generations["a"] = 3
        request(generation = 1)
        assertNull(offers.offer)
    }

    @Test
    fun allowingForThePageAppliesOnce() {
        request()
        val offer = offers.offer!!
        offers.showOptions(offer.token)
        assertTrue(offers.offer!!.showDialog)

        offers.respond(offer.token, FederatedLoginPromptChoice.AllowForTab)

        assertEquals(listOf("a" to false), allowed)
        assertNull(offers.offer)
    }

    @Test
    fun aPrivateTabCannotRememberTheAnswerForTheWorkspace() {
        selected = "p"
        request(tabId = "p")

        offers.respond(offers.offer!!.token, FederatedLoginPromptChoice.AllowForProfile)

        assertTrue(allowed.isEmpty())
    }

    @Test
    fun showingOptionsForAPageThatMovedOnDropsTheOffer() {
        request()
        val token = offers.offer!!.token
        generations["a"] = 2

        offers.showOptions(token)

        assertNull(offers.offer)
    }

    @Test
    fun forgettingTheTabOrClearingAllowsANewOffer() {
        request()
        offers.dismissFor("a")
        request()
        assertNull(offers.offer)

        offers.forgetTab("a")
        request()
        assertNotNull(offers.offer)

        offers.clear()
        assertNull(offers.offer)
        request()
        assertNotNull(offers.offer)
    }
}
