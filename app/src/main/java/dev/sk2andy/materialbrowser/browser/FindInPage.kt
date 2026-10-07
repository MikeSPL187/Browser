package dev.sk2andy.materialbrowser.browser

/** How the page is searched. Engines without these options ignore them (see the port). */
internal data class FindInPageOptions(
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
)

internal data class FindInPageState(
    val tabId: String,
    val query: String = "",
    val options: FindInPageOptions = FindInPageOptions(),
    val activeMatchOrdinal: Int? = null,
    val matchCount: Int = 0,
    val isDoneCounting: Boolean = true,
    /** The engine found the query on the page, even when it has not counted the matches yet. */
    val isMatchFound: Boolean = false,
)

internal data class FindInPageMatchPosition(
    val activeMatchNumber: Int,
    val matchCount: Int,
    /** False when the page has matches but the engine never reported how many. */
    val isCountKnown: Boolean = true,
)

internal object FindInPageRules {
    /** How long to wait before asking the engine again for a count it did not report. */
    const val RECOUNT_DELAY_MILLIS = 400L

    /** How many times to ask again before showing the match without a total. */
    const val MAX_RECOUNTS = 2

    fun withQuery(state: FindInPageState, query: String): FindInPageState {
        if (query == state.query) return state
        return state.copy(
            query = query,
            activeMatchOrdinal = null,
            matchCount = 0,
            isDoneCounting = query.isEmpty(),
            isMatchFound = false,
        )
    }

    /**
     * Applies an engine result. A result that found the query but reports no matches is not a
     * final "0/0": GeckoView can answer before it has counted (seen in the tour on GeckoView 156),
     * so the state keeps counting and [needsRecount] asks the engine again.
     */
    fun withResult(
        state: FindInPageState,
        activeMatchOrdinal: Int,
        matchCount: Int,
        isDoneCounting: Boolean,
        found: Boolean = matchCount > 0,
    ): FindInPageState {
        if (state.query.isEmpty()) return withQuery(state, state.query)
        val normalizedMatchCount = matchCount.coerceAtLeast(0)
        val isMatchFound = found || normalizedMatchCount > 0
        return state.copy(
            activeMatchOrdinal = when {
                normalizedMatchCount > 0 -> activeMatchOrdinal.coerceIn(0, normalizedMatchCount - 1)
                isMatchFound -> activeMatchOrdinal.coerceAtLeast(0)
                else -> null
            },
            matchCount = normalizedMatchCount,
            isDoneCounting = isDoneCounting && (normalizedMatchCount > 0 || !isMatchFound),
            isMatchFound = isMatchFound,
        )
    }

    /** Whether the page has matches the engine has not counted yet. */
    fun needsRecount(state: FindInPageState): Boolean =
        state.query.isNotEmpty() && state.isMatchFound && !state.isDoneCounting

    /** Stops waiting for a count the engine keeps withholding: the match shows without a total. */
    fun withoutCount(state: FindInPageState): FindInPageState =
        if (needsRecount(state)) state.copy(isDoneCounting = true) else state

    /**
     * Stops waiting when the engine gave no answer (a failed search or a closed page): whatever is
     * already known stays, so a new query shows "0/0" instead of counting forever.
     */
    fun withoutResult(state: FindInPageState): FindInPageState =
        if (state.query.isEmpty() || state.isDoneCounting) state else state.copy(isDoneCounting = true)

    fun canNavigate(state: FindInPageState): Boolean =
        state.query.isNotEmpty() && (state.matchCount > 0 || state.isMatchFound)

    fun displayPosition(state: FindInPageState): FindInPageMatchPosition {
        val normalizedMatchCount = state.matchCount.coerceAtLeast(0)
        if (normalizedMatchCount == 0 && state.isMatchFound && state.query.isNotEmpty()) {
            return FindInPageMatchPosition(
                activeMatchNumber = (state.activeMatchOrdinal ?: 0).coerceAtLeast(0) + 1,
                matchCount = 0,
                isCountKnown = false,
            )
        }
        val activeMatchNumber = if (normalizedMatchCount == 0) {
            0
        } else {
            (state.activeMatchOrdinal ?: 0)
                .coerceIn(0, normalizedMatchCount - 1) + 1
        }
        return FindInPageMatchPosition(
            activeMatchNumber = activeMatchNumber,
            matchCount = normalizedMatchCount,
        )
    }
}
