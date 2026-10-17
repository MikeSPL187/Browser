package dev.sk2andy.materialbrowser.shared.sync.webdav

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferMergeRules

/** What a sync changes in this phone's bookmarks. */
data class WebDavBookmarkChanges(
    /** Bookmarks this phone lacks, in their folders. */
    val add: List<LocalTransferBookmark>,
    /** Addresses, as this phone stores them, deleted on another device. */
    val remove: List<String>,
    /** This phone's bookmarks renamed or moved on another device: its address, the new title and folder. */
    val update: List<LocalTransferBookmark>,
) {
    val isEmpty: Boolean get() = add.isEmpty() && remove.isEmpty() && update.isEmpty()
}

/**
 * How the devices agree on the bookmarks (V27). Every device publishes its view of every bookmark,
 * live or a tombstone, with the time it last changed; the merge keeps the newest state of each
 * ([newer]). The merge does not depend on order or repetition, so devices that sync in any order
 * end with the same bookmarks. A deletion stays a tombstone for [TOMBSTONE_RETENTION_MILLIS], so it
 * does not come back from a device that still had the bookmark.
 */
object WebDavBookmarkRules {
    /** How long a deletion is remembered; a device away for longer may bring a deleted bookmark back. */
    const val TOMBSTONE_RETENTION_MILLIS = 180L * 24 * 60 * 60 * 1_000

    /**
     * The winner of two states of one bookmark: the later change; at the same time a deletion,
     * then the greater title, folder and address, so every device picks the same one.
     */
    fun newer(first: WebDavBookmarkEntry, second: WebDavBookmarkEntry): WebDavBookmarkEntry {
        val order = compareValuesBy(
            first,
            second,
            WebDavBookmarkEntry::modifiedAtMillis,
            WebDavBookmarkEntry::deleted,
            WebDavBookmarkEntry::title,
            WebDavBookmarkEntry::folder,
            WebDavBookmarkEntry::url,
        )
        return if (order >= 0) first else second
    }

    /**
     * This phone's view of the bookmarks: [current] on the phone against [remembered], what it
     * held after the last sync ([remember]). A bookmark that is new or changed since gets the time
     * [nowMillis] (or just after its last change, should the clock be behind); one that went is a
     * tombstone. Before the first sync ([remembered] is null) nothing is known about the age of
     * the phone's bookmarks, so they count as the oldest: a deletion another device already
     * synced wins over them, and the ones the server lacks are added there.
     */
    fun journal(
        remembered: Map<String, WebDavBookmarkEntry>?,
        current: List<LocalTransferBookmark>,
        nowMillis: Long,
    ): Map<String, WebDavBookmarkEntry> {
        val result = LinkedHashMap<String, WebDavBookmarkEntry>()
        current.forEach { bookmark ->
            val key = LocalTransferMergeRules.bookmarkKey(bookmark.url)
            if (key in result) return@forEach
            val previous = remembered?.get(key)
            result[key] = when {
                remembered == null -> bookmark.entry(modifiedAtMillis = 0)
                previous != null && !previous.deleted && previous.title == bookmark.title && previous.folder == bookmark.folder ->
                    previous.copy(url = bookmark.url)
                else -> bookmark.entry(modifiedAtMillis = after(previous, nowMillis))
            }
        }
        remembered?.forEach { (key, previous) ->
            if (key in result) return@forEach
            when {
                !previous.deleted -> result[key] = previous.copy(modifiedAtMillis = after(previous, nowMillis), deleted = true)
                isKept(previous, nowMillis) -> result[key] = previous
            }
        }
        return result
    }

    /** [views] merged ([newer] for each bookmark), without tombstones past [TOMBSTONE_RETENTION_MILLIS]. */
    fun merge(views: List<Map<String, WebDavBookmarkEntry>>, nowMillis: Long): Map<String, WebDavBookmarkEntry> {
        val merged = LinkedHashMap<String, WebDavBookmarkEntry>()
        views.forEach { view ->
            view.forEach { (key, entry) -> merged[key] = merged[key]?.let { newer(it, entry) } ?: entry }
        }
        merged.values.removeAll { it.deleted && !isKept(it, nowMillis) }
        if (merged.size > WebDavSyncFormat.MAX_BOOKMARK_ENTRIES) {
            merged.values.filter { it.deleted }
                .sortedBy { it.modifiedAtMillis }
                .take(merged.size - WebDavSyncFormat.MAX_BOOKMARK_ENTRIES)
                .forEach { merged.remove(it.key) }
        }
        return merged
    }

    /** What makes [current] match [merged]. */
    fun changes(current: List<LocalTransferBookmark>, merged: Map<String, WebDavBookmarkEntry>): WebDavBookmarkChanges {
        val present = HashSet<String>()
        val remove = ArrayList<String>()
        val update = ArrayList<LocalTransferBookmark>()
        current.forEach { bookmark ->
            val key = LocalTransferMergeRules.bookmarkKey(bookmark.url)
            present += key
            val entry = merged[key] ?: return@forEach
            when {
                entry.deleted -> remove += bookmark.url
                entry.title != bookmark.title || entry.folder != bookmark.folder ->
                    update += LocalTransferBookmark(bookmark.url, entry.title, entry.folder)
            }
        }
        val add = merged.values
            .filter { !it.deleted && it.key !in present }
            .sortedWith(compareBy(WebDavBookmarkEntry::folder, WebDavBookmarkEntry::title, WebDavBookmarkEntry::url))
            .map { LocalTransferBookmark(it.url, it.title, it.folder) }
        return WebDavBookmarkChanges(add, remove, update)
    }

    /**
     * What to remember after a sync for the next [journal]: the bookmarks the phone [applied]
     * holds now, at [merged]'s times, and [merged]'s tombstones. Only bookmarks the phone really
     * holds count as its own, so one that did not fit the favorites limit is not taken for a
     * deletion next time; one whose removal or update failed stays just older than [merged]'s state,
     * so the next sync applies that state again.
     */
    fun remember(merged: Map<String, WebDavBookmarkEntry>, applied: List<LocalTransferBookmark>): Map<String, WebDavBookmarkEntry> {
        val result = LinkedHashMap<String, WebDavBookmarkEntry>()
        applied.forEach { bookmark ->
            val key = LocalTransferMergeRules.bookmarkKey(bookmark.url)
            if (key in result) return@forEach
            val entry = merged[key]
            result[key] = when {
                entry == null -> bookmark.entry(modifiedAtMillis = 0)
                entry.deleted || entry.title != bookmark.title || entry.folder != bookmark.folder ->
                    bookmark.entry(modifiedAtMillis = (entry.modifiedAtMillis - 1).coerceAtLeast(0))
                else -> entry.copy(url = bookmark.url)
            }
        }
        merged.forEach { (key, entry) -> if (entry.deleted && key !in result) result[key] = entry }
        return result
    }

    private fun LocalTransferBookmark.entry(modifiedAtMillis: Long) = WebDavBookmarkEntry(url, title, folder, modifiedAtMillis)

    private fun after(previous: WebDavBookmarkEntry?, nowMillis: Long): Long =
        maxOf(nowMillis, (previous?.modifiedAtMillis ?: -1) + 1)

    private fun isKept(tombstone: WebDavBookmarkEntry, nowMillis: Long): Boolean =
        nowMillis - tombstone.modifiedAtMillis <= TOMBSTONE_RETENTION_MILLIS
}
