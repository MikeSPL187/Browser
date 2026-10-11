package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.data.FavoriteBookmarkMergeResult
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFavoritesTest {
    private class FakeStorage : ProfileFavorites.Storage {
        val libraries = mutableMapOf<String, FavoriteLibrary>()

        override fun load(profileId: String) = libraries[profileId] ?: FavoriteLibrary()

        override fun save(profileId: String, library: FavoriteLibrary) {
            libraries[profileId] = library
        }

        override fun mergeImported(profileId: String, imported: List<FavoriteEntry>): FavoriteBookmarkMergeResult {
            libraries[profileId] = FavoriteLibrary(imported + load(profileId).entries)
            return FavoriteBookmarkMergeResult(imported, imported.size, skippedCount = 0, limitReached = false)
        }

        override fun all() = libraries.values.toList()

        override fun delete(profileId: String) {
            libraries.remove(profileId)
        }
    }

    private val storage = FakeStorage()
    private var prunedUrls = emptySet<String>()
    private var prunedFolders = emptySet<String>()
    private val favorites = ProfileFavorites(
        storage = storage,
        runInBackground = { task -> task() },
        post = { task -> task() },
        isDestroyed = { false },
        pruneIcons = { urls, folderIds ->
            prunedUrls = urls
            prunedFolders = folderIds
        },
    )

    @Test
    fun eachProfileSeesAndSavesItsOwnFavorites() {
        storage.libraries["home"] = FavoriteLibrary(listOf(FavoriteEntry("https://home.example/", "Home", 1)))
        favorites.select("home")
        assertEquals(listOf("https://home.example/"), favorites.favorites.map(FavoriteEntry::url))

        favorites.select("work")
        assertTrue(favorites.favorites.isEmpty())
        assertNotNull(favorites.toggle("https://work.example/", "Work", now = 2))

        assertEquals(listOf("https://work.example/"), storage.load("work").favorites.map(FavoriteEntry::url))
        assertEquals(listOf("https://home.example/"), storage.load("home").favorites.map(FavoriteEntry::url))
        assertTrue(favorites.isFavorite("https://work.example/"))
        assertFalse(favorites.isFavorite("https://home.example/"))
    }

    @Test
    fun iconsStayWhileAnyProfileKeepsThem() {
        storage.libraries["home"] = FavoriteLibrary(
            listOf(FavoriteEntry("https://home.example/", "Home", 1), FavoriteFolder("trips", "Trips")),
        )
        favorites.select("work")
        favorites.toggle("https://work.example/", "Work", now = 2)

        assertEquals(setOf("https://home.example/", "https://work.example/"), prunedUrls)
        assertEquals(setOf("trips"), prunedFolders)

        favorites.forget("home")
        assertEquals(setOf("https://work.example/"), prunedUrls)
        assertTrue(prunedFolders.isEmpty())
    }

    @Test
    fun undoAppliesOnlyInTheProfileThatMadeTheChange() {
        favorites.select("work")
        val added = favorites.toggle("https://work.example/", "Work", now = 1)!!
        assertTrue(favorites.undo(added))
        assertTrue(storage.load("work").favorites.isEmpty())

        val again = favorites.toggle("https://work.example/", "Work", now = 2)!!
        favorites.select("home")
        favorites.select("work")
        assertFalse(favorites.undo(again))
        assertEquals(1, storage.load("work").favorites.size)
    }

    @Test
    fun importGoesToTheProfileThatStartedIt() {
        favorites.select("work")
        var result: FavoriteBookmarkMergeResult? = null
        favorites.import(listOf(FavoriteEntry("https://imported.example/", "Imported", 1))) { result = it }

        assertEquals(1, result?.importedCount)
        assertEquals(listOf("https://imported.example/"), favorites.favorites.map(FavoriteEntry::url))
        assertTrue(storage.load("home").favorites.isEmpty())
    }

    @Test
    fun aSyncedProfileUsesTheLocalListTheFavoritesScreenOpens() {
        // The controller maps a synced profile to the local one it keeps active (`persist()`).
        val mapped = ProfileFavorites(
            storage = storage,
            ownerOf = { id -> if (id == "synced-laptop") "home" else id },
            runInBackground = { task -> task() },
            post = { task -> task() },
            isDestroyed = { false },
            pruneIcons = { _, _ -> },
        )
        mapped.select("synced-laptop")
        assertEquals("home", mapped.profileId)
        assertNotNull(mapped.toggle("https://laptop.example/", "Laptop", now = 1))

        assertEquals(listOf("https://laptop.example/"), storage.load("home").favorites.map(FavoriteEntry::url))
        assertTrue(storage.libraries.keys.none { it == "synced-laptop" })
        // Switching between the synced profile and its local owner keeps the same list and its undo.
        mapped.select("home")
        assertTrue(mapped.isFavorite("https://laptop.example/"))
    }

    @Test
    fun nothingHappensBeforeAProfileIsSelected() {
        assertNull(favorites.toggle("https://work.example/", "Work", now = 1))
        var called = false
        favorites.import(emptyList()) { result ->
            called = true
            assertNull(result)
        }
        assertTrue(called)
        assertTrue(storage.libraries.isEmpty())
    }
}
