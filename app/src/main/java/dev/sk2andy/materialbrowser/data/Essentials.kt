package dev.sk2andy.materialbrowser.data

import java.net.URI
import java.util.Locale

/**
 * A site pinned to a workspace's new tab: Zen's Essentials. Each workspace keeps its own list;
 * bookmarks and folders stay in Favorites.
 */
data class EssentialEntry(
    val url: String,
    val title: String,
) {
    /** One entry per page: two links to the same page share this key. */
    val id: String
        get() = CanonicalWebUrl.key(url) ?: url.trim()
}

/** An open tab the add sheet offers. */
data class EssentialCandidate(
    val tabId: String,
    val url: String,
    val title: String,
)

/** The pure rules behind [dev.sk2andy.materialbrowser.browser.EssentialsController]. */
object EssentialsRules {
    /** Four rows of four: the grid stays within the thumb zone of a phone. */
    const val MAX_ENTRIES = 16

    /** The first run copies at most three rows of the old new tab favorites. */
    const val MIGRATED_ENTRY_LIMIT = 12

    sealed interface AddResult {
        data class Added(val entries: List<EssentialEntry>) : AddResult
        data object Duplicate : AddResult
        data object Full : AddResult
        data object Unsupported : AddResult
    }

    /** Drops pages that are not http(s), repeats and anything past [MAX_ENTRIES]. */
    fun normalize(entries: List<EssentialEntry>): List<EssentialEntry> =
        entries.asSequence()
            .filter { entry -> CanonicalWebUrl.key(entry.url) != null }
            .distinctBy(EssentialEntry::id)
            .map { entry -> entry.copy(url = entry.url.trim(), title = entry.title.trim()) }
            .take(MAX_ENTRIES)
            .toList()

    fun add(entries: List<EssentialEntry>, url: String, title: String): AddResult {
        val entry = EssentialEntry(url = url.trim(), title = title.trim())
        return when {
            CanonicalWebUrl.key(entry.url) == null -> AddResult.Unsupported
            entries.any { it.id == entry.id } -> AddResult.Duplicate
            entries.size >= MAX_ENTRIES -> AddResult.Full
            else -> AddResult.Added(entries + entry)
        }
    }

    /** Puts a removed entry back where it was, unless the page came back some other way. */
    fun restore(entries: List<EssentialEntry>, entry: EssentialEntry, index: Int): List<EssentialEntry>? {
        if (entries.any { it.id == entry.id } || entries.size >= MAX_ENTRIES) return null
        return entries.toMutableList().apply { add(index.coerceIn(0, size), entry) }
    }

    fun move(entries: List<EssentialEntry>, id: String, toIndex: Int): List<EssentialEntry> {
        val from = entries.indexOfFirst { it.id == id }
        if (from < 0) return entries
        val target = toIndex.coerceIn(0, entries.lastIndex)
        if (target == from) return entries
        return entries.toMutableList().apply { add(target, removeAt(from)) }
    }

    /**
     * The first run after Essentials replaced the favorites grid: the sites shown at the top level
     * of the old grid, in its order. Folders and their contents stay in Favorites only.
     */
    fun migrationSeed(library: FavoriteLibrary): List<EssentialEntry> =
        normalize(
            library.entries
                .filterIsInstance<FavoriteEntry>()
                .filter { favorite -> favorite.parentFolderId == null }
                .map { favorite -> EssentialEntry(url = favorite.url, title = favorite.title) },
        ).take(MIGRATED_ENTRY_LIMIT)

    /**
     * The workspace a new workspace copies its Essentials from: «Personal» (the first workspace)
     * when it still exists, otherwise the first workspace that has a list.
     */
    fun templateProfileId(
        stored: Map<String, List<EssentialEntry>>,
        profileIds: List<String>,
        defaultProfileId: String,
    ): String? =
        defaultProfileId.takeIf(stored::containsKey)
            ?: profileIds.firstOrNull(stored::containsKey)

    /** Open tabs the add sheet offers: real pages that are not pinned yet, one row per page. */
    fun candidates(
        entries: List<EssentialEntry>,
        tabs: List<EssentialCandidate>,
    ): List<EssentialCandidate> {
        val pinned = entries.mapTo(hashSetOf(), EssentialEntry::id)
        return tabs
            .filter { tab -> CanonicalWebUrl.key(tab.url) != null }
            .distinctBy { tab -> CanonicalWebUrl.key(tab.url) }
            .filterNot { tab -> CanonicalWebUrl.key(tab.url) in pinned }
    }

    /** The tile caption: the page title, or the site name when the page has none. */
    fun label(entry: EssentialEntry): String =
        entry.title.ifBlank { host(entry.url) }

    /** The letter on a tile without an icon. */
    fun monogram(entry: EssentialEntry): String =
        label(entry).firstOrNull(Char::isLetterOrDigit)?.uppercase(Locale.ROOT)
            ?: host(entry.url).take(1).uppercase(Locale.ROOT)

    fun host(url: String): String =
        runCatching { URI(url.trim()).host }.getOrNull()
            ?.lowercase(Locale.ROOT)
            ?.removePrefix("www.")
            ?: url.trim()
}
