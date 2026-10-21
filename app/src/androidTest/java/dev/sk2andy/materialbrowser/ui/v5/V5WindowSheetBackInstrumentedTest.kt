package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Risk 1 of the independent check (20.10): back inside a [V5WindowSheet]. The sheet lives in a
 * dialog window of its own with the platform's back off, so back must reach the sheet's own
 * handler there: one back closes the sheet and only the sheet, the screen under it stays; a step
 * inside the sheet that handles back itself is left first.
 */
@RunWith(AndroidJUnit4::class)
class V5WindowSheetBackInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    @Test
    fun backClosesTheSheetAndNotTheScreenUnderIt() {
        composeRule.setContent {
            VolaV5Theme {
                var visible by remember { mutableStateOf(true) }
                V5WindowSheet(
                    visible = visible,
                    onDismiss = {
                        calls += "dismiss"
                        visible = false
                    },
                    title = "Sheet",
                ) {
                    Text(ROW)
                }
            }
        }
        awaitRow(present = true)

        Espresso.pressBack()
        awaitRow(present = false)

        assertEquals(listOf("dismiss"), calls)
        assertFalse("Back went past the sheet and closed the screen", composeRule.activity.isFinishing)
    }

    @Test
    fun aStepInsideTheSheetTakesBackFirst() {
        composeRule.setContent {
            VolaV5Theme {
                var visible by remember { mutableStateOf(true) }
                var step by remember { mutableStateOf(true) }
                V5WindowSheet(
                    visible = visible,
                    onDismiss = {
                        calls += "dismiss"
                        visible = false
                    },
                    title = "Sheet",
                ) {
                    BackHandler(enabled = step) {
                        calls += "step back"
                        step = false
                    }
                    Text(if (step) STEP else ROW)
                }
            }
        }
        awaitText(STEP, present = true)

        Espresso.pressBack()
        awaitRow(present = true)
        assertEquals(listOf("step back"), calls)

        Espresso.pressBack()
        awaitRow(present = false)
        assertEquals(listOf("step back", "dismiss"), calls)
        assertFalse(composeRule.activity.isFinishing)
    }

    private fun awaitRow(present: Boolean) = awaitText(ROW, present)

    private fun awaitText(text: String, present: Boolean) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            val found = composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes(atLeastOneRootRequired = false)
            found.isNotEmpty() == present
        }
    }

    private companion object {
        const val ROW = "A row of the sheet"
        const val STEP = "A second step"
        const val TIMEOUT_MILLIS = 5_000L
    }
}
