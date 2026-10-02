package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EssentialsRulesTest {
    @Test
    fun `normalize drops non web pages, repeats and anything past the limit`() {
        val entries = listOf(
            entry("https://mail.example.com/"),
            entry("about:config"),
            entry("https://MAIL.example.com"),
            entry("javascript:alert(1)"),
        ) + (1..20).map { entry("https://site$it.example.com/") }

        val normalized = EssentialsRules.normalize(entries)

        assertEquals(EssentialsRules.MAX_ENTRIES, normalized.size)
        assertEquals("https://mail.example.com/", normalized.first().url)
        assertEquals(normalized.map { it.id }.distinct(), normalized.map { it.id })
    }

    @Test
    fun `add appends, refuses duplicates, a full grid and non web pages`() {
        val one = listOf(entry("https://a.example.com/"))

        val added = EssentialsRules.add(one, " https://b.example.com/ ", " B ")
        assertEquals(
            listOf("https://a.example.com/", "https://b.example.com/"),
            (added as EssentialsRules.AddResult.Added).entries.map { it.url },
        )
        assertEquals("B", added.entries.last().title)
        assertEquals(
            EssentialsRules.AddResult.Duplicate,
            EssentialsRules.add(one, "https://A.example.com", ""),
        )
        assertEquals(
            EssentialsRules.AddResult.Unsupported,
            EssentialsRules.add(one, "file:///sdcard/a.html", ""),
        )
        val full = (1..EssentialsRules.MAX_ENTRIES).map { entry("https://s$it.example.com/") }
        assertEquals(EssentialsRules.AddResult.Full, EssentialsRules.add(full, "https://new.example.com/", ""))
    }

    @Test
    fun `restore puts an entry back at its index unless it came back already`() {
        val a = entry("https://a.example.com/")
        val b = entry("https://b.example.com/")
        val c = entry("https://c.example.com/")

        assertEquals(listOf(a, b, c), EssentialsRules.restore(listOf(a, c), b, 1))
        assertEquals(listOf(a, c, b), EssentialsRules.restore(listOf(a, c), b, 9))
        assertNull(EssentialsRules.restore(listOf(a, b), b, 0))
    }

    @Test
    fun `move reorders within bounds and ignores unknown ids`() {
        val a = entry("https://a.example.com/")
        val b = entry("https://b.example.com/")
        val c = entry("https://c.example.com/")
        val list = listOf(a, b, c)

        assertEquals(listOf(b, c, a), EssentialsRules.move(list, a.id, 2))
        assertEquals(listOf(c, a, b), EssentialsRules.move(list, c.id, -4))
        assertEquals(list, EssentialsRules.move(list, "missing", 0))
        assertEquals(list, EssentialsRules.move(list, b.id, 1))
    }

    @Test
    fun `migration copies the top level favorites in order, without folders, at most twelve`() {
        val library = FavoriteLibrary(
            listOf(
                FavoriteFolder(id = "work", title = "Work"),
                FavoriteEntry(url = "https://inside.example.com/", title = "Inside", addedAt = 0, parentFolderId = "work"),
            ) + (1..14).map { index ->
                FavoriteEntry(url = "https://top$index.example.com/", title = "Top $index", addedAt = 0)
            },
        )

        val seed = EssentialsRules.migrationSeed(library)

        assertEquals(EssentialsRules.MIGRATED_ENTRY_LIMIT, seed.size)
        assertEquals("https://top1.example.com/", seed.first().url)
        assertEquals("Top 1", seed.first().title)
        assertTrue(seed.none { it.url.contains("inside") })
    }

    @Test
    fun `template is Personal while it exists, then the first workspace with a list`() {
        val stored = mapOf("candy" to listOf(entry("https://a.example.com/")), "work" to emptyList())

        assertEquals("candy", EssentialsRules.templateProfileId(stored, listOf("work", "candy"), "candy"))
        assertEquals("work", EssentialsRules.templateProfileId(stored - "candy", listOf("new", "work"), "candy"))
        assertNull(EssentialsRules.templateProfileId(emptyMap(), listOf("new"), "candy"))
    }

    @Test
    fun `candidates skip pinned pages, repeats and pages that are not on the web`() {
        val pinned = listOf(entry("https://a.example.com/"))
        val tabs = listOf(
            EssentialCandidate("1", "https://a.example.com", "A"),
            EssentialCandidate("2", "https://b.example.com/", "B"),
            EssentialCandidate("3", "https://B.example.com/", "B again"),
            EssentialCandidate("4", "about:blank", ""),
        )

        assertEquals(listOf("2"), EssentialsRules.candidates(pinned, tabs).map { it.tabId })
    }

    @Test
    fun `label falls back to the site name and the monogram to its first letter`() {
        assertEquals("example.com", EssentialsRules.label(entry("https://www.example.com/path")))
        assertEquals("Почта", EssentialsRules.label(EssentialEntry("https://mail.example.com/", "Почта")))
        assertEquals("П", EssentialsRules.monogram(EssentialEntry("https://mail.example.com/", "почта")))
        assertEquals("E", EssentialsRules.monogram(EssentialEntry("https://example.com/", "  ★ ")))
    }

    private fun entry(url: String) = EssentialEntry(url = url, title = "")

    @Test
    fun `a tap switches to the first open tab on the same page, else opens a new one`() {
        val entry = EssentialEntry("https://www.example.com/", "Example")
        val tabs = listOf(
            EssentialCandidate("a", "https://other.org/", "Other"),
            EssentialCandidate("b", "https://www.example.com", "Example"),
            EssentialCandidate("c", "https://www.example.com/", "Example again"),
            EssentialCandidate("d", "https://example.com/", "Another host"),
        )

        assertEquals("b", EssentialsRules.openTabId(entry, tabs))
        assertNull(EssentialsRules.openTabId(entry, tabs.take(1)))
        assertNull(EssentialsRules.openTabId(entry, emptyList()))
    }
}
