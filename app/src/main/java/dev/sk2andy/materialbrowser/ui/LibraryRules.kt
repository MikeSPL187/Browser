package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.data.FavoriteLibraryEntry
import dev.sk2andy.materialbrowser.data.HistoryClearRequest
import dev.sk2andy.materialbrowser.data.HistoryEntry
import java.text.Collator
import java.util.Locale

/** Where a row sits in its card: the card rounds only its outer corners. */
internal enum class LibraryRowPosition {
    Single,
    First,
    Middle,
    Last,
    ;

    val roundsTop: Boolean get() = this == Single || this == First
    val roundsBottom: Boolean get() = this == Single || this == Last
}

/**
 * History waiting behind «Undo»: hidden at once, deleted only when the undo window closes or the
 * screen goes away.
 */
internal sealed interface HistoryPendingDeletion {
    data class Entries(val entries: List<HistoryEntry>, val keys: Set<String>) : HistoryPendingDeletion

    data class Clear(val request: HistoryClearRequest) : HistoryPendingDeletion
}

/**
 * A [deletion] behind «Undo» and the wall-clock moment its window closes, kept across recreation
 * and process death so a rotation neither loses «Undo» nor confirms the deletion by itself.
 */
internal data class HistoryUndo(
    val deletion: HistoryPendingDeletion,
    val deadlineMillis: Long,
)

/** The current folder's level, split as on board W-Favorites: folder cards, then loose sites. */
internal data class FavoritesLevel(
    val folders: List<FavoriteFolder>,
    val favorites: List<FavoriteEntry>,
)

/** How a favorites level is ordered: the user's own order (the one dragging changes), by name, or newest first. */
internal enum class FavoritesSort {
    Manual,
    Name,
    Recent,
}

internal object LibraryRules {
    /** How long «Undo» stays on screen after deleting history. */
    const val UNDO_WINDOW_MILLIS = 5_000L

    /** The first character of a label, upper-cased; a whole code point, so emoji stay whole. */
    fun initial(label: String): String {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return ""
        return String(Character.toChars(trimmed.codePointAt(0))).uppercase()
    }

    /** The same site always gets the same tile color. */
    fun tileIndex(key: String, count: Int): Int {
        require(count > 0)
        return Math.floorMod(key.trim().lowercase().hashCode(), count)
    }

    fun position(index: Int, size: Int): LibraryRowPosition = when {
        size <= 1 -> LibraryRowPosition.Single
        index == 0 -> LibraryRowPosition.First
        index == size - 1 -> LibraryRowPosition.Last
        else -> LibraryRowPosition.Middle
    }

    fun withoutPending(
        entries: List<HistoryEntry>,
        pending: HistoryPendingDeletion?,
        keyOf: (HistoryEntry) -> String,
    ): List<HistoryEntry> = when (pending) {
        null -> entries
        is HistoryPendingDeletion.Entries -> entries.filterNot { keyOf(it) in pending.keys }
        is HistoryPendingDeletion.Clear -> entries.filterNot { clears(pending.request, it) }
    }

    /** How long «Undo» still stays: never negative, never longer than a fresh [windowMillis]. */
    fun undoRemainingMillis(undo: HistoryUndo, nowMillis: Long, windowMillis: Long): Long =
        (undo.deadlineMillis - nowMillis).coerceIn(0L, windowMillis.coerceAtLeast(0L))

    /** [undo] as bundle-friendly values for rememberSaveable; read back by [restoreUndo]. */
    fun saveUndo(undo: HistoryUndo): ArrayList<Any> = when (val deletion = undo.deletion) {
        is HistoryPendingDeletion.Entries -> arrayListOf(
            UNDO_ENTRIES,
            undo.deadlineMillis,
            ArrayList(deletion.keys),
            deletion.entries.flatMapTo(ArrayList()) { entry ->
                listOf(
                    entry.url,
                    entry.title,
                    entry.lastVisitedAt.toString(),
                    entry.profileId,
                    entry.visitId,
                )
            },
        )
        is HistoryPendingDeletion.Clear -> arrayListOf(
            UNDO_CLEAR,
            undo.deadlineMillis,
            ArrayList(deletion.request.profileIds),
            deletion.request.sinceInclusiveMillis,
            deletion.request.untilExclusiveMillis,
        )
    }

    /** The «Undo» state [saveUndo] wrote, or null when [saved] is not one. */
    fun restoreUndo(saved: Any?): HistoryUndo? = runCatching {
        val values = saved as List<*>
        val deadline = values[1] as Long
        val deletion = when (values[0]) {
            UNDO_ENTRIES -> HistoryPendingDeletion.Entries(
                entries = (values[3] as List<*>).map { it as String }
                    .chunked(UNDO_ENTRY_FIELDS) { fields ->
                        require(fields.size == UNDO_ENTRY_FIELDS)
                        HistoryEntry(
                            url = fields[0],
                            title = fields[1],
                            lastVisitedAt = fields[2].toLong(),
                            profileId = fields[3],
                            visitId = fields[4],
                        )
                    },
                keys = (values[2] as List<*>).mapTo(linkedSetOf()) { it as String },
            )
            UNDO_CLEAR -> HistoryPendingDeletion.Clear(
                HistoryClearRequest(
                    profileIds = (values[2] as List<*>).mapTo(linkedSetOf()) { it as String },
                    sinceInclusiveMillis = values[3] as Long,
                    untilExclusiveMillis = values[4] as Long,
                ),
            )
            else -> return null
        }
        HistoryUndo(deletion, deadline)
    }.getOrNull()

    private const val UNDO_ENTRIES = "entries"
    private const val UNDO_CLEAR = "clear"
    private const val UNDO_ENTRY_FIELDS = 5

    fun clears(request: HistoryClearRequest, entry: HistoryEntry): Boolean =
        entry.profileId in request.profileIds &&
            entry.lastVisitedAt >= request.sinceInclusiveMillis &&
            entry.lastVisitedAt < request.untilExclusiveMillis

    fun level(entries: List<FavoriteLibraryEntry>): FavoritesLevel = FavoritesLevel(
        folders = entries.filterIsInstance<FavoriteFolder>(),
        favorites = entries.filterIsInstance<FavoriteEntry>(),
    )

    /**
     * The level in [sort] order. Folders follow names when sorting by name; they have no date, so by
     * date they keep the user's order. Equal keys keep the user's order too.
     */
    fun sorted(level: FavoritesLevel, sort: FavoritesSort, locale: Locale): FavoritesLevel {
        if (sort == FavoritesSort.Manual) return level
        val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        val byName = compareBy<String, String>(collator) { it.trim() }
        return when (sort) {
            FavoritesSort.Manual -> level
            FavoritesSort.Name -> FavoritesLevel(
                folders = level.folders.sortedWith(compareBy(byName, FavoriteFolder::title)),
                favorites = level.favorites.sortedWith(compareBy(byName) { it.title.ifBlank { it.url } }),
            )
            FavoritesSort.Recent -> level.copy(
                favorites = level.favorites.sortedByDescending(FavoriteEntry::addedAt),
            )
        }
    }

    /**
     * Where «Move earlier» ([step] -1) or «Move later» ([step] +1) puts [entryId]: the index in
     * [siblings] of its neighbour in [group], the folder cards or the sites it is shown among.
     * Null at the group's edge. Folders and sites interleave in [siblings], so a neighbour there may
     * be of the other kind, and moving past it would save an order the screen never shows.
     */
    fun reorderTarget(
        siblings: List<FavoriteLibraryEntry>,
        group: List<FavoriteLibraryEntry>,
        entryId: String,
        step: Int,
    ): Int? {
        val position = group.indexOfFirst { it.id == entryId }
        if (position < 0) return null
        val neighbour = group.getOrNull(position + step) ?: return null
        return siblings.indexOfFirst { it.id == neighbour.id }.takeIf { it >= 0 }
    }

    /** Sites in a folder and its subfolders, for «12 sites» under the folder's name. */
    fun siteCount(library: FavoriteLibrary, folderId: String): Int {
        val visited = hashSetOf<String>()
        fun count(parentId: String): Int {
            if (!visited.add(parentId)) return 0
            return BrowsingFavoritesRules.children(library, parentId).sumOf { entry ->
                when (entry) {
                    is FavoriteEntry -> 1
                    is FavoriteFolder -> count(entry.id)
                }
            }
        }
        if (BrowsingFavoritesRules.folder(library, folderId) == null) return 0
        return count(folderId)
    }
}
