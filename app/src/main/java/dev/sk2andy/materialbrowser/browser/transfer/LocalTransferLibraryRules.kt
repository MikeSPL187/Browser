package dev.sk2andy.materialbrowser.browser.transfer

import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.BrowsingLibraryRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayloadCodec
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab

/**
 * What a local transfer (V26) takes from this phone's library and what it adds to it, without
 * storage or screens. Folder paths travel as titles joined by [PATH_SEPARATOR]; a `/` inside a
 * title is sent as [SLASH_IN_TITLE] so it does not split the folder on the other phone.
 */
internal object LocalTransferLibraryRules {
    const val PATH_SEPARATOR = "/"
    const val SLASH_IN_TITLE = "∕"

    /** The open web tabs of [tabs] that may travel: no private tabs, no blank or local pages. */
    fun tabs(tabs: List<BrowserTab>): List<LocalTransferTab> = tabs
        .filter { !it.isIncognito && LocalTransferPayloadCodec.isTransferable(it.url) }
        .map { LocalTransferTab(it.url, it.title) }

    /** Every bookmark of [library] with its folder path, in the library's order. */
    fun bookmarks(library: FavoriteLibrary): List<LocalTransferBookmark> {
        val normalized = BrowsingFavoritesRules.normalizeLibrary(library)
        val folders = normalized.folders.associateBy { it.id }
        return normalized.favorites
            .filter { LocalTransferPayloadCodec.isTransferable(it.url) }
            .map { LocalTransferBookmark(it.url, it.title, folderPath(it.parentFolderId, folders)) }
    }

    /**
     * [library] with [bookmarks] added, how many of them did not fit, and what was added
     * ([addedEntryIds], [createdFolderIds]) so [undo] can take exactly that back.
     */
    class Applied(
        val library: FavoriteLibrary,
        val added: Int,
        val skipped: Int,
        val addedEntryIds: Set<String> = emptySet(),
        val createdFolderIds: Set<String> = emptySet(),
    )

    /**
     * Adds [bookmarks] (already free of duplicates, see `LocalTransferMergeRules.plan`) into
     * their folders, creating missing folders by title under the same parent. The library keeps
     * at most [BrowsingLibraryRules.MAX_FAVORITES]; what does not fit, or what the library refuses,
     * is counted in [Applied.skipped] so the screen can say so.
     */
    fun apply(
        library: FavoriteLibrary,
        bookmarks: List<LocalTransferBookmark>,
        nowMillis: Long,
        newFolderId: () -> String,
    ): Applied {
        val before = BrowsingFavoritesRules.normalizeLibrary(library)
        var current = before
        for (bookmark in bookmarks) {
            if (current.favorites.size >= BrowsingLibraryRules.MAX_FAVORITES) break
            val (withFolders, folderId) = ensureFolder(current, bookmark.folder, newFolderId)
            val entry = FavoriteEntry(url = bookmark.url, title = bookmark.title, addedAt = nowMillis, parentFolderId = folderId)
            current = BrowsingFavoritesRules.normalizeLibrary(withFolders.copy(entries = withFolders.entries + entry))
        }
        val added = current.favorites.size - before.favorites.size
        return Applied(
            library = current,
            added = added,
            skipped = bookmarks.size - added,
            addedEntryIds = current.favorites.map { it.id }.toSet() - before.favorites.map { it.id }.toSet(),
            createdFolderIds = current.folders.map { it.id }.toSet() - before.folders.map { it.id }.toSet(),
        )
    }

    /**
     * Takes a transfer back (the done screen's «Undo»): removes the bookmarks [applied] added and
     * the folders it created, from [library] as it is now. A created folder that now holds
     * something else — the person moved a bookmark into it — stays, with that bookmark.
     */
    fun undo(library: FavoriteLibrary, applied: Applied): FavoriteLibrary {
        var entries = BrowsingFavoritesRules.normalizeLibrary(library).entries
            .filterNot { it is FavoriteEntry && it.id in applied.addedEntryIds }
        while (true) {
            val emptyCreated = entries.filter { entry ->
                entry is FavoriteFolder && entry.id in applied.createdFolderIds &&
                    entries.none { it.parentFolderId == entry.id }
            }.map { it.id }.toSet()
            if (emptyCreated.isEmpty()) break
            entries = entries.filterNot { it.id in emptyCreated }
        }
        return BrowsingFavoritesRules.normalizeLibrary(FavoriteLibrary(entries))
    }

    private fun folderPath(folderId: String?, folders: Map<String, FavoriteFolder>): String {
        val titles = ArrayDeque<String>()
        var id = folderId
        while (id != null && titles.size < folders.size) {
            val folder = folders[id] ?: break
            titles.addFirst(folder.title.trim().replace(PATH_SEPARATOR, SLASH_IN_TITLE))
            id = folder.parentFolderId
        }
        return titles.joinToString(PATH_SEPARATOR).take(LocalTransferPayloadCodec.MAX_FOLDER_CHARS)
    }

    /** The folder at [path] (titles from the top), created where missing; null for the top level. */
    private fun ensureFolder(library: FavoriteLibrary, path: String, newFolderId: () -> String): Pair<FavoriteLibrary, String?> {
        var current = library
        var parent: String? = null
        val titles = path.split(PATH_SEPARATOR)
            .map { it.trim().replace(SLASH_IN_TITLE, PATH_SEPARATOR).take(BrowsingFavoritesRules.MAX_FOLDER_TITLE_CHARS) }
            .filter(String::isNotEmpty)
        for (title in titles) {
            val existing = current.folders.firstOrNull { it.parentFolderId == parent && it.title.trim() == title }
            parent = existing?.id ?: newFolderId().also { id ->
                current = BrowsingFavoritesRules.addFolder(current, FavoriteFolder(id = id, title = title, parentFolderId = parent))
                if (current.folders.none { it.id == id }) return current to parent
            }
        }
        return current to parent
    }
}
