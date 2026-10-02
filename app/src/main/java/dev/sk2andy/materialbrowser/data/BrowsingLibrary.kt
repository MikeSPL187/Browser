package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_ID
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class HistoryEntry(
    val url: String,
    val title: String,
    val lastVisitedAt: Long,
    val profileId: String = DEFAULT_PROFILE_ID,
    val visitId: String = "",
)

internal data class HistoryDaySection(
    val date: LocalDate,
    val entries: List<HistoryEntry>,
)

internal data class HistoryClearRequest(
    val profileIds: Set<String>,
    val sinceInclusiveMillis: Long,
    val untilExclusiveMillis: Long,
)

data class AddressSuggestion(
    val url: String,
    val title: String,
    val openTabId: String? = null,
    /** The workspace of [openTabId] when it is not the current one; null for the current one. */
    val openTabProfileId: String? = null,
    val source: AddressSuggestionSource = if (openTabId == null) {
        AddressSuggestionSource.History
    } else {
        AddressSuggestionSource.OpenTab
    },
    /** When the page was last visited or saved, for the subtitle; null when unknown. */
    val lastVisitedAt: Long? = null,
)

/** Where an address suggestion comes from; the row shows it with its own icon. */
enum class AddressSuggestionSource {
    OpenTab,
    Favorite,
    History,
}

internal object BrowsingLibraryRules {
    const val MAX_HISTORY_ENTRIES = 250
    const val MAX_FAVORITES = 100

    fun addHistory(
        current: List<HistoryEntry>,
        entry: HistoryEntry,
        limit: Int = MAX_HISTORY_ENTRIES,
    ): List<HistoryEntry> {
        if (historyKey(entry) == null) return current
        if (entry.profileId.isBlank()) return current
        val safeTitle = entry.title.trim().ifEmpty { displayHost(entry.url) }
        return (listOf(entry.copy(title = safeTitle)) + current)
            .sortedByDescending(HistoryEntry::lastVisitedAt)
            .take(limit.coerceAtLeast(0))
    }

    fun suggestions(
        history: List<HistoryEntry>,
        query: String,
        limit: Int,
    ): List<HistoryEntry> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        return history.asSequence()
            .filter { urlKey(it.url) != null }
            .mapNotNull { entry ->
                val title = entry.title.lowercase(Locale.ROOT)
                val url = entry.url.lowercase(Locale.ROOT)
                val host = displayHost(entry.url).lowercase(Locale.ROOT)
                val score = when {
                    normalizedQuery.isEmpty() -> 0
                    host.startsWith(normalizedQuery) -> 4
                    title.startsWith(normalizedQuery) -> 3
                    url.startsWith(normalizedQuery) -> 2
                    host.contains(normalizedQuery) ||
                        title.contains(normalizedQuery) ||
                        url.contains(normalizedQuery) -> 1
                    else -> return@mapNotNull null
                }
                ScoredHistory(entry, score)
            }
            .sortedWith(
                compareByDescending<ScoredHistory> { it.score }
                    .thenByDescending { it.entry.lastVisitedAt },
            )
            .distinctBy { scored -> urlKey(scored.entry.url) }
            .map(ScoredHistory::entry)
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    /**
     * Open tabs, favorites and history that match [query], best first; open tabs lead so the
     * address bar offers "Switch to tab" before a second copy of the page.
     *
     * [otherWorkspaceTabs] are tabs of the other workspaces the caller may reveal: not locked and
     * not private (proposal П1 in docs/vola/ROADMAP.md). A private tab sees only private tabs.
     */
    fun addressSuggestions(
        history: List<HistoryEntry>,
        tabs: List<BrowserTab>,
        selectedTabId: String,
        isIncognito: Boolean,
        query: String,
        limit: Int,
        includeHistory: Boolean = true,
        favorites: List<FavoriteEntry> = emptyList(),
        otherWorkspaceTabs: List<BrowserTab> = emptyList(),
    ): List<AddressSuggestion> {
        val candidates = linkedMapOf<String, AddressSuggestion>()
        fun addTabs(source: List<BrowserTab>, otherWorkspace: Boolean) {
            source.asSequence()
                .filter { tab ->
                    tab.id != selectedTabId &&
                        tab.isIncognito == isIncognito &&
                        tab.url != BLANK_URL
                }
                .sortedByDescending(BrowserTab::lastAccessedAt)
                .forEach { tab ->
                    val key = urlKey(tab.url) ?: return@forEach
                    candidates.putIfAbsent(
                        key,
                        AddressSuggestion(
                            url = tab.url,
                            title = tab.title.trim().ifEmpty { displayHost(tab.url) },
                            openTabId = tab.id,
                            openTabProfileId = tab.profileId.takeIf { otherWorkspace },
                            lastVisitedAt = tab.lastAccessedAt,
                        ),
                    )
                }
        }
        addTabs(tabs, otherWorkspace = false)
        if (!isIncognito) {
            addTabs(otherWorkspaceTabs, otherWorkspace = true)
            val lastVisits = if (includeHistory) {
                history.asSequence()
                    .mapNotNull { entry -> urlKey(entry.url)?.let { key -> key to entry.lastVisitedAt } }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, visits) -> visits.max() }
            } else {
                emptyMap()
            }
            favorites.forEach { favorite ->
                val key = urlKey(favorite.url) ?: return@forEach
                candidates.putIfAbsent(
                    key,
                    AddressSuggestion(
                        url = favorite.url,
                        title = favorite.title.trim().ifEmpty { displayHost(favorite.url) },
                        source = AddressSuggestionSource.Favorite,
                        lastVisitedAt = maxOf(lastVisits[key] ?: 0L, favorite.addedAt),
                    ),
                )
            }
            if (includeHistory) {
                history.forEach { entry ->
                    val key = urlKey(entry.url) ?: return@forEach
                    candidates.putIfAbsent(
                        key,
                        AddressSuggestion(
                            url = entry.url,
                            title = entry.title,
                            lastVisitedAt = entry.lastVisitedAt,
                        ),
                    )
                }
            }
        }

        val matches = suggestions(
            history = candidates.values.map { suggestion ->
                HistoryEntry(
                    url = suggestion.url,
                    title = suggestion.title,
                    lastVisitedAt = suggestion.lastVisitedAt ?: 0L,
                )
            },
            query = query,
            limit = candidates.size,
        )
        return matches
            .mapNotNull { entry -> urlKey(entry.url)?.let(candidates::get) }
            .sortedByDescending { suggestion ->
                when {
                    suggestion.openTabId == null -> 0
                    suggestion.openTabProfileId == null -> 2
                    else -> 1
                }
            }
            .take(limit.coerceAtLeast(0))
    }

    fun domainCompletion(
        history: List<HistoryEntry>,
        favorites: List<FavoriteEntry>,
        tabs: List<BrowserTab>,
        selectedTabId: String,
        isIncognito: Boolean,
        query: String,
        includeHistory: Boolean = true,
    ): String? {
        val value = query.trim()
        val prefix = value.lowercase(Locale.ROOT)
        if (
            prefix.isEmpty() ||
            value != query ||
            prefix.any(Char::isWhitespace) ||
            prefix.any { it == '/' || it == ':' || it == '@' }
        ) {
            return null
        }
        val candidateUrls = buildList {
            tabs.asSequence()
                .filter { it.id != selectedTabId && it.isIncognito == isIncognito }
                .sortedByDescending(BrowserTab::lastAccessedAt)
                .map(BrowserTab::url)
                .forEach(::add)
            if (!isIncognito) {
                favorites.asSequence().map(FavoriteEntry::url).forEach(::add)
                if (includeHistory) {
                    history.asSequence()
                        .sortedByDescending(HistoryEntry::lastVisitedAt)
                        .map(HistoryEntry::url)
                        .forEach(::add)
                }
            }
        }
        return candidateUrls.asSequence()
            .mapNotNull(::completionHost)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .firstOrNull { host ->
                host.length > prefix.length && host.lowercase(Locale.ROOT).startsWith(prefix)
            }
    }

    fun toggleFavorite(
        current: List<FavoriteEntry>,
        entry: FavoriteEntry,
        limit: Int = MAX_FAVORITES,
    ): List<FavoriteEntry> {
        val key = urlKey(entry.url) ?: return current
        val existing = current.any { urlKey(it.url) == key }
        if (existing) return current.filterNot { urlKey(it.url) == key }
        val safeTitle = entry.title.trim().ifEmpty { displayHost(entry.url) }
        return (listOf(entry.copy(title = safeTitle)) + current)
            .distinctBy { urlKey(it.url) }
            .take(limit.coerceAtLeast(0))
    }

    fun isFavorite(favorites: List<FavoriteEntry>, url: String): Boolean {
        val key = urlKey(url) ?: return false
        return favorites.any { urlKey(it.url) == key }
    }

    private fun urlKey(url: String): String? = CanonicalWebUrl.key(url)

    private fun historyKey(entry: HistoryEntry): String? =
        urlKey(entry.url)?.let { key -> "${entry.profileId}\u0000$key" }

    private fun displayHost(url: String): String = runCatching {
        URI(url).host?.removePrefix("www.")
    }.getOrNull().orEmpty().ifEmpty { url }

    private fun completionHost(url: String): String? = runCatching {
        URI(url).host?.removePrefix("www.")?.takeIf(String::isNotBlank)
    }.getOrNull()

    private data class ScoredHistory(
        val entry: HistoryEntry,
        val score: Int,
    )
}

internal object BrowsingHistoryRules {
    private val newestFirst = compareByDescending(HistoryEntry::lastVisitedAt)

    fun visibleEntries(
        history: List<HistoryEntry>,
        selectedProfileIds: Set<String>,
        query: String,
    ): List<HistoryEntry> {
        if (selectedProfileIds.isEmpty()) return emptyList()
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        return history.asSequence()
            .filter { entry ->
                entry.profileId in selectedProfileIds &&
                    CanonicalWebUrl.key(entry.url) != null
            }
            .filter { entry ->
                normalizedQuery.isEmpty() ||
                    entry.title.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                    entry.url.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                    displayHost(entry.url).lowercase(Locale.ROOT).contains(normalizedQuery)
            }
            .sortedWith(newestFirst)
            .toList()
    }

    fun distinctEntries(entries: List<HistoryEntry>): List<HistoryEntry> = entries
        .sortedWith(newestFirst)
        .distinctBy { entry ->
            entry.profileId to (CanonicalWebUrl.key(entry.url) ?: entryKey(entry))
        }

    fun sections(
        entries: List<HistoryEntry>,
        zoneId: ZoneId,
    ): List<HistoryDaySection> = entries
        .groupBy { entry ->
            Instant.ofEpochMilli(entry.lastVisitedAt).atZone(zoneId).toLocalDate()
        }
        .entries
        .sortedByDescending(Map.Entry<LocalDate, *>::key)
        .map { (date, dayEntries) ->
            HistoryDaySection(
                date = date,
                entries = dayEntries.sortedByDescending(HistoryEntry::lastVisitedAt),
            )
        }

    fun removeEntries(
        history: List<HistoryEntry>,
        entries: Collection<HistoryEntry>,
    ): List<HistoryEntry> {
        val removedKeys = entries.mapTo(hashSetOf(), ::entryKey)
        if (removedKeys.isEmpty()) return history
        return history.filterNot { entry -> entryKey(entry) in removedKeys }
    }

    fun removeProfiles(
        history: List<HistoryEntry>,
        profileIds: Set<String>,
    ): List<HistoryEntry> = history.filterNot { entry -> entry.profileId in profileIds }

    fun removeRange(
        history: List<HistoryEntry>,
        request: HistoryClearRequest,
    ): List<HistoryEntry> {
        if (
            request.profileIds.isEmpty() ||
            request.sinceInclusiveMillis >= request.untilExclusiveMillis
        ) {
            return history
        }
        return history.filterNot { entry ->
            entry.profileId in request.profileIds &&
                entry.lastVisitedAt >= request.sinceInclusiveMillis &&
                entry.lastVisitedAt < request.untilExclusiveMillis
        }
    }

    fun entryKey(entry: HistoryEntry): String = buildString {
        if (entry.visitId.isNotBlank()) {
            append(entry.visitId)
            return@buildString
        }
        append(entry.profileId)
        append('\u0000')
        append(CanonicalWebUrl.key(entry.url).orEmpty())
        append('\u0000')
        append(entry.lastVisitedAt)
    }

    private fun displayHost(url: String): String = runCatching {
        URI(url).host?.removePrefix("www.")
    }.getOrNull().orEmpty()
}
