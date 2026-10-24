package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.blocking.PrivacyRequestSanitizer
import java.util.concurrent.ConcurrentHashMap

/**
 * The offer to let «Sign in with Google» and similar logins through blocked third-party cookies
 * (#500): noticing a login provider on the selected page, offering it once per site and provider
 * in a tab, and applying the answer for the page or for the workspace. Detection may run on a
 * network thread; the offer itself changes on the main thread only.
 */
internal class FederatedLoginOffers(private val host: Host) {
    interface Host {
        val isDestroyed: Boolean
        val selectedTabId: String
        val blocksThirdPartyCookies: Boolean

        fun tab(tabId: String): BrowserTab?

        /** The address the page reports, which may differ from the tab's while it loads. */
        fun pageUrl(tabId: String): String?

        fun navigationGeneration(tabId: String): Int?

        /** Third-party logins already pass on [pageUrl]. */
        fun isAllowed(tab: BrowserTab, pageUrl: String?): Boolean

        fun postToMainThread(task: () -> Unit)

        /** Lets third-party logins through on the tab's site, for this page or for its workspace. */
        fun allow(tabId: String, persistently: Boolean)
    }

    var offer by mutableStateOf<FederatedLoginOffer?>(null)
        private set
    private var sequence = 0L
    private val offeredKeys = ConcurrentHashMap<String, String>()

    /** A subresource of [pageHost]'s page in [tabId] went to [requestUrl]; any thread. */
    fun onSubresourceRequest(
        tabId: String,
        requestUrl: String,
        pageHost: String?,
        profileId: String,
        navigationGeneration: Int,
    ) {
        if (!host.blocksThirdPartyCookies || pageHost == null) return
        val provider = FederatedLoginRules.providerForSubresource(
            requestUrl = requestUrl,
            pageUrl = "https://$pageHost/",
        ) ?: return
        host.postToMainThread {
            if (host.isDestroyed || host.selectedTabId != tabId) return@postToMainThread
            val tab = host.tab(tabId) ?: return@postToMainThread
            val currentHost = PrivacyRequestSanitizer.webHost(host.pageUrl(tabId) ?: tab.url)
            if (currentHost != pageHost || tab.profileId != profileId) return@postToMainThread
            if (host.navigationGeneration(tabId) != navigationGeneration) return@postToMainThread
            if (host.isAllowed(tab, host.pageUrl(tabId))) return@postToMainThread
            val key = "$pageHost:${provider.name}"
            if (offeredKeys[tabId] == key) return@postToMainThread
            offeredKeys[tabId] = key
            sequence++
            offer = FederatedLoginOffer(
                token = sequence,
                tabId = tabId,
                profileId = tab.profileId,
                pageHost = pageHost,
                provider = provider,
                isPrivate = tab.isIncognito,
                navigationGeneration = navigationGeneration,
            )
        }
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

    fun respond(token: Long, choice: FederatedLoginPromptChoice) {
        val offer = offer?.takeIf { it.token == token } ?: return
        this.offer = null
        if (choice == FederatedLoginPromptChoice.Deny || !isCurrent(offer)) return
        if (choice == FederatedLoginPromptChoice.AllowForProfile && offer.isPrivate) return
        host.allow(offer.tabId, persistently = choice == FederatedLoginPromptChoice.AllowForProfile)
    }

    private fun isCurrent(offer: FederatedLoginOffer): Boolean {
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
