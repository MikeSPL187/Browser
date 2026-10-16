package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoritesLibraryV5RulesTest {
    private val trip = FavoriteFolder(id = "trip", title = "Поездка")
    private val maps = FavoriteFolder(id = "maps", title = "Карты", parentFolderId = "trip")
    private val polar = FavoriteEntry(url = "https://polar-notes.ru/", title = "Полярные заметки", addedAt = 1)
    private val train = FavoriteEntry(url = "https://rzd-tickets.ru/", title = "Поезд", addedAt = 2, parentFolderId = "trip")
    private val metro = FavoriteEntry(url = "https://metro.example/", title = "Схема метро", addedAt = 3, parentFolderId = "maps")
    private val library = FavoriteLibrary(listOf(trip, maps, polar, train, metro))

    @Test
    fun `the top shows its folders with their counts, then its favorites`() {
        val view = FavoritesLibraryV5Rules.view(library, folderId = null, query = "")
        assertEquals(listOf(trip to 2), view.folders)
        assertEquals(listOf(polar), view.favorites)
    }

    @Test
    fun `an open folder shows only what is directly in it`() {
        val view = FavoritesLibraryV5Rules.view(library, folderId = "trip", query = "")
        assertEquals(listOf(maps to 1), view.folders)
        assertEquals(listOf(train), view.favorites)
    }

    @Test
    fun `a search looks through every folder and shows no folders`() {
        val view = FavoritesLibraryV5Rules.view(library, folderId = null, query = " МЕТРО ")
        assertEquals(emptyList<Pair<FavoriteFolder, Int>>(), view.folders)
        assertEquals(listOf(metro), view.favorites)
    }

    @Test
    fun `up from a folder is its parent, and the top has none`() {
        assertEquals("trip", FavoritesLibraryV5Rules.parentOf(library, "maps"))
        assertNull(FavoritesLibraryV5Rules.parentOf(library, "trip"))
        assertNull(FavoritesLibraryV5Rules.parentOf(library, null))
    }
}
