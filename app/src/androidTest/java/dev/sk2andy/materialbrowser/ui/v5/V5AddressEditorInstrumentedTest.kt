package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.data.AddressSuggestion
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 address editor on the device (audit 20.10, А-7): a tap opens a row, a long press fills the
 * field from a page row, a search row fills only by its button (А-4), and Esc cancels (А-5).
 */
@RunWith(AndroidJUnit4::class)
class V5AddressEditorInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()
    private val page = AddressSuggestionItem.Navigation(AddressSuggestion("https://polar-notes.example", "Polar notes"))
    private val search = AddressSuggestionItem.Search("polar night")

    private fun show() {
        composeRule.setContent {
            VolaV5Theme {
                V5AddressEditor(
                    state = V5EditorState(
                        value = TextFieldValue("pol"),
                        layout = V5SuggestionLayoutRules.layout(listOf(page, search), query = "pol"),
                    ),
                    onValueChange = {},
                    onSubmit = { calls += "submit $it" },
                    onSelect = { calls += "select ${it.stableId}" },
                    onFill = { calls += "fill ${it.stableId}" },
                    onClipboard = { calls += "clipboard" },
                    onCancel = { calls += "cancel" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun aTapOpensARowAndALongPressFillsTheField() {
        show()

        composeRule.onNodeWithTag(V5EditorTestTags.row(page)).performClick()
        composeRule.onNodeWithTag(V5EditorTestTags.row(page)).performTouchInput { longClick() }

        assertEquals(listOf("select ${page.stableId}", "fill ${page.stableId}"), calls)
    }

    @Test
    fun aSearchRowFillsByItsButton() {
        show()

        composeRule.onNodeWithContentDescription(context.getString(R.string.address_suggestion_fill)).performClick()

        assertEquals(listOf("fill ${search.stableId}"), calls)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun escapeCancels() {
        show()

        composeRule.onNodeWithTag(V5EditorTestTags.Field).performKeyInput { pressKey(Key.Escape) }

        assertEquals(listOf("cancel"), calls)
    }
}
