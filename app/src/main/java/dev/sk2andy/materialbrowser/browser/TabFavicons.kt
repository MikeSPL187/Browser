package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tab icons (#500): the ones kept from the last run, the ones the engine reports and, where it
 * reports none, one fetched from the site. An icon only lands while its tab still shows the page it
 * belongs to; private and session-only tabs never write one to disk. [B] is the image
 * ([android.graphics.Bitmap] in the app), [S] the engine session.
 */
internal class TabFavicons<B : Any, S : Any>(
    /** The icons the screens show; shared with the controller, written only here. */
    val images: MutableMap<String, B>,
    private val store: Store<B>,
    private val isRecycled: (B) -> Boolean,
    private val recycle: (B) -> Unit,
    private val host: Host<S>,
) {
    interface Store<B> {
        /** Loads the kept icons of [tabIds]; [onLoaded] runs off the main thread. */
        fun restore(tabIds: Set<String>, onLoaded: (tabId: String, icon: B) -> Unit)

        fun save(tabId: String, icon: B)

        fun delete(tabId: String)

        fun clear()

        /** Fetches [pageUrl]'s icon off the main thread; false when it could not start. */
        fun fetch(pageUrl: String, shouldFetch: () -> Boolean, onLoaded: (B?) -> Unit): Boolean
    }

    interface Host<S> {
        val isDestroyed: Boolean

        /** Whether the engine reports no icons itself, so they are fetched. */
        val fetchesIcons: Boolean

        fun tab(tabId: String): BrowserTab?

        /** A popup or extension page that lives for this run only. */
        fun isSessionOnly(tabId: String): Boolean

        fun session(tabId: String): S?

        fun navigationGeneration(tabId: String): Int

        fun postToMainThread(task: () -> Unit)
    }

    private class FetchAttempt<S>(
        val session: S,
        val pageUrl: String,
        val navigationGeneration: Int,
        val epoch: Int,
    ) {
        val cancelled = AtomicBoolean(false)

        fun sameAs(other: FetchAttempt<S>?): Boolean = other != null &&
            other.session === session &&
            other.pageUrl == pageUrl &&
            other.navigationGeneration == navigationGeneration &&
            other.epoch == epoch
    }

    /** Clearing browsing data starts a new epoch: nothing from before it may land. */
    private var epoch = 0
    private val generations = mutableMapOf<String, Int>()
    private val fetchAttempts = mutableMapOf<String, FetchAttempt<S>>()

    private fun keepsOnDisk(tab: BrowserTab): Boolean = !tab.isIncognito && !host.isSessionOnly(tab.id)

    /** At start: the regular tabs' kept icons, unless a fresher one arrived meanwhile. */
    fun restore(tabIds: Set<String>) {
        val restoreEpoch = epoch
        val restoreGenerations = tabIds.associateWith { tabId -> generations[tabId] ?: 0 }
        store.restore(tabIds) { tabId, icon ->
            host.postToMainThread {
                if (
                    !host.isDestroyed &&
                    epoch == restoreEpoch &&
                    generations.getOrDefault(tabId, 0) == restoreGenerations[tabId] &&
                    host.tab(tabId)?.isIncognito == false &&
                    images[tabId] == null
                ) {
                    images[tabId] = icon
                } else {
                    recycle(icon)
                }
            }
        }
    }

    fun store(tabId: String, icon: B) {
        val tab = host.tab(tabId)
        if (isRecycled(icon) || tab == null) return
        images[tabId] = icon
        if (keepsOnDisk(tab)) store.save(tabId, icon)
    }

    /** The engine reported [icon] for [pageUrl] in [tabId]'s [session]. */
    fun onEngineIcon(tabId: String, session: S, pageUrl: String?, icon: B) {
        if (host.isDestroyed || host.session(tabId) !== session || isRecycled(icon)) return
        val tab = host.tab(tabId) ?: return
        if (!FaviconRules.belongsToDocument(tab.url, pageUrl)) return
        store(tabId, icon)
    }

    /** [tabId] loaded [pageUrl] and has no icon yet: fetch the site's. */
    fun scheduleFetch(tabId: String, pageUrl: String) {
        if (!host.fetchesIcons || images[tabId] != null) return
        val safeUrl = BrowserUriPolicy.normalizeHttpUrl(pageUrl) ?: return
        val tab = host.tab(tabId) ?: return
        if (!keepsOnDisk(tab) || !FaviconRules.belongsToDocument(tab.url, safeUrl)) return
        val session = host.session(tabId) ?: return
        val attempt = FetchAttempt(session, safeUrl, host.navigationGeneration(tabId), epoch)
        if (attempt.sameAs(fetchAttempts[tabId])) return
        fetchAttempts.put(tabId, attempt)?.cancelled?.set(true)
        val accepted = store.fetch(safeUrl, shouldFetch = { !attempt.cancelled.get() }) { icon ->
            host.postToMainThread {
                val currentTab = host.tab(tabId)
                if (
                    icon != null &&
                    !isRecycled(icon) &&
                    !host.isDestroyed &&
                    fetchAttempts[tabId] === attempt &&
                    host.session(tabId) === session &&
                    host.navigationGeneration(tabId) == attempt.navigationGeneration &&
                    epoch == attempt.epoch &&
                    currentTab != null &&
                    keepsOnDisk(currentTab) &&
                    images[tabId] == null &&
                    FaviconRules.belongsToDocument(currentTab.url, safeUrl)
                ) {
                    store(tabId, icon)
                } else {
                    icon?.takeUnless(isRecycled)?.let(recycle)
                }
            }
        }
        if (!accepted && fetchAttempts[tabId] === attempt) fetchAttempts.remove(tabId)
    }

    /** [tabId] left its page or closed: its icon goes, and so does any still on its way. */
    fun invalidate(tabId: String) {
        generations[tabId] = generations.getOrDefault(tabId, 0) + 1
        fetchAttempts.remove(tabId)?.cancelled?.set(true)
        images.remove(tabId)
        store.delete(tabId)
    }

    /** [tabId] is gone for good: its count of invalidations goes too. */
    fun forgetTab(tabId: String) {
        generations.remove(tabId)
    }

    /** The controller is destroyed: icons in memory go, nothing on its way lands. */
    fun release() {
        images.clear()
        generations.clear()
        cancelFetches()
    }

    /** Browsing data was cleared: every icon goes, kept ones too. */
    fun clearAll() {
        epoch++
        release()
        store.clear()
    }

    private fun cancelFetches() {
        fetchAttempts.values.forEach { it.cancelled.set(true) }
        fetchAttempts.clear()
    }
}
