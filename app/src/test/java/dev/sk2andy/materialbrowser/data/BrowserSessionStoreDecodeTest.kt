package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserSessionStoreDecodeTest {
    @Test
    fun `a tab entry that cannot be read hides only itself`() {
        val raw = """
            [
              {"id": "first", "url": "https://example.com/", "lastAccessedAt": 5},
              {"url": "https://no-id.example/"},
              "broken",
              null,
              {"id": "", "url": "https://blank-id.example/"},
              {"id": "second", "url": "https://example.org/"}
            ]
        """.trimIndent()

        val tabs = BrowserSessionStore.decodeTabs(raw, nowMillis = 42L)

        assertEquals(listOf("first", "second"), tabs.map(BrowserTab::id))
        assertEquals("https://example.com/", tabs.first().url)
        assertEquals(42L, tabs.last().lastAccessedAt)
        assertEquals(DEFAULT_PROFILE_ID, tabs.last().profileId)
    }

    @Test
    fun `an unreadable tab list decodes as empty`() {
        assertTrue(BrowserSessionStore.decodeTabs("[{not json", nowMillis = 1L).isEmpty())
    }

    @Test
    fun `a group entry that cannot be read hides only itself`() {
        val tabs = listOf(tab("a"), tab("b"), tab("c"))
        val raw = """
            [
              {"id": "kept", "profileId": "$DEFAULT_PROFILE_ID", "name": "Work", "tabIds": ["a", "b"]},
              {"profileId": "$DEFAULT_PROFILE_ID", "name": "No id", "tabIds": ["c"]},
              {"id": "no-name", "profileId": "$DEFAULT_PROFILE_ID", "tabIds": ["c"]},
              {"id": "too-many", "profileId": "$DEFAULT_PROFILE_ID", "name": "Big",
               "tabIds": ["a", "b", "c", "d"]},
              "broken",
              {"id": "also-kept", "profileId": "$DEFAULT_PROFILE_ID", "name": "Trip", "tabIds": ["c"]}
            ]
        """.trimIndent()

        val stacks = BrowserSessionStore.decodeTabStacks(raw, tabs)

        assertEquals(listOf("kept", "also-kept"), stacks.map { it.id })
        assertEquals(listOf("a", "b"), stacks.first().tabIds)
    }

    @Test
    fun `an unreadable group list decodes as empty`() {
        assertTrue(BrowserSessionStore.decodeTabStacks("{", listOf(tab("a"))).isEmpty())
    }

    private fun tab(id: String) = BrowserTab(id, 1L, url = "https://example.com/$id")
}
