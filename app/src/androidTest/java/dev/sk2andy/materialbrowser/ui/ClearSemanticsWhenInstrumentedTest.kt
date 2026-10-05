package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClearSemanticsWhenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun subtreeReturnsToTheTreeEveryTimeTheFlagClears() {
        var hidden by mutableStateOf(true)
        composeRule.setContent {
            Box(Modifier.clearSemanticsWhen(hidden)) {
                Text("More options")
            }
        }

        repeat(3) {
            composeRule.onAllNodesWithText("More options").fetchSemanticsNodes().let { nodes ->
                assertEquals(0, nodes.size)
            }
            hidden = false
            composeRule.waitForIdle()
            composeRule.onNodeWithText("More options").assertIsDisplayed()
            hidden = true
            composeRule.waitForIdle()
        }
    }
}
