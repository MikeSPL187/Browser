package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AddressSuggestionRowRulesTest {
    @Test
    fun `the tile shows the title's first letter, else the host's`() {
        assertEquals("П", AddressSuggestionRowRules.monogram("погода на Байкале", "pogoda.ru"))
        assertEquals("E", AddressSuggestionRowRules.monogram("  «…»", "www.example.com"))
        assertEquals("", AddressSuggestionRowRules.monogram("", ""))
    }

    @Test
    fun `only the part after the typed text is bold`() {
        assertEquals(10, AddressSuggestionRowRules.completionStart("байкал лёд толщина", "Байкал лёд"))
        assertEquals(4, AddressSuggestionRowRules.completionStart("zen browser", " zen "))
        assertEquals(6, AddressSuggestionRowRules.completionStart("другое", "лёд"))
        assertEquals(3, AddressSuggestionRowRules.completionStart("лёд", "  "))
    }
}
