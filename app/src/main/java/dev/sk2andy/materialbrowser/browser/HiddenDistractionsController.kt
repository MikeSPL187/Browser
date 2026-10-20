package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.blocking.PrivacyRequestSanitizer
import dev.sk2andy.materialbrowser.data.HiddenDistractions
import dev.sk2andy.materialbrowser.data.HiddenDistractionsRules
import dev.sk2andy.materialbrowser.data.HiddenDistractionsStorage

/**
 * «Hide distracting items» (v5 V20, after Safari): which page blocks each profile hid on each
 * site, and the choosing mode on a tab's page. A profile's list is read off the main thread the
 * first time one of its pages asks; until then its pages show what was hidden since, and the
 * list catches up once read. Hiding in a private tab keeps the block in memory only, for that
 * profile's private tabs, until the last private tab closes; nothing of it reaches the disk.
 *
 * [onChanged] sends the pages their new lists (the Gecko page policies); [pageFor] is the engine
 * port of a tab, null when the tab has no page.
 */
internal class HiddenDistractionsController(
    private val storage: HiddenDistractionsStorage,
    private val host: Host,
    private val pageFor: (tabId: String) -> PageDistractionsPort?,
    private val onChanged: () -> Unit,
) {
    interface Host {
        /** Runs [task] on the storage thread; tests run it at once. */
        fun runInBackground(task: () -> Unit) = task()

        /** Runs [action] on the main thread. */
        fun post(action: () -> Unit) = action()
    }

    private class Saved {
        var state: HiddenDistractions? = null
        var loading = false

        /** Changes made before [state] was read, applied to it once it is. */
        val pending = mutableListOf<(HiddenDistractions) -> HiddenDistractions>()

        fun current(): HiddenDistractions = state ?: pending.fold(HiddenDistractions()) { value, change -> change(value) }
    }

    private val saved = mutableMapOf<String, Saved>()
    private val privateSession = mutableMapOf<String, HiddenDistractions>()

    /**
     * Sites whose blocks a private tab showed again, by profile: private tabs show them there until
     * the private session ends, and the profile's saved list stays as it is on the disk.
     */
    private val privateShown = mutableMapOf<String, MutableSet<String>>()

    /** Grows with every change, so a screen that reads [hiddenCount] recomposes. */
    var revision by mutableLongStateOf(0L)
        private set

    /** The tab whose page is in choosing mode, or null. */
    var pickingTabId by mutableStateOf<String?>(null)
        private set

    /** What the last tap while choosing did, which the panel says aloud. */
    var pickingStep by mutableStateOf(DistractionPickingStep.Choosing)
        private set

    /** The block hidden last while choosing, which «Undo» shows again; gone when choosing ends. */
    private var lastHidden by mutableStateOf<LastHidden?>(null)

    private data class LastHidden(val profileId: String, val isPrivate: Boolean, val pageHost: String, val selector: String)

    /** Whether «Undo» has a block to show again: one was hidden since choosing started. */
    val canUndo: Boolean
        get() = lastHidden != null && pickingTabId != null

    /** What a page of [tab] at [pageUrl] hides: the profile's list and, in a private tab, the session's. */
    fun selectorsFor(tab: BrowserTab, pageUrl: String): List<String> {
        val pageHost = PrivacyRequestSanitizer.webHost(pageUrl) ?: return emptyList()
        val profile = savedFor(tab.profileId).current().selectorsFor(pageHost)
        if (!tab.isIncognito) return profile
        val shown = privateShown[tab.profileId]?.contains(pageHost) == true
        val session = privateSession[tab.profileId]?.selectorsFor(pageHost).orEmpty()
        return ((if (shown) emptyList() else profile) + session).distinct()
            .takeLast(HiddenDistractionsRules.MAX_SELECTORS_PER_SITE)
    }

    /** How many blocks [tab]'s page hides; read from a screen, it follows every change. */
    fun hiddenCount(tab: BrowserTab): Int = selectorsFor(tab, tab.url).size.takeIf { revision >= 0 } ?: 0

    /** Starts choosing on [tab]'s page; false when its page cannot. */
    fun startPicking(tab: BrowserTab, highlightArgb: Int): Boolean {
        stopPicking()
        val page = pageFor(tab.id) ?: return false
        val started = page.startPicking(
            highlightArgb,
            object : PageDistractionsListener {
                override fun onHidden(pageHost: String, selector: String) {
                    hide(tab.profileId, tab.isIncognito, pageHost, selector)
                    if (pickingTabId == tab.id) pickingStep = DistractionPickingStep.Hidden
                }

                override fun onOutlined(outlined: Boolean) {
                    if (pickingTabId == tab.id) {
                        pickingStep = if (outlined) DistractionPickingStep.Selected else DistractionPickingStep.Choosing
                    }
                }

                override fun onPickingEnded() {
                    if (pickingTabId == tab.id) {
                        pickingTabId = null
                        lastHidden = null
                    }
                }
            },
        )
        if (started) {
            pickingTabId = tab.id
            pickingStep = DistractionPickingStep.Choosing
        }
        return started
    }

    fun stopPicking() {
        lastHidden = null
        val tabId = pickingTabId ?: return
        pickingTabId = null
        pageFor(tabId)?.stopPicking()
    }

    /**
     * Shows again every block hidden on the site of [tab]'s page, «Show hidden items». From a
     * private tab it holds for private tabs only and in memory only: the saved list is not touched.
     */
    fun showAll(tab: BrowserTab) {
        lastHidden = null
        pickingStep = DistractionPickingStep.Choosing
        val pageHost = PrivacyRequestSanitizer.webHost(tab.url) ?: return
        privateSession[tab.profileId]?.let { session ->
            privateSession[tab.profileId] = HiddenDistractionsRules.showAll(session, pageHost)
        }
        if (tab.isIncognito) {
            privateShown.getOrPut(tab.profileId) { mutableSetOf() } += pageHost
            changed()
            return
        }
        change(tab.profileId) { HiddenDistractionsRules.showAll(it, pageHost) }
    }

    /**
     * Shows again the block hidden last while choosing (DS-03): a slip costs one block, not every
     * block of the site as «Show hidden items» would. Only once; then that is the way.
     */
    fun undoLastHide() {
        val last = lastHidden ?: return
        lastHidden = null
        pickingStep = DistractionPickingStep.Choosing
        if (last.isPrivate) {
            val session = privateSession[last.profileId] ?: return
            privateSession[last.profileId] = HiddenDistractionsRules.show(session, last.pageHost, last.selector)
            changed()
        } else {
            change(last.profileId) { HiddenDistractionsRules.show(it, last.pageHost, last.selector) }
        }
    }

    /** Clearing the browsing data forgets every hidden block: the sites alone tell where one has been. */
    fun clearAll() {
        lastHidden = null
        saved.clear()
        privateSession.clear()
        privateShown.clear()
        host.runInBackground { storage.clearAll() }
        changed()
    }

    /** A deleted profile's list goes with it. */
    fun forgetProfile(profileId: String) {
        if (lastHidden?.profileId == profileId) lastHidden = null
        saved.remove(profileId)
        privateSession.remove(profileId)
        privateShown.remove(profileId)
        host.runInBackground { storage.delete(profileId) }
        revision++
    }

    /** The last private tab closed: what its pages hid is forgotten. */
    fun endPrivateSession() {
        if (lastHidden?.isPrivate == true) lastHidden = null
        if (privateSession.isEmpty() && privateShown.isEmpty()) return
        privateSession.clear()
        privateShown.clear()
        revision++
    }

    private fun hide(profileId: String, isPrivate: Boolean, pageHost: String, selector: String) {
        if (HiddenDistractionsRules.isValidSelector(selector)) lastHidden = LastHidden(profileId, isPrivate, pageHost, selector)
        if (isPrivate) {
            privateSession[profileId] = HiddenDistractionsRules.hide(
                privateSession[profileId] ?: HiddenDistractions(),
                pageHost,
                selector,
            )
            changed()
        } else {
            change(profileId) { HiddenDistractionsRules.hide(it, pageHost, selector) }
        }
    }

    private fun change(profileId: String, update: (HiddenDistractions) -> HiddenDistractions) {
        val entry = savedFor(profileId)
        val state = entry.state
        if (state == null) {
            entry.pending += update
        } else {
            val next = update(state)
            if (next == state) return
            entry.state = next
            host.runInBackground { storage.save(profileId, next) }
        }
        changed()
    }

    private fun savedFor(profileId: String): Saved = saved.getOrPut(profileId) { Saved() }.also { entry ->
        if (entry.state != null || entry.loading) return@also
        entry.loading = true
        host.runInBackground {
            val loaded = storage.load(profileId)
            host.post { restored(profileId, entry, loaded) }
        }
    }

    private fun restored(profileId: String, entry: Saved, loaded: HiddenDistractions) {
        // A profile deleted while its list was being read stays deleted.
        if (saved[profileId] !== entry) return
        val state = entry.pending.fold(loaded) { value, change -> change(value) }
        val hadChanges = entry.pending.isNotEmpty()
        entry.pending.clear()
        entry.state = state
        entry.loading = false
        if (hadChanges && state != loaded) host.runInBackground { storage.save(profileId, state) }
        if (state.sites.isNotEmpty() || hadChanges) changed()
    }

    private fun changed() {
        revision++
        onChanged()
    }
}

/** What the last tap did while a page chooses distracting items. */
enum class DistractionPickingStep { Choosing, Selected, Hidden }
