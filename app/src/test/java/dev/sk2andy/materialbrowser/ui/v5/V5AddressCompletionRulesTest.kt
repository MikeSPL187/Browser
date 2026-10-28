package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.data.AddressSuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V5AddressCompletionRulesTest {
    private val site = AddressSuggestionItem.Navigation(
        AddressSuggestion("https://www.polar-notes.ru/noch", "Полярная ночь"),
    )

    private fun typed(text: String) = TextFieldValue(text, TextRange(text.length))

    @Test
    fun `a site name being typed is completed to the best match's site, without www`() {
        assertEquals("ar-notes.ru", V5AddressCompletionRules.suffix(typed("pol"), site, deleting = false))
        assertEquals("ar-notes.ru", V5AddressCompletionRules.suffix(typed("POL"), site, deleting = false))
    }

    @Test
    fun `go opens the typed text with the completion`() {
        assertEquals("polar-notes.ru", V5AddressCompletionRules.submitted("pol", "ar-notes.ru"))
        assertEquals("pol", V5AddressCompletionRules.submitted("pol", null))
    }

    @Test
    fun `after delete nothing is completed until the next letter`() {
        assertNull(V5AddressCompletionRules.suffix(typed("pol"), site, deleting = true))
    }

    @Test
    fun `only at the end of the text, with nothing selected`() {
        assertNull(V5AddressCompletionRules.suffix(TextFieldValue("pol", TextRange(1)), site, deleting = false))
        assertNull(V5AddressCompletionRules.suffix(TextFieldValue("pol", TextRange(0, 3)), site, deleting = false))
    }

    @Test
    fun `a query, a path, another site, a whole site or a search is not completed`() {
        assertNull(V5AddressCompletionRules.suffix(typed("pol n"), site, deleting = false))
        assertNull(V5AddressCompletionRules.suffix(typed("polar-notes.ru/"), site, deleting = false))
        assertNull(V5AddressCompletionRules.suffix(typed("north"), site, deleting = false))
        assertNull(V5AddressCompletionRules.suffix(typed("polar-notes.ru"), site, deleting = false))
        assertNull(V5AddressCompletionRules.suffix(typed("pol"), AddressSuggestionItem.Search("polar"), deleting = false))
        assertNull(V5AddressCompletionRules.suffix(typed("pol"), null, deleting = false))
    }

    @Test
    fun `a page that is not on the web is not completed`() {
        val file = AddressSuggestionItem.Navigation(AddressSuggestion("file:///sdcard/polar.html", "Polar"))
        assertNull(V5AddressCompletionRules.suffix(typed("pol"), file, deleting = false))
    }
}
