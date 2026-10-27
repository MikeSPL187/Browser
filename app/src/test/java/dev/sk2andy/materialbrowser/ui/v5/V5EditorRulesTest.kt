package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import dev.sk2andy.materialbrowser.browser.ClipboardOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5EditorRulesTest {
    private fun state(text: String = "", overStart: Boolean = true, clipboard: ClipboardOffer? = null) =
        V5EditorState(
            value = TextFieldValue(text),
            layout = V5SuggestionLayout.Empty,
            clipboard = clipboard,
            overStart = overStart,
        )

    @Test
    fun `an empty editor over a new tab lets the pinned sites show through`() {
        assertTrue(V5EditorRules.seeThrough(state()))
    }

    @Test
    fun `typing, a page under it or a clip on offer bring the suggestion list back`() {
        assertFalse(V5EditorRules.seeThrough(state(text = "vola")))
        assertFalse(V5EditorRules.seeThrough(state(overStart = false)))
        assertFalse(V5EditorRules.seeThrough(state(clipboard = ClipboardOffer.entries.first())))
    }

    @Test
    fun `the field starts as wide as the bar's capsule and grows by the buttons it covers`() {
        // Bar: 2 round buttons of 48, two gaps of 8 and the edge of 12; editor: «Cancel» of 72 and 8.
        assertEquals(44f, V5EditorRules.morphInsetPx(Density(1f)), 0.001f)
        assertEquals(132f, V5EditorRules.morphInsetPx(Density(3f)), 0.001f)
    }
}
