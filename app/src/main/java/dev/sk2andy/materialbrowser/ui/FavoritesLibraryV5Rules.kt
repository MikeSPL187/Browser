package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import java.util.Locale

/** What the favorites segment of the v5 library shows (V28, B2c). */
internal data class V5FavoritesView(
    /** Folders in the open folder, each with how many things it holds; none while searching. */
    val folders: List<Pair<FavoriteFolder, Int>>,
    val favorites: List<FavoriteEntry>,
)

internal object FavoritesLibraryV5Rules {
    /**
     * The open folder ([folderId], null at the top) in the library's own order, folders first; a
     * search instead looks through every favorite, in every folder, by title and address.
     */
    fun view(library: FavoriteLibrary, folderId: String?, query: String): V5FavoritesView {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isNotEmpty()) {
            return V5FavoritesView(
                folders = emptyList(),
                favorites = library.favorites.filter { favorite ->
                    favorite.title.lowercase(Locale.ROOT).contains(needle) ||
                        favorite.url.lowercase(Locale.ROOT).contains(needle)
                },
            )
        }
        val children = library.entries.groupingBy { entry -> entry.parentFolderId }.eachCount()
        return V5FavoritesView(
            folders = library.folders
                .filter { folder -> folder.parentFolderId == folderId }
                .map { folder -> folder to (children[folder.id] ?: 0) },
            favorites = library.favorites.filter { favorite -> favorite.parentFolderId == folderId },
        )
    }

    /** The folder one level up from [folderId]; null is the top. */
    fun parentOf(library: FavoriteLibrary, folderId: String?): String? =
        library.folders.firstOrNull { folder -> folder.id == folderId }?.parentFolderId
}
