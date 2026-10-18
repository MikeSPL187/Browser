package dev.sk2andy.materialbrowser.reader

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.StandardCharsets

class ReaderLibraryStoreCodecTest {
    @Test
    fun `a library round trips`() {
        val state = library()

        assertEquals(state, ReaderLibraryStore.decodeLibrary(ReaderLibraryStore.encodeLibrary(state)))
    }

    @Test
    fun `a file that is not JSON is unreadable, not an empty library`() {
        assertNull(ReaderLibraryStore.decodeLibrary("{\"version\":1,\"snap".bytes()))
    }

    @Test
    fun `a file of another version is unreadable, not an empty library`() {
        val stored = JSONObject(ReaderLibraryStore.encodeLibrary(library()).text())

        assertNull(ReaderLibraryStore.decodeLibrary(stored.put("version", 2).toString().bytes()))
        stored.remove("version")
        assertNull(ReaderLibraryStore.decodeLibrary(stored.toString().bytes()))
    }

    @Test
    fun `a file over the size limit is unreadable`() {
        assertNull(ReaderLibraryStore.decodeLibrary(ByteArray(8 * 1024 * 1024 + 1) { ' '.code.toByte() }))
    }

    @Test
    fun `a saved article that cannot be read leaves only itself out`() {
        val state = library()
        val stored = JSONObject(ReaderLibraryStore.encodeLibrary(state).text())
        stored.getJSONArray("snapshots").getJSONObject(0).put("id", "not-a-uuid")

        val restored = ReaderLibraryStore.decodeLibrary(stored.toString().bytes())

        assertEquals(state.copy(snapshots = state.snapshots.drop(1)), restored)
    }

    private fun library() = ReaderLibraryState(
        settings = ReaderSettings(1.3f, ReaderTheme.Paper),
        progressByUrl = mapOf("https://example.com/read" to 0.4f),
        snapshots = listOf(
            snapshot("9b2f6c1e-0d4a-4f7e-9a51-1c3b5d7e9f01", "https://example.com/one"),
            snapshot("4e8a2b6c-7d1f-4a3e-8b5c-2d4f6a8c0e12", "https://example.com/two"),
        ),
    )

    private fun snapshot(id: String, url: String) = ReaderSnapshot(
        id = id,
        document = ReaderDocument(
            title = "Article $id",
            sourceUrl = url,
            siteName = "Example",
            blocks = listOf(
                ReaderBlock(
                    ReaderBlockKind.Paragraph,
                    "Readable article body with enough content for persistence tests, safe restoration and more.",
                ),
            ),
        ),
        progress = 0.5f,
        savedAtMillis = 10L,
    )

    private fun String.bytes(): ByteArray = toByteArray(StandardCharsets.UTF_8)

    private fun ByteArray.text(): String = toString(StandardCharsets.UTF_8)
}
