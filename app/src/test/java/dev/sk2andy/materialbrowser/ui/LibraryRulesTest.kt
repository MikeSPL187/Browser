package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.data.HistoryClearRequest
import dev.sk2andy.materialbrowser.data.HistoryEntry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRulesTest {
    @Test
    fun `initial takes the first whole character, upper-cased`() {
        assertEquals("Б", LibraryRules.initial("  байкал"))
        assertEquals("😀", LibraryRules.initial("😀 smile"))
        assertEquals("", LibraryRules.initial("   "))
    }

    @Test
    fun `the same site always gets the same tile`() {
        val first = LibraryRules.tileIndex("north-guide.ru", 8)
        assertEquals(first, LibraryRules.tileIndex(" North-Guide.ru ", 8))
        assertTrue(first in 0 until 8)
    }

    @Test
    fun `a card rounds only its outer rows`() {
        assertEquals(LibraryRowPosition.Single, LibraryRules.position(0, 1))
        assertEquals(LibraryRowPosition.First, LibraryRules.position(0, 3))
        assertEquals(LibraryRowPosition.Middle, LibraryRules.position(1, 3))
        assertEquals(LibraryRowPosition.Last, LibraryRules.position(2, 3))
        assertTrue(LibraryRowPosition.Single.roundsTop && LibraryRowPosition.Single.roundsBottom)
        assertFalse(LibraryRowPosition.Middle.roundsTop || LibraryRowPosition.Middle.roundsBottom)
    }

    @Test
    fun `history behind undo is hidden until the deletion commits`() {
        val work = HistoryEntry("https://a.example/", "A", lastVisitedAt = 100, profileId = "work")
        val late = HistoryEntry("https://b.example/", "B", lastVisitedAt = 200, profileId = "work")
        val personal = HistoryEntry("https://c.example/", "C", lastVisitedAt = 100, profileId = "personal")
        val all = listOf(work, late, personal)
        val key: (HistoryEntry) -> String = { "${it.profileId}:${it.url}" }

        assertEquals(all, LibraryRules.withoutPending(all, null, key))
        assertEquals(
            listOf(late, personal),
            LibraryRules.withoutPending(
                all,
                HistoryPendingDeletion.Entries(listOf(work), setOf(key(work))),
                key,
            ),
        )
        val clear = HistoryClearRequest(
            profileIds = setOf("work"),
            sinceInclusiveMillis = 100,
            untilExclusiveMillis = 200,
        )
        assertEquals(
            listOf(late, personal),
            LibraryRules.withoutPending(all, HistoryPendingDeletion.Clear(clear), key),
        )
    }

    @Test
    fun `a folder counts the sites in it and in its subfolders`() {
        val travel = FavoriteFolder("travel", "Travel")
        val winter = FavoriteFolder("winter", "Winter", parentFolderId = "travel")
        val library = FavoriteLibrary(
            listOf(
                travel,
                winter,
                favorite("routes", "travel"),
                favorite("ice", "winter"),
                favorite("loose", null),
            ),
        )
        assertEquals(2, LibraryRules.siteCount(library, "travel"))
        assertEquals(1, LibraryRules.siteCount(library, "winter"))
        assertEquals(0, LibraryRules.siteCount(library, "missing"))

        val level = LibraryRules.level(library.entries.filter { it.parentFolderId == null })
        assertEquals(listOf(travel), level.folders)
        assertEquals(listOf("https://loose.example/"), level.favorites.map(FavoriteEntry::url))
    }

    @Test
    fun `favorites sort by name or newest first, folders by name only`() {
        val ice = FavoriteFolder("ice", "лёд")
        val routes = FavoriteFolder("routes", "Маршруты")
        val level = FavoritesLevel(
            folders = listOf(routes, ice),
            favorites = listOf(
                favorite("yandex", null, title = "яндекс", addedAt = 3),
                favorite("baikal", null, title = "Байкал", addedAt = 1),
                favorite("ozon", null, title = "ёлки", addedAt = 2),
            ),
        )
        val ru = Locale.forLanguageTag("ru")

        assertEquals(level, LibraryRules.sorted(level, FavoritesSort.Manual, ru))
        val byName = LibraryRules.sorted(level, FavoritesSort.Name, ru)
        assertEquals(listOf(ice, routes), byName.folders)
        assertEquals(listOf("Байкал", "ёлки", "яндекс"), byName.favorites.map(FavoriteEntry::title))
        val recent = LibraryRules.sorted(level, FavoritesSort.Recent, ru)
        assertEquals(listOf(routes, ice), recent.folders)
        assertEquals(listOf("яндекс", "ёлки", "Байкал"), recent.favorites.map(FavoriteEntry::title))
    }

    @Test
    fun `move earlier and later step over the other kind to the visible neighbour`() {
        val a = favorite("a", null)
        val folder = FavoriteFolder("f", "Folder")
        val b = favorite("b", null)
        val second = FavoriteFolder("g", "Second")
        val source = FavoriteLibrary(listOf(a, folder, b, second))
        val siblings = BrowsingFavoritesRules.children(source, null)
        val level = LibraryRules.level(siblings)

        // B «up» lands before A, not merely before the folder between them.
        val earlier = LibraryRules.reorderTarget(siblings, level.favorites, b.id, -1)
        assertEquals(0, earlier)
        val moved = BrowsingFavoritesRules.reorder(source, b.id, earlier!!)
        assertEquals(listOf(b, a), LibraryRules.level(BrowsingFavoritesRules.children(moved, null)).favorites)
        // A «down» lands after B.
        val later = LibraryRules.reorderTarget(siblings, level.favorites, a.id, 1)
        assertEquals(2, later)
        val movedDown = BrowsingFavoritesRules.reorder(source, a.id, later!!)
        assertEquals(listOf(b, a), LibraryRules.level(BrowsingFavoritesRules.children(movedDown, null)).favorites)
        // The first site and the last folder sit at their group's edge, whatever lies around them.
        assertNull(LibraryRules.reorderTarget(siblings, level.favorites, a.id, -1))
        assertNull(LibraryRules.reorderTarget(siblings, level.favorites, b.id, 1))
        assertNull(LibraryRules.reorderTarget(siblings, level.folders, folder.id, -1))
        assertNull(LibraryRules.reorderTarget(siblings, level.folders, second.id, 1))
        val folderLater = LibraryRules.reorderTarget(siblings, level.folders, folder.id, 1)
        assertEquals(3, folderLater)
        val foldersMoved = BrowsingFavoritesRules.reorder(source, folder.id, folderLater!!)
        assertEquals(listOf(second, folder), LibraryRules.level(BrowsingFavoritesRules.children(foldersMoved, null)).folders)
        assertNull(LibraryRules.reorderTarget(siblings, level.favorites, "missing", 1))
    }

    private fun favorite(name: String, parent: String?, title: String = name, addedAt: Long = 1) = FavoriteEntry(
        url = "https://$name.example/",
        title = title,
        addedAt = addedAt,
        parentFolderId = parent,
    )
}
