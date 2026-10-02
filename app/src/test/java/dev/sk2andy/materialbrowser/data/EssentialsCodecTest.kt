package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EssentialsCodecTest {
    @Test
    fun `round trip keeps every workspace and the order`() {
        val entries = mapOf(
            "candy" to listOf(
                EssentialEntry("https://a.example.com/", "A"),
                EssentialEntry("https://b.example.com/", "Б \"кавычки\""),
            ),
            "work" to emptyList(),
        )

        assertEquals(entries, EssentialsCodec.decode(EssentialsCodec.encode(entries)))
    }

    @Test
    fun `broken input and broken entries are skipped`() {
        assertEquals(emptyMap<String, List<EssentialEntry>>(), EssentialsCodec.decode("not json"))
        val decoded = EssentialsCodec.decode(
            """{"candy": [{"url": "https://a.example.com/"}, {"title": "no url"}, 3,
                {"url": "about:blank"}], "work": "oops"}""",
        )
        assertEquals(listOf(EssentialEntry("https://a.example.com/", "")), decoded["candy"])
        assertEquals(emptyList<EssentialEntry>(), decoded["work"])
    }
}
