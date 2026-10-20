package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.blocking.PrivacyRequestSanitizer
import java.util.concurrent.ConcurrentHashMap

/**
 * The offer to let a site's captcha through blocked third-party cookies (#500): noticing a captcha
 * provider on the selected page, offering it once per page load and provider, and applying the
 * answer for the page or for the workspace. Detection may run on a network thread; the offer
 * itself changes on the main thread only.
 */
internal class CaptchaCompatibilityOffers(private val host: Host) {
    interface Host {
        val isDestroyed: Boolean
        val selectedTabId: String
        val blocksThirdPartyCookies: Boolean

        fun tab(tabId: String): BrowserTab?

        /** The address the page reports, which may differ from the tab's while it loads. */
        fun pageUrl(tabId: String): String?

        fun navigationGeneration(tabId: String): Int?

        /** Protection is paused or third-party cookies already pass on [pageUrl]. */
        fun isRelaxed(tab: BrowserTab, pageUrl: String): Boolean

        fun postToMainThread(task: () -> Unit)

        /** Lets captchas through on the tab's site, for this page or for its workspace. */
        fun allow(tabId: String, persistently: Boolean)
    }

    var offer by mutableStateOf<CaptchaCompatibilityOffer?>(null)
        private set
    private var sequence = 0L
    private val offeredKeys = ConcurrentHashMap<String, MutableSet<String>>()

    /** A subresource of [pageHost]'s page in [tabId] went to [requestUrl]; any thread. */
    fun onSubresourceRequest(
        tabId: String,
        requestUrl: String,
        pageHost: String?,
        profileId: String,
        navigationGeneration: Int,
    ) {
        if (!host.blocksThirdPartyCookies || pageHost == null) return
        val provider = CaptchaCompatibilityRules.providerForSubresource(
            requestUrl = requestUrl,
            pageUrl = "https://$pageHost/",
        ) ?: return
        host.postToMainThread {
            if (host.isDestroyed || host.selectedTabId != tabId || !host.blocksThirdPartyCookies) {
                return@postToMainThread
            }
            val tab = host.tab(tabId) ?: return@postToMainThread
            val pageUrl = host.pageUrl(tabId) ?: tab.url
            if (PrivacyRequestSanitizer.webHost(pageUrl) != pageHost || tab.profileId != profileId) {
                return@postToMainThread
            }
            if (host.navigationGeneration(tabId) != navigationGeneration) return@postToMainThread
            if (host.isRelaxed(tab, pageUrl)) return@postToMainThread
            queue(tab, pageHost, provider, navigationGeneration)
        }
    }

    /** The selected page in [tabId] itself answered with a Cloudflare challenge; main thread. */
    fun onMainFrameChallenge(
        tabId: String,
        responseUrl: String,
        pageHost: String?,
        profileId: String,
        navigationGeneration: Int,
    ) {
        if (!host.blocksThirdPartyCookies || pageHost == null) return
        if (PrivacyRequestSanitizer.webHost(responseUrl) != pageHost) return
        if (host.isDestroyed || host.selectedTabId != tabId) return
        val tab = host.tab(tabId) ?: return
        val pageUrl = host.pageUrl(tabId) ?: tab.url
        if (PrivacyRequestSanitizer.webHost(pageUrl) != pageHost) return
        if (tab.profileId != profileId) return
        if (host.navigationGeneration(tabId) != navigationGeneration) return
        if (host.isRelaxed(tab, pageUrl)) return
        queue(tab, pageHost, CaptchaProvider.Cloudflare, navigationGeneration)
    }

    private fun queue(tab: BrowserTab, pageHost: String, provider: CaptchaProvider, navigationGeneration: Int) {
        val key = "$navigationGeneration:$pageHost:${provider.name}"
        val tabKeys = offeredKeys.computeIfAbsent(tab.id) { ConcurrentHashMap.newKeySet() }
        if (!tabKeys.add(key)) return
        sequence++
        offer = CaptchaCompatibilityOffer(
            token = sequence,
            tabId = tab.id,
            profileId = tab.profileId,
            pageHost = pageHost,
            provider = provider,
            isPrivate = tab.isIncognito,
            navigationGeneration = navigationGeneration,
        )
    }

    /** The snackbar's action: the offer turns into its dialog, if the page is still the same. */
    fun showOptions(token: Long) {
        val offer = offer?.takeIf { it.token == token } ?: return
        if (!isCurrent(offer)) {
            this.offer = null
            return
        }
        this.offer = offer.copy(showDialog = true)
    }

    fun respond(token: Long, choice: CaptchaCompatibilityPromptChoice) {
        val offer = offer?.takeIf { it.token == token } ?: return
        this.offer = null
        if (choice == CaptchaCompatibilityPromptChoice.Deny || !isCurrent(offer)) return
        if (choice == CaptchaCompatibilityPromptChoice.AllowForProfile && offer.isPrivate) return
        host.allow(offer.tabId, persistently = choice == CaptchaCompatibilityPromptChoice.AllowForProfile)
    }

    private fun isCurrent(offer: CaptchaCompatibilityOffer): Boolean {
        val tab = host.tab(offer.tabId) ?: return false
        val currentHost = PrivacyRequestSanitizer.webHost(host.pageUrl(tab.id) ?: tab.url)
        return host.selectedTabId == tab.id &&
            tab.profileId == offer.profileId &&
            tab.isIncognito == offer.isPrivate &&
            currentHost == offer.pageHost &&
            host.navigationGeneration(tab.id) == offer.navigationGeneration
    }

    /** [tabId] left the screen: its offer goes with it. */
    fun dismissFor(tabId: String) {
        if (offer?.tabId == tabId) offer = null
    }

    /** [tabId] closed: a new tab with its id may be offered again. */
    fun forgetTab(tabId: String) {
        offeredKeys.remove(tabId)
    }

    fun clear() {
        offer = null
        offeredKeys.clear()
    }
}
