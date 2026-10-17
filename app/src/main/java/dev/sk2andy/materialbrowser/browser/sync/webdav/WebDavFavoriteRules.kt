package dev.sk2andy.materialbrowser.browser.sync.webdav

import dev.sk2andy.materialbrowser.browser.transfer.LocalTransferLibraryRules
import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavBookmarkChanges

/**
 * What a WebDAV sync (V27) changes in a profile's favorites, without storage: removes the
 * bookmarks deleted elsewhere, renames and moves the ones changed elsewhere (creating folders by
 * title, as the local transfer does) and adds the missing ones up to the favorites limit.
 * Folders are never removed: an emptied folder stays, as the person made it.
 */
internal object WebDavFavoriteRules {
    fun apply(
        library: FavoriteLibrary,
        changes: WebDavBookmarkChanges,
        nowMillis: Long,
        newFolderId: () -> String,
    ): FavoriteLibrary {
        val removed = changes.remove.toSet()
        var current = BrowsingFavoritesRules.normalizeLibrary(library).let { normalized ->
            normalized.copy(entries = normalized.entries.filterNot { it is FavoriteEntry && it.url in removed })
        }
        for (update in changes.update) {
            val entry = current.favorites.firstOrNull { it.url == update.url } ?: continue
            val (withFolders, folderId) = LocalTransferLibraryRules.ensureFolder(current, update.folder, newFolderId)
            current = BrowsingFavoritesRules.move(
                withFolders.copy(
                    entries = withFolders.entries.map { if (it.id == entry.id) entry.copy(title = update.title) else it },
                ),
                entry.id,
                folderId,
            )
        }
        return LocalTransferLibraryRules.apply(current, changes.add, nowMillis, newFolderId).library
    }
}
