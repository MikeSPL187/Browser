package dev.sk2andy.materialbrowser.ui.v5

import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What TalkBack is handed for a v5 text field (FP-06, СС-1, А-3), read from the real
 * [AccessibilityNodeInfo] the platform builds, not from Compose semantics: while the field is empty
 * its name is the hint; once something is typed the node carries the typed text and no description
 * that would be spoken in its place. TalkBack's own voice is not on the emulator; this is its input.
 */
@RunWith(AndroidJUnit4::class)
class V5FieldTalkBackInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val labels = V5FindInPageLabels(
        placeholder = "Find on page",
        counting = "Counting",
        previous = "Previous match",
        next = "Next match",
        done = "Done",
    )

    private fun editableNode(): AccessibilityNodeInfo {
        composeRule.waitForIdle()
        val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow
        assertNotNull("No accessibility window", root)
        return findEditable(root!!) ?: error("No editable node in the window")
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let(::findEditable)?.let { return it }
        }
        return null
    }

    @Test
    fun findOnPageSaysItsNameWhileEmptyAndTheTypedTextAfter() {
        composeRule.setContent {
            var query by remember { mutableStateOf("") }
            VolaV5Theme {
                V5FindInPage(
                    query = query,
                    onQueryChange = { query = it },
                    matchText = "",
                    isCounting = false,
                    canNavigate = false,
                    labels = labels,
                    onPrevious = {},
                    onNext = {},
                    onDone = {},
                )
            }
        }

        assertEquals("Find on page", editableNode().contentDescription?.toString())

        composeRule.onNodeWithTag(V5FindInPageTestTags.Query).performTextInput("aurora")

        val typed = editableNode()
        assertEquals("aurora", typed.text?.toString())
        assertNull(typed.contentDescription)
    }

    @Test
    fun aSearchFieldSaysItsNameWhileEmptyAndTheTypedTextAfter() {
        composeRule.setContent {
            var query by remember { mutableStateOf("") }
            VolaV5Theme {
                Column {
                    V5SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        hint = "Search history",
                        fieldModifier = Modifier.testTag(FIELD),
                    )
                }
            }
        }

        assertEquals("Search history", editableNode().contentDescription?.toString())

        composeRule.onNodeWithTag(FIELD).performTextInput("kotlin")

        val typed = editableNode()
        assertEquals("kotlin", typed.text?.toString())
        assertNull(typed.contentDescription)
    }

    private companion object {
        const val FIELD = "v5-field-under-test"
    }
}
