package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * СС-1 and А-3 (audit 20.10), FP-06 (audit 18.10): what TalkBack reads in a v5 text field, from the
 * tree it reads. An empty field says its name once — the grey hint the eye reads is not a second
 * stop; a filled field reads what was typed, with no name in contentDescription to drown it.
 */
@RunWith(AndroidJUnit4::class)
class V5FieldLabelInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aFormFieldSaysItsNameOnceAndThenWhatWasTyped() {
        composeRule.setContent {
            VolaV5Theme {
                var value by remember { mutableStateOf("") }
                V5NameField(
                    value = value,
                    placeholder = NAME,
                    onValueChange = { value = it },
                    onDone = {},
                    maxLength = 40,
                    modifier = Modifier.testTag(FIELD),
                )
            }
        }
        assertReadsOnly(NAME)

        composeRule.onNodeWithTag(FIELD).performTextInput(TYPED)
        assertReadsTyped()
    }

    @Test
    fun aSearchFieldSaysItsNameOnceAndThenWhatWasTyped() {
        composeRule.setContent {
            VolaV5Theme {
                var query by remember { mutableStateOf("") }
                V5SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    hint = NAME,
                    fieldModifier = Modifier.testTag(FIELD),
                )
            }
        }
        assertReadsOnly(NAME)

        composeRule.onNodeWithTag(FIELD).performTextInput(TYPED)
        assertReadsTyped()
    }

    /** One stop says [name]: the field; the hint drawn under it is not read on its own. */
    private fun assertReadsOnly(name: String) {
        composeRule.waitForIdle()
        val stops = composeRule.onAllNodes(hasText(name) or hasContentDescription(name))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        assertEquals("TalkBack stops that say «$name»", 1, stops.size)
        assertEquals(listOf(name), composeRule.onNodeWithTag(FIELD).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription))
    }

    private fun assertReadsTyped() {
        composeRule.waitForIdle()
        val field = composeRule.onNodeWithTag(FIELD).fetchSemanticsNode().config
        assertEquals(TYPED, field.getOrNull(SemanticsProperties.EditableText)?.text)
        assertNull("A name in contentDescription would be read instead of the text", field.getOrNull(SemanticsProperties.ContentDescription))
        val stops = composeRule.onAllNodes(hasText(NAME) or hasContentDescription(NAME))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        assertEquals("A stop still says the name over the typed text", 0, stops.size)
    }

    private companion object {
        const val FIELD = "field"
        const val NAME = "Profile name"
        const val TYPED = "Northern"
    }
}
