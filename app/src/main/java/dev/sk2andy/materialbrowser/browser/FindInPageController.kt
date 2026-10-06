package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.gecko.BrowserEngineFindPort

/** One find-in-page run: the page it searches and the navigation it started on. */
internal data class FindInPageSession(
    val id: Long,
    val tabId: String,
    val engineSession: BrowserEngineFindPort,
    val navigationGeneration: Int,
)

/**
 * Find in page for the selected tab or the link preview. [BrowserController] owns one, opens it on
 * the page to search and answers through [Host] whether that page is still the one on screen.
 *
 * When the engine finds the query but has not counted the matches, it asks again a little later
 * (see [FindInPageRules.withResult]) instead of showing "0/0".
 */
class FindInPageController internal constructor(
    private val host: Host,
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
) {
    internal fun interface Host {
        /** Whether [session] still searches the page the user sees. */
        fun isCurrent(session: FindInPageSession): Boolean
    }

    internal var state by mutableStateOf<FindInPageState?>(null)
        private set

    private var session: FindInPageSession? = null
    private var nextSessionId = 0L
    private var pendingRecount: Runnable? = null

    /** The engine session being searched, if any. */
    internal val engineSession: BrowserEngineFindPort?
        get() = session?.engineSession

    /** Whether the find bar can offer match case and whole word for the page being searched. */
    internal val supportsOptions: Boolean
        get() = session?.engineSession?.supportsFindInPageOptions == true

    internal fun open(
        tabId: String,
        engineSession: BrowserEngineFindPort,
        navigationGeneration: Int,
        resetOptions: Boolean,
    ) {
        close()
        session = FindInPageSession(
            id = ++nextSessionId,
            tabId = tabId,
            engineSession = engineSession,
            navigationGeneration = navigationGeneration,
        )
        if (resetOptions) engineSession.setFindInPageOptions(FindInPageOptions())
        state = FindInPageState(tabId = tabId)
    }

    fun updateQuery(query: String) {
        val session = session ?: return
        val current = state?.takeIf { it.tabId == session.tabId } ?: return
        val updated = FindInPageRules.withQuery(current, query)
        if (updated === current) return
        cancelRecount()
        state = updated
        if (query.isEmpty()) {
            session.engineSession.clearFindInPage()
        } else {
            search(session, query, forward = true, recountsLeft = FindInPageRules.MAX_RECOUNTS)
        }
    }

    fun findNext(forward: Boolean): Boolean {
        val session = session ?: return false
        val current = state ?: return false
        if (!FindInPageRules.canNavigate(current)) return false
        cancelRecount()
        search(session, current.query, forward, recountsLeft = FindInPageRules.MAX_RECOUNTS)
        return true
    }

    /** Changes how the page is searched and repeats the current search with it. */
    internal fun updateOptions(options: FindInPageOptions) {
        val session = session ?: return
        val current = state?.takeIf { it.tabId == session.tabId } ?: return
        if (current.options == options) return
        session.engineSession.setFindInPageOptions(options)
        val query = current.query
        state = FindInPageRules.withQuery(current.copy(options = options), query = "")
        if (query.isNotEmpty()) updateQuery(query)
    }

    /**
     * Follows a navigation of [engineSession], now at [navigationGeneration]. A new document closes
     * find: its matches belong to the page that is going away. A URL change within the same document
     * (pushState, replaceState, a fragment) keeps find open on the new generation; sites change the
     * URL while scrolling, and searching again would move the user to another match each time.
     */
    internal fun onNavigation(
        engineSession: BrowserEngineFindPort,
        navigationGeneration: Int,
        sameDocument: Boolean,
    ) {
        val session = session?.takeIf { it.engineSession === engineSession } ?: return
        if (sameDocument) {
            this.session = session.copy(navigationGeneration = navigationGeneration)
        } else {
            close()
        }
    }

    fun close() {
        cancelRecount()
        val closing = session
        session = null
        state = null
        nextSessionId++
        closing?.engineSession?.clearFindInPage()
    }

    private fun search(
        session: FindInPageSession,
        query: String,
        forward: Boolean,
        recountsLeft: Int,
    ) {
        session.engineSession.findInPage(query = query, forward = forward) { result ->
            val current = state
            if (
                result == null ||
                current == null ||
                !isSearching(session, query)
            ) {
                return@findInPage
            }
            val updated = FindInPageRules.withResult(
                state = current,
                activeMatchOrdinal = result.activeMatchOrdinal,
                matchCount = result.matchCount,
                isDoneCounting = result.isDoneCounting,
                found = result.found,
            )
            state = when {
                !FindInPageRules.needsRecount(updated) -> updated
                recountsLeft > 0 -> updated.also { scheduleRecount(session, query, recountsLeft - 1) }
                else -> FindInPageRules.withoutCount(updated)
            }
        }
    }

    /**
     * Asks for the count again without moving: one match back and forward again, so the result
     * that carries the count lands on the match the user already sees.
     */
    private fun scheduleRecount(session: FindInPageSession, query: String, recountsLeft: Int) {
        cancelRecount()
        val recount = Runnable {
            pendingRecount = null
            if (!isSearching(session, query)) return@Runnable
            session.engineSession.findInPage(query = query, forward = false) { back ->
                if (back == null || !isSearching(session, query)) return@findInPage
                search(session, query, forward = true, recountsLeft = recountsLeft)
            }
        }
        pendingRecount = recount
        postDelayed(recount, FindInPageRules.RECOUNT_DELAY_MILLIS)
    }

    private fun cancelRecount() {
        pendingRecount?.let(removeCallbacks)
        pendingRecount = null
    }

    /**
     * Whether a request made in [session] for [query] still belongs to the open find. The page is
     * checked against the session as it is now, so a request in flight survives [onNavigation]
     * rebinding it to a new URL of the same document.
     */
    private fun isSearching(session: FindInPageSession, query: String): Boolean {
        val current = state ?: return false
        val active = this.session ?: return false
        return active.id == session.id &&
            current.tabId == active.tabId &&
            current.query == query &&
            host.isCurrent(active)
    }
}
