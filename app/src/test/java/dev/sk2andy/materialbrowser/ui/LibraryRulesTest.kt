package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.data.HistoryClearRequest
import dev.sk2andy.materialbrowser.data.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun favorite(name: String, parent: String?) = FavoriteEntry(
        url = "https://$name.example/",
        title = name,
        addedAt = 1,
        parentFolderId = parent,
    )
}
