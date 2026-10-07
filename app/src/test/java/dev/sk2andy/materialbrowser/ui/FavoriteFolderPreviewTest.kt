package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteFolderPreviewTest {
    @Test
    fun `folder preview follows nested sibling order and takes four favorites`() {
        val root = FavoriteFolder("root", "Root")
        val nested = FavoriteFolder("nested", "Nested", "root")
        val first = favorite("first", "root")
        val second = favorite("second", "nested")
        val third = favorite("third", "nested")
        val fourth = favorite("fourth", "root")
        val fifth = favorite("fifth", "root")
        val source = FavoriteLibrary(listOf(root, first, nested, fourth, second, third, fifth))
        assertEquals(listOf(first, second, third, fourth), favoriteFolderPreviewFavorites(source, root.id))
        assertEquals(emptyList<FavoriteEntry>(), favoriteFolderPreviewFavorites(source, "missing"))
    }

    @Test
    fun `the mosaic shows the favicons of the first four sites, or nothing`() {
        val root = FavoriteFolder("root", "Root")
        val first = favorite("first", "root")
        val second = favorite("second", "root")
        val third = favorite("third", "root")
        val fourth = favorite("fourth", "root")
        val fifth = favorite("fifth", "root")
        val source = FavoriteLibrary(listOf(root, first, second, third, fourth, fifth))
        val icons = mapOf(first.url to "1", third.url to "3", fifth.url to "5")

        // The fifth site is past the four the tile shows, even though the second has no icon.
        assertEquals(listOf("1", "3"), favoriteFolderMosaic(source, root.id, icons::get))
        assertEquals(emptyList<String>(), favoriteFolderMosaic(source, root.id) { null })
    }

    private fun favorite(name: String, parent: String) = FavoriteEntry(
        url = "https://$name.example/",
        title = name,
        addedAt = 1,
        parentFolderId = parent,
    )
}
