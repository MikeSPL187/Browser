package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class AddressEditorRulesTest {
    private val none = AddressEditorState.NO_HIGHLIGHT

    @Test
    fun `from the field down enters at the top and up at the bottom`() {
        assertEquals(0, AddressEditorRules.movedHighlight(none, 1, 4))
        assertEquals(3, AddressEditorRules.movedHighlight(none, -1, 4))
    }

    @Test
    fun `moving stays in the list and above the top returns to the field`() {
        assertEquals(2, AddressEditorRules.movedHighlight(1, 1, 4))
        assertEquals(3, AddressEditorRules.movedHighlight(3, 1, 4))
        assertEquals(none, AddressEditorRules.movedHighlight(0, -1, 4))
    }

    @Test
    fun `no suggestions or a stale highlight resets`() {
        assertEquals(none, AddressEditorRules.movedHighlight(2, 1, 0))
        assertEquals(0, AddressEditorRules.movedHighlight(7, 1, 3))
    }

    @Test
    fun `new suggestions for the same query keep the chosen row where it moved`() {
        val before = listOf("tab:a", "history:b", "search:c")
        val after = listOf("tab:a", "search:x", "history:b", "search:c")
        assertEquals(2, AddressEditorRules.carriedHighlight(before, 1, after))
    }

    @Test
    fun `nothing chosen or the chosen row gone carries nothing`() {
        val before = listOf("tab:a", "history:b")
        assertEquals(null, AddressEditorRules.carriedHighlight(before, none, before))
        assertEquals(null, AddressEditorRules.carriedHighlight(before, 1, listOf("tab:a")))
    }
}
