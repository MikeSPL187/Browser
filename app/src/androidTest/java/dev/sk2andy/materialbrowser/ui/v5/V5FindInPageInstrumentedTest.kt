package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 find capsule on the device (FP-05): typing reaches the binding, «Not found» and «2 of 7»
 * show, the arrows are off with nothing to step to, and «Search» on the keyboard searches again
 * even then (FP-02), so a page that gave no answer can be asked once more.
 */
@RunWith(AndroidJUnit4::class)
class V5FindInPageInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val labels = V5FindInPageLabels(
        placeholder = "Find on page",
        counting = "Counting",
        previous = "Previous match",
        next = "Next match",
        done = "Done",
    )

    private var match by mutableStateOf("")
    private var counting by mutableStateOf(false)
    private var navigable by mutableStateOf(false)

    private fun show() {
        composeRule.setContent {
            var query by remember { mutableStateOf("") }
            VolaV5Theme {
                V5FindInPage(
                    query = query,
                    onQueryChange = {
                        query = it
                        calls += "query $it"
                    },
                    matchText = match,
                    isCounting = counting,
                    canNavigate = navigable,
                    labels = labels,
                    onPrevious = { calls += "previous" },
                    onNext = { calls += "next" },
                    onDone = { calls += "done" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun nothingFoundSaysSoAndSearchAsksAgain() {
        show()
        composeRule.onNodeWithTag(V5FindInPageTestTags.Query).performTextInput("aurora")
        match = "Not found"
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Not found").assertExists()
        composeRule.onNodeWithTag(V5FindInPageTestTags.Next).assertIsNotEnabled()
        composeRule.onNodeWithTag(V5FindInPageTestTags.Previous).assertIsNotEnabled()
        composeRule.onNodeWithTag(V5FindInPageTestTags.Query).performImeAction()

        assertEquals(listOf("query aurora", "next"), calls)
    }

    @Test
    fun matchesCountAndTheArrowsStepThroughThem() {
        show()
        counting = true
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Counting").assertExists()

        counting = false
        match = "2 of 7"
        navigable = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2 of 7").assertExists()
        composeRule.onNodeWithTag(V5FindInPageTestTags.Next).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(V5FindInPageTestTags.Previous).performClick()
        composeRule.onNodeWithTag(V5FindInPageTestTags.Done).performClick()

        assertEquals(listOf("next", "previous", "done"), calls)
    }
}
