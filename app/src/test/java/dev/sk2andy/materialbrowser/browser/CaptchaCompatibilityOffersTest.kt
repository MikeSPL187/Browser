package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptchaCompatibilityOffersTest {
    private val hcaptcha = "https://js.hcaptcha.com/1/api.js"
    private val tabs = mutableListOf(
        BrowserTab(id = "a", lastAccessedAt = 1, profileId = "home", url = "https://shop.example/login"),
        BrowserTab(id = "p", lastAccessedAt = 2, profileId = "home", url = "https://shop.example/", isIncognito = true),
    )
    private var selected = "a"
    private var blocksCookies = true
    private var relaxed = false
    private val generations = mutableMapOf("a" to 1, "p" to 1)
    private val mainThread = ArrayDeque<() -> Unit>()
    private val allowed = mutableListOf<Pair<String, Boolean>>()

    private val offers = CaptchaCompatibilityOffers(
        object : CaptchaCompatibilityOffers.Host {
            override val isDestroyed = false
            override val selectedTabId get() = selected
            override val blocksThirdPartyCookies get() = blocksCookies
            override fun tab(tabId: String) = tabs.firstOrNull { it.id == tabId }
            override fun pageUrl(tabId: String): String? = null
            override fun navigationGeneration(tabId: String) = generations[tabId]
            override fun isRelaxed(tab: BrowserTab, pageUrl: String) = relaxed
            override fun postToMainThread(task: () -> Unit) {
                mainThread.addLast(task)
            }
            override fun allow(tabId: String, persistently: Boolean) {
                allowed += tabId to persistently
            }
        },
    )

    private fun request(tabId: String = "a", url: String = hcaptcha, generation: Int = 1) {
        offers.onSubresourceRequest(tabId, url, "shop.example", "home", generation)
        while (mainThread.isNotEmpty()) mainThread.removeFirst()()
    }

    @Test
    fun aCaptchaOnTheSelectedPageIsOfferedOncePerLoad() {
        request()
        val first = offers.offer!!
        assertEquals(CaptchaProvider.HCaptcha, first.provider)

        request()
        assertEquals(first, offers.offer)

        generations["a"] = 2
        request(generation = 2)
        assertTrue(offers.offer!!.token > first.token)
    }

    @Test
    fun nothingIsOfferedWhenCookiesPassOrTheRequestIsNotACaptcha() {
        relaxed = true
        request()
        assertNull(offers.offer)

        relaxed = false
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
    fun allowingForTheWorkspaceIsRemembered() {
        request()
        val offer = offers.offer!!
        offers.showOptions(offer.token)
        assertTrue(offers.offer!!.showDialog)

        offers.respond(offer.token, CaptchaCompatibilityPromptChoice.AllowForProfile)

        assertEquals(listOf("a" to true), allowed)
        assertNull(offers.offer)
    }

    @Test
    fun aPrivateTabCannotRememberTheAnswerForTheWorkspace() {
        selected = "p"
        request(tabId = "p")
        val offer = offers.offer!!
        assertTrue(offer.isPrivate)

        offers.respond(offer.token, CaptchaCompatibilityPromptChoice.AllowForProfile)
        assertTrue(allowed.isEmpty())
    }

    @Test
    fun anAnswerAfterThePageMovedOnChangesNothing() {
        request()
        val offer = offers.offer!!
        generations["a"] = 2

        offers.respond(offer.token, CaptchaCompatibilityPromptChoice.AllowForTab)

        assertTrue(allowed.isEmpty())
    }

    @Test
    fun aCloudflareChallengeOnThePageItselfIsOffered() {
        offers.onMainFrameChallenge("a", "https://shop.example/login", "shop.example", "home", 1)

        assertEquals(CaptchaProvider.Cloudflare, offers.offer!!.provider)
    }

    @Test
    fun forgettingTheTabAllowsANewOfferAndClearingDropsIt() {
        request()
        offers.dismissFor("a")
        assertNull(offers.offer)
        request()
        assertNull(offers.offer)

        offers.forgetTab("a")
        request()
        assertFalse(offers.offer == null)

        offers.clear()
        assertNull(offers.offer)
    }
}
