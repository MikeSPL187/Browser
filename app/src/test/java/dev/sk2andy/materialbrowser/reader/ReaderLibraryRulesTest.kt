package dev.sk2andy.materialbrowser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLibraryRulesTest {
    @Test
    fun `private mode cannot cross any persistence boundary`() {
        val state = ReaderLibraryState(
            settings = ReaderSettings(1.2f, ReaderTheme.Paper),
            progressByUrl = mapOf("https://example.com" to 0.4f),
            snapshots = listOf(snapshot("one", "https://example.com", 1L)),
        )

        assertSame(
            state,
            ReaderLibraryRules.updateSettings(
                state,
                ReaderSettings(1.6f, ReaderTheme.Dark),
                isPrivate = true,
            ),
        )
        assertSame(
            state,
            ReaderLibraryRules.updateProgress(state, "https://other.example", 0.8f, true),
        )
        assertSame(
            state,
            ReaderLibraryRules.saveSnapshot(
                state,
                snapshot("two", "https://other.example", 2L),
                true,
            ),
        )
        assertSame(state, ReaderLibraryRules.deleteSnapshot(state, "one", true))
        assertEquals(ReaderLibraryState(), ReaderLibraryRules.visibleState(state, true))
    }

    @Test
    fun `reading themes follow the browser and keep earlier saved names`() {
        assertEquals(ReaderTheme.Light, ReaderTheme.System.resolved(browserDark = false))
        assertEquals(ReaderTheme.Dark, ReaderTheme.System.resolved(browserDark = true))
        assertEquals(ReaderTheme.Paper, ReaderTheme.Paper.resolved(browserDark = true))
        assertEquals(ReaderTheme.Light, ReaderTheme.Light.resolved(browserDark = true))

        assertEquals(ReaderTheme.Dark, ReaderTheme.fromStoredName("Night"))
        assertEquals(ReaderTheme.Paper, ReaderTheme.fromStoredName("Paper"))
        assertEquals(ReaderTheme.System, ReaderTheme.fromStoredName("Sepia"))
        assertEquals(ReaderTheme.System, ReaderTheme.fromStoredName(null))
    }

    @Test
    fun `reading time rounds up at two hundred words a minute`() {
        assertEquals(1, ReaderLibraryRules.readingMinutes(""))
        assertEquals(1, ReaderLibraryRules.readingMinutes("word ".repeat(200)))
        assertEquals(2, ReaderLibraryRules.readingMinutes("word ".repeat(201)))
        assertEquals(12, ReaderLibraryRules.readingMinutes(" a\n\tb ".repeat(1_150)))
    }

    @Test
    fun `text size stays on the slider range`() {
        val state = ReaderLibraryRules.updateSettings(
            ReaderLibraryState(),
            ReaderSettings(fontScale = 3f),
            isPrivate = false,
        )
        assertEquals(ReaderLibraryRules.MAX_FONT_SCALE, state.settings.fontScale)
    }

    @Test
    fun `progress is bounded and resumes by source url`() {
        var state = ReaderLibraryState()
        state = ReaderLibraryRules.updateProgress(
            state,
            "https://example.com/article",
            1.4f,
            isPrivate = false,
        )

        assertEquals(1f, state.progressByUrl["https://example.com/article"])
        assertEquals(0.5f, ReaderLibraryRules.progress(50, 100))
        assertEquals(0f, ReaderLibraryRules.progress(50, 0))
    }

    @Test
    fun `new snapshot replaces same source and library remains bounded`() {
        var state = ReaderLibraryState()
        repeat(ReaderLibraryRules.MAX_SNAPSHOTS + 4) { index ->
            state = ReaderLibraryRules.saveSnapshot(
                state,
                snapshot("id-$index", "https://example.com/$index", index.toLong()),
                isPrivate = false,
            )
        }
        state = ReaderLibraryRules.saveSnapshot(
            state,
            snapshot("replacement", "https://example.com/23", 100L),
            isPrivate = false,
        )

        assertEquals(ReaderLibraryRules.MAX_SNAPSHOTS, state.snapshots.size)
        assertEquals("replacement", state.snapshots.first().id)
        assertEquals(1, state.snapshots.count { it.document.sourceUrl.endsWith("/23") })
        assertTrue(state.snapshots.zipWithNext().all { (a, b) -> a.savedAtMillis >= b.savedAtMillis })
    }

    @Test
    fun `newer url progress wins over snapshot save progress`() {
        val snapshot = snapshot("saved", "https://example.com/article", 1L)
        val state = ReaderLibraryState(
            progressByUrl = mapOf(snapshot.document.sourceUrl to 0.8f),
            snapshots = listOf(snapshot),
        )

        assertEquals(
            0.8f,
            ReaderLibraryRules.resumeProgress(
                state,
                snapshot.progress,
                snapshot.document.sourceUrl,
            ),
        )
        assertEquals(
            snapshot.progress,
            ReaderLibraryRules.resumeProgress(
                ReaderLibraryState(),
                snapshot.progress,
                snapshot.document.sourceUrl,
            ),
        )
    }

    @Test
    fun `justification applies only to flowing article text`() {
        assertTrue(
            ReaderLibraryRules.shouldJustify(
                ReaderBlockKind.Paragraph,
                ReaderTextAlignment.Justified,
            ),
        )
        assertTrue(
            ReaderLibraryRules.shouldJustify(
                ReaderBlockKind.ListItem,
                ReaderTextAlignment.Justified,
            ),
        )
        assertTrue(
            ReaderBlockKind.entries
                .filterNot { it == ReaderBlockKind.Paragraph || it == ReaderBlockKind.ListItem }
                .none { ReaderLibraryRules.shouldJustify(it, ReaderTextAlignment.Justified) },
        )
        assertTrue(
            ReaderBlockKind.entries.none {
                ReaderLibraryRules.shouldJustify(it, ReaderTextAlignment.Start)
            },
        )
    }

    @Test
    fun `storage budget evicts oldest entries until encoded bytes fit`() {
        val kept = ReaderStorageBudget.evictOldestUntil(
            newestFirst = listOf("new", "middle", "old"),
            maxBytes = 10,
            encodedBytes = { entries -> entries.sumOf(String::length) },
        )

        assertEquals(listOf("new", "middle"), kept)
    }

    private fun snapshot(id: String, url: String, savedAt: Long) = ReaderSnapshot(
        id = id,
        document = ReaderDocument(
            title = "Article $id",
            sourceUrl = url,
            siteName = "Example",
            blocks = listOf(
                ReaderBlock(
                    ReaderBlockKind.Paragraph,
                    "Readable article body with enough content for persistence behavior tests and safe restoration.",
                ),
            ),
        ),
        progress = 0.5f,
        savedAtMillis = savedAt,
    )
}
