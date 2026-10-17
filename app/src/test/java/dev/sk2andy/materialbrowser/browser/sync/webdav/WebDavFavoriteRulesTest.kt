package dev.sk2andy.materialbrowser.browser.sync.webdav

import dev.sk2andy.materialbrowser.browser.transfer.LocalTransferLibraryRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavBookmarkChanges
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavFavoriteRulesTest {
    private val library = FavoriteLibrary(
        listOf(
            FavoriteFolder(id = "work", title = "Work"),
            FavoriteEntry(url = "https://keep.example/", title = "Keep", addedAt = 1),
            FavoriteEntry(url = "https://gone.example/", title = "Gone", addedAt = 2, parentFolderId = "work"),
            FavoriteEntry(url = "https://moved.example/", title = "Moved", addedAt = 3),
        ),
    )
    private var folders = 0

    private fun apply(changes: WebDavBookmarkChanges) =
        WebDavFavoriteRules.apply(library, changes, nowMillis = 10) { "folder-${++folders}" }

    @Test
    fun removalsRenamesMovesAndAdditionsLand() {
        val result = apply(
            WebDavBookmarkChanges(
                add = listOf(LocalTransferBookmark("https://new.example/", "New", "Work/Docs")),
                remove = listOf("https://gone.example/"),
                update = listOf(LocalTransferBookmark("https://moved.example/", "Renamed", "Work")),
            ),
        )

        assertEquals(
            listOf(
                LocalTransferBookmark("https://keep.example/", "Keep", ""),
                LocalTransferBookmark("https://moved.example/", "Renamed", "Work"),
                LocalTransferBookmark("https://new.example/", "New", "Work/Docs"),
            ).sortedBy { it.url },
            LocalTransferLibraryRules.bookmarks(result).sortedBy { it.url },
        )
        assertTrue("an emptied folder stays", result.folders.any { it.id == "work" })
    }

    @Test
    fun nothingToChangeKeepsTheLibrary() {
        val result = apply(WebDavBookmarkChanges(emptyList(), emptyList(), emptyList()))

        assertEquals(LocalTransferLibraryRules.bookmarks(library), LocalTransferLibraryRules.bookmarks(result))
    }

    @Test
    fun aChangeForABookmarkThePhoneLacksIsIgnored() {
        val result = apply(
            WebDavBookmarkChanges(
                add = emptyList(),
                remove = listOf("https://unknown.example/"),
                update = listOf(LocalTransferBookmark("https://unknown.example/", "X")),
            ),
        )

        assertEquals(3, result.favorites.size)
    }
}
