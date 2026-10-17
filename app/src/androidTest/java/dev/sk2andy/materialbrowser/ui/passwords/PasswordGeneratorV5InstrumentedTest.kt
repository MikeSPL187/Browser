package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The v5 generator sheet: «new» makes another value, a switch changes the options, «Use» hands the shown value over. */
@RunWith(AndroidJUnit4::class)
class PasswordGeneratorV5InstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theSheetMakesAPasswordAndHandsItOver() {
        val used = mutableListOf<String>()
        lateinit var state: PasswordGeneratorState
        composeRule.setContent {
            VolaV5Theme {
                state = remember { PasswordGeneratorState(wordlist = null) }
                PasswordGeneratorSheetV5(
                    visible = true,
                    subtitle = "cloud.example.com",
                    useLabel = "Use",
                    useNeedsFingerprint = false,
                    onUse = { used += it },
                    onDismiss = {},
                    state = state,
                )
            }
        }
        composeRule.waitForIdle()

        val first = state.value
        composeRule.onNodeWithTag(PasswordGeneratorTestTags.Refresh).performClick()
        composeRule.runOnIdle { assertNotEquals(first, state.value) }

        val ambiguous = state.options.avoidAmbiguous
        composeRule.onNodeWithTag(PasswordGeneratorTestTags.AvoidAmbiguous).performClick()
        composeRule.runOnIdle { assertEquals(!ambiguous, state.options.avoidAmbiguous) }

        composeRule.onNodeWithTag(PasswordGeneratorTestTags.Use).performClick()
        composeRule.runOnIdle { assertEquals(listOf(state.value), used) }
    }
}
