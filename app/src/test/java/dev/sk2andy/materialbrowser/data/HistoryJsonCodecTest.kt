package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_ID
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryJsonCodecTest {
    @Test
    fun `a damaged row is skipped and the valid rows stay`() {
        val raw = """
            [
              {"url": "https://a.example/", "title": "A", "lastVisitedAt": 20, "profileId": "work", "visitId": "a"},
              {"title": "No URL", "lastVisitedAt": 15},
              42,
              {"url": "https://b.example/", "title": "B", "lastVisitedAt": 10, "profileId": "work", "visitId": "b"}
            ]
        """.trimIndent()

        val history = HistoryJsonCodec.decode(raw)

        assertEquals(
            listOf(
                HistoryEntry("https://a.example/", "A", 20, profileId = "work", visitId = "a"),
                HistoryEntry("https://b.example/", "B", 10, profileId = "work", visitId = "b"),
            ),
            history,
        )
    }

    @Test
    fun `legacy rows without optional fields keep their defaults`() {
        val history = HistoryJsonCodec.decode("""[{"url": "https://a.example/", "profileId": " "}]""")

        assertEquals(
            listOf(HistoryEntry("https://a.example/", "", 0, profileId = DEFAULT_PROFILE_ID)),
            history,
        )
    }

    @Test
    fun `missing or unreadable history is empty`() {
        assertEquals(emptyList<HistoryEntry>(), HistoryJsonCodec.decode(null))
        assertEquals(emptyList<HistoryEntry>(), HistoryJsonCodec.decode("{not json"))
        assertEquals(emptyList<HistoryEntry>(), HistoryJsonCodec.decode("""{"url": "https://a.example/"}"""))
    }
}
