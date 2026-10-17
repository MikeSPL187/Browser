package dev.sk2andy.materialbrowser.browser.transfer

import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.BrowsingLibraryRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalTransferLibraryRulesTest {
    private val library = FavoriteLibrary(
        listOf(
            FavoriteFolder(id = "work", title = "Work"),
            FavoriteFolder(id = "docs", title = "Docs/Specs", parentFolderId = "work"),
            FavoriteEntry(url = "https://example.org/", title = "Example", addedAt = 1),
            FavoriteEntry(url = "https://example.com/spec", title = "Spec", addedAt = 2, parentFolderId = "docs"),
        ),
    )

    @Test
    fun onlyOpenWebTabsTravel() {
        val tabs = listOf(
            BrowserTab(id = "a", lastAccessedAt = 0, title = "A", url = "https://example.com/a"),
            BrowserTab(id = "b", lastAccessedAt = 0, title = "Private", url = "https://example.com/b", isIncognito = true),
            BrowserTab(id = "c", lastAccessedAt = 0, url = "about:blank"),
            BrowserTab(id = "d", lastAccessedAt = 0, url = "file:///sdcard/x.html"),
        )

        assertEquals(listOf(LocalTransferTab("https://example.com/a", "A")), LocalTransferLibraryRules.tabs(tabs))
    }

    @Test
    fun bookmarksTravelWithTheirFolderPath() {
        val bookmarks = LocalTransferLibraryRules.bookmarks(library)

        assertEquals(
            setOf(
                LocalTransferBookmark("https://example.org/", "Example", ""),
                LocalTransferBookmark("https://example.com/spec", "Spec", "Work/Docs∕Specs"),
            ),
            bookmarks.toSet(),
        )
    }

    @Test
    fun receivedBookmarksLandInTheSameFoldersOnTheOtherPhone() {
        var ids = 0
        val sent = LocalTransferLibraryRules.bookmarks(library)

        val applied = LocalTransferLibraryRules.apply(FavoriteLibrary(), sent, nowMillis = 5) { "folder-${ids++}" }

        assertEquals(2, applied.added)
        assertEquals(0, applied.skipped)
        assertEquals(setOf("Work", "Docs/Specs"), applied.library.folders.map { it.title }.toSet())
        val docs = applied.library.folders.single { it.title == "Docs/Specs" }
        assertEquals(applied.library.folders.single { it.title == "Work" }.id, docs.parentFolderId)
        assertEquals(docs.id, applied.library.favorites.single { it.title == "Spec" }.parentFolderId)
        assertEquals(sent.toSet(), LocalTransferLibraryRules.bookmarks(applied.library).toSet())
    }

    @Test
    fun existingFoldersAreReusedByTitle() {
        var ids = 0
        val applied = LocalTransferLibraryRules.apply(
            library,
            listOf(LocalTransferBookmark("https://example.net/", "Net", "Work")),
            nowMillis = 5,
        ) { "new-${ids++}" }

        assertEquals(1, applied.added)
        assertEquals(0, ids)
        assertEquals("work", applied.library.favorites.single { it.title == "Net" }.parentFolderId)
    }

    @Test
    fun whatDoesNotFitIsCountedNotLost() {
        val full = FavoriteLibrary(
            List(BrowsingLibraryRules.MAX_FAVORITES - 1) { FavoriteEntry(url = "https://example.com/$it", title = "$it", addedAt = 1) },
        )
        val incoming = listOf(
            LocalTransferBookmark("https://example.org/one", "One"),
            LocalTransferBookmark("https://example.org/two", "Two"),
            LocalTransferBookmark("https://example.org/three", "Three"),
        )

        val applied = LocalTransferLibraryRules.apply(full, incoming, nowMillis = 5) { "f" }

        assertEquals(1, applied.added)
        assertEquals(2, applied.skipped)
        assertEquals(BrowsingLibraryRules.MAX_FAVORITES, applied.library.favorites.size)
    }

    @Test
    fun undoTakesBackOnlyWhatTheTransferAdded() {
        var ids = 0
        val incoming = listOf(
            LocalTransferBookmark("https://example.net/a", "A", "Work"),
            LocalTransferBookmark("https://example.net/b", "B", "Trip/Rome"),
        )
        val applied = LocalTransferLibraryRules.apply(library, incoming, nowMillis = 5) { "new-${ids++}" }
        assertEquals(2, applied.addedEntryIds.size)
        assertEquals(2, applied.createdFolderIds.size)

        assertEquals(
            BrowsingFavoritesRules.normalizeLibrary(library),
            LocalTransferLibraryRules.undo(applied.library, applied),
        )
    }

    @Test
    fun undoKeepsACreatedFolderThePersonFilledMeanwhile() {
        var ids = 0
        val applied = LocalTransferLibraryRules.apply(
            FavoriteLibrary(),
            listOf(LocalTransferBookmark("https://example.net/a", "A", "Trip")),
            nowMillis = 5,
        ) { "new-${ids++}" }
        val trip = applied.library.folders.single().id
        val edited = applied.library.copy(
            entries = applied.library.entries + FavoriteEntry(url = "https://example.org/mine", title = "Mine", addedAt = 6, parentFolderId = trip),
        )

        val undone = LocalTransferLibraryRules.undo(edited, applied)

        assertEquals(listOf("Mine"), undone.favorites.map { it.title })
        assertEquals(listOf(trip), undone.folders.map { it.id })
    }
}
