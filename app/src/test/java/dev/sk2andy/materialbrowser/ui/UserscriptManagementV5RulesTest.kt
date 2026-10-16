package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.shared.topping.ToppingFrameScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserscriptManagementV5RulesTest {
    @Test
    fun `patterns drop blanks and repeats and keep the script's order`() {
        val script = script(urlPatterns = listOf("https://b.example/*", "", "https://a.example/*", "https://b.example/*"))

        assertEquals(
            listOf("https://b.example/*", "https://a.example/*"),
            UserscriptManagementV5Rules.patterns(script),
        )
    }

    @Test
    fun `frame choices never go past what the metadata asks for`() {
        assertEquals(
            listOf(ToppingFrameScope.Top),
            UserscriptManagementV5Rules.frameChoices(script(declared = ToppingFrameScope.Top)),
        )
        assertEquals(
            listOf(ToppingFrameScope.Top, ToppingFrameScope.SameOrigin),
            UserscriptManagementV5Rules.frameChoices(script(declared = ToppingFrameScope.SameOrigin)),
        )
        assertEquals(
            ToppingFrameScope.entries.toList(),
            UserscriptManagementV5Rules.frameChoices(script(declared = ToppingFrameScope.AllMatching)),
        )
    }

    @Test
    fun `frame access is editable only when there is something to narrow and nothing is pending`() {
        assertFalse(UserscriptManagementV5Rules.frameScopeEditable(script(declared = ToppingFrameScope.Top), busy = false))
        assertTrue(UserscriptManagementV5Rules.frameScopeEditable(script(declared = ToppingFrameScope.SameOrigin), busy = false))
        assertFalse(UserscriptManagementV5Rules.frameScopeEditable(script(declared = ToppingFrameScope.AllMatching), busy = true))
    }

    @Test
    fun `save needs a source and no save in flight`() {
        assertTrue(UserscriptManagementV5Rules.canSave("// ==UserScript==", saving = false))
        assertFalse(UserscriptManagementV5Rules.canSave("  \n", saving = false))
        assertFalse(UserscriptManagementV5Rules.canSave("// ==UserScript==", saving = true))
    }

    @Test
    fun `scope labels start rows with a capital`() {
        assertEquals("Main page only", UserscriptManagementV5Rules.capitalized("main page only"))
        assertEquals("Только основная страница", UserscriptManagementV5Rules.capitalized("только основная страница"))
    }

    @Test
    fun `notices put the host first and then the last error`() {
        assertEquals(emptyList<UserscriptNotice>(), UserscriptManagementV5Rules.notices(true, null))
        assertEquals(
            listOf(UserscriptNotice(null), UserscriptNotice("Couldn’t save")),
            UserscriptManagementV5Rules.notices(false, "Couldn’t save"),
        )
        assertEquals(listOf(UserscriptNotice("Busy")), UserscriptManagementV5Rules.notices(true, "Busy"))
    }

    private fun script(
        declared: ToppingFrameScope = ToppingFrameScope.Top,
        urlPatterns: List<String> = listOf("https://example.com/*"),
    ) = UserscriptUiItem(
        id = "id",
        name = "Name",
        source = "",
        enabled = true,
        runAtLabel = "document end",
        urlPatterns = urlPatterns,
        declaredFrameScope = declared,
        allowedFrameScope = ToppingFrameScope.Top,
    )
}
