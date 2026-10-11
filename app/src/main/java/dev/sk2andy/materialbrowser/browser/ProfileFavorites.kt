package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.BrowsingLibraryRules
import dev.sk2andy.materialbrowser.data.CanonicalWebUrl
import dev.sk2andy.materialbrowser.data.FavoriteBookmarkMergeResult
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.data.FavoriteMutation
import dev.sk2andy.materialbrowser.data.FavoriteUndoRules

/**
 * The favorites of the active profile (v5 V11: every profile keeps its own, as in Safari). Selecting
 * a profile shows its list, a change is saved under that profile, and the icons of favorites and
 * folders that no profile keeps any more are dropped. An undo offered before a profile switch no
 * longer applies after it. [ownerOf] says whose favorites a profile shows: a synced profile from
 * another device keeps none here and shows those of a local profile.
 */
internal class ProfileFavorites(
    private val storage: Storage,
    private val ownerOf: (profileId: String) -> String = { it },
    private val runInBackground: (() -> Unit) -> Unit,
    private val post: (() -> Unit) -> Unit,
    private val isDestroyed: () -> Boolean,
    private val pruneIcons: (favoriteUrls: Set<String>, folderIds: Set<String>) -> Unit,
) {
    interface Storage {
        fun load(profileId: String): FavoriteLibrary

        fun save(profileId: String, library: FavoriteLibrary)

        fun mergeImported(profileId: String, imported: List<FavoriteEntry>): FavoriteBookmarkMergeResult?

        /** Every profile's favorites, the active one included. */
        fun all(): List<FavoriteLibrary>

        fun delete(profileId: String)
    }

    val favorites = mutableStateListOf<FavoriteEntry>()
    var library by mutableStateOf(FavoriteLibrary())
        private set
    var profileId: String? = null
        private set
    var importInFlight = false
        private set
    private var revision = 0L
    private var requested: String? = null
    private val undoSnapshots = mutableMapOf<Long, FavoriteLibrary>()

    fun isFavorite(url: String): Boolean = BrowsingLibraryRules.isFavorite(favorites, url)

    fun select(profileId: String) {
        requested = profileId
        val owner = ownerOf(profileId)
        if (owner == this.profileId) return
        this.profileId = owner
        reload()
    }

    fun reload() {
        val id = profileId ?: return
        revision++
        undoSnapshots.clear()
        apply(storage.load(id))
        pruneUnusedIcons()
    }

    fun import(imported: List<FavoriteEntry>, onComplete: (FavoriteBookmarkMergeResult?) -> Unit) {
        val id = profileId
        if (importInFlight || id == null) {
            onComplete(null)
            return
        }
        importInFlight = true
        runInBackground {
            val result = storage.mergeImported(id, imported)
            post {
                importInFlight = false
                if (isDestroyed()) return@post
                if (result != null && result.importedCount > 0 && id == profileId) reload()
                onComplete(result)
            }
        }
    }

    /** Adds [url] to the favorites or removes it; null when nothing changed. */
    fun toggle(url: String, title: String, now: Long): FavoriteMutation? {
        val id = profileId ?: return null
        if (importInFlight) return null
        val before = favorites.toList()
        val libraryBefore = library
        val wasFavorite = BrowsingLibraryRules.isFavorite(favorites, url)
        val updated = BrowsingLibraryRules.toggleFavorite(
            current = favorites,
            entry = FavoriteEntry(url = url, title = title, addedAt = now),
        )
        if (updated == before) return null
        val updatedLibrary = if (wasFavorite) {
            FavoriteLibrary(
                library.entries.filterNot { entry ->
                    entry is FavoriteEntry && CanonicalWebUrl.key(entry.url) == CanonicalWebUrl.key(url)
                },
            )
        } else {
            FavoriteLibrary(entries = listOf(updated.first()) + library.entries)
        }
        apply(updatedLibrary)
        storage.save(id, library)
        pruneUnusedIcons()
        return FavoriteMutation(
            before = before,
            applied = updated,
            added = !wasFavorite,
            revision = ++revision,
        ).also { mutation -> undoSnapshots[mutation.revision] = libraryBefore }
    }

    /** Reverts [mutation] when it is still the latest change of this profile's favorites. */
    fun undo(mutation: FavoriteMutation): Boolean {
        val id = profileId ?: return false
        if (importInFlight) return false
        FavoriteUndoRules.restore(
            current = favorites.toList(),
            currentRevision = revision,
            mutation = mutation,
        ) ?: return false
        val changed = undoSnapshots.remove(mutation.revision) ?: if (mutation.added) {
            mutation.applied.firstOrNull { applied -> mutation.before.none { it.id == applied.id } }
                ?.let { added -> FavoriteLibrary(library.entries.filterNot { it.id == added.id }) }
        } else {
            mutation.before.firstOrNull { before -> mutation.applied.none { it.id == before.id } }
                ?.let { removed -> FavoriteLibrary(listOf(removed) + library.entries) }
        } ?: return false
        revision++
        apply(changed)
        storage.save(id, library)
        pruneUnusedIcons()
        return true
    }

    /**
     * Drops a deleted profile's favorites and the icons only they used. When the shown list was
     * that profile's (a synced profile showing the first local one), the profile is chosen again
     * once the deletion is through, so it shows the list of the profile that now owns it.
     */
    fun forget(profileId: String) {
        runInBackground { storage.delete(profileId) }
        if (profileId == this.profileId) {
            this.profileId = null
            post { if (!isDestroyed()) requested?.let(::select) }
        }
        pruneUnusedIcons()
    }

    private fun apply(library: FavoriteLibrary) {
        this.library = BrowsingFavoritesRules.normalizeLibrary(library)
        favorites.clear()
        favorites += this.library.favorites
    }

    // Reading every profile's favorites stays off the main thread.
    private fun pruneUnusedIcons() {
        val current = library
        runInBackground {
            val libraries = storage.all() + current
            pruneIcons(
                libraries.flatMapTo(hashSetOf()) { kept -> kept.favorites.map(FavoriteEntry::url) },
                libraries.flatMapTo(hashSetOf()) { kept -> kept.folders.map { folder -> folder.id } },
            )
        }
    }
}

/** [ProfileFavorites.Storage] in the browser's preferences. */
internal class ProfileFavoritesStore(private val store: BrowserSessionStore) : ProfileFavorites.Storage {
    override fun load(profileId: String): FavoriteLibrary = store.loadFavoriteLibrary(profileId)

    override fun save(profileId: String, library: FavoriteLibrary) = store.saveFavoriteLibrary(library, profileId)

    override fun mergeImported(profileId: String, imported: List<FavoriteEntry>): FavoriteBookmarkMergeResult? =
        store.mergeImportedFavoritesCommitted(imported, profileId)

    override fun all(): List<FavoriteLibrary> = store.loadAllFavoriteLibraries()

    override fun delete(profileId: String) = store.deleteFavorites(profileId)
}
