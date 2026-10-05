package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordGeneratorInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theGeneratorMakesAPasswordAndHandsItOver() {
        val used = mutableListOf<String>()
        lateinit var state: PasswordGeneratorState
        composeRule.setContent {
            MaterialBrowserTheme {
                state = remember { PasswordGeneratorState(wordlist = listOf("acid", "acorn", "acre", "acts")) }
                PasswordGeneratorContent(
                    subtitle = "cloud.example.com",
                    useLabel = "Use",
                    useNeedsFingerprint = false,
                    onUse = { used += it },
                    state = state,
                )
            }
        }

        val first = state.value
        composeRule.onNodeWithTag(PasswordGeneratorTestTags.Refresh).performClick()
        assertNotEquals(first, state.value)

        composeRule.onNodeWithTag(PasswordGeneratorTestTags.DigitsAndSymbols).performScrollTo().assertIsOn().performClick()
        composeRule.onNodeWithTag(PasswordGeneratorTestTags.DigitsAndSymbols).assertIsOff()
        assertTrue(state.value.all(Char::isLetter))

        composeRule.onNodeWithTag(PasswordGeneratorTestTags.mode(PasswordGeneratorMode.Phrase)).performScrollTo().performClick()
        assertEquals(6, state.value.split("-").size)

        composeRule.onNodeWithTag(PasswordGeneratorTestTags.Use).performScrollTo().performClick()
        assertEquals(listOf(state.value), used)
    }
}
