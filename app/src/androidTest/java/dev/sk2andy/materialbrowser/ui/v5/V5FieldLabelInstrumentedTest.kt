package dev.sk2andy.materialbrowser.ui.v5

import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * СС-1 and А-3 (audit 20.10), FP-06 (audit 18.10): what TalkBack is handed for a v5 text field, read
 * from the real [AccessibilityNodeInfo] tree the platform builds, not from Compose semantics.
 *
 * TalkBack speaks a focusable node with the words of its children that are not focusable on their
 * own; Compose puts the name of a field that merges its children into such a child (its «fake node»
 * for contentDescription) rather than onto the edit node itself. So the name is looked for in the
 * edit node and its non-focusable subtree: an empty field says it exactly once, nothing else on screen
 * says it again, and a filled field carries the typed text with the name gone. TalkBack's own voice
 * is not on the emulator; on failure the message lists the whole subtree.
 */
@RunWith(AndroidJUnit4::class)
class V5FieldLabelInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aFormFieldSaysItsNameOnceAndThenWhatWasTyped() {
        show {
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
        checkField(FIELD)
    }

    @Test
    fun aSearchFieldSaysItsNameOnceAndThenWhatWasTyped() {
        show {
            var query by remember { mutableStateOf("") }
            V5SearchField(
                query = query,
                onQueryChange = { query = it },
                hint = NAME,
                fieldModifier = Modifier.testTag(FIELD),
            )
        }
        checkField(FIELD)
    }

    @Test
    fun findOnPageSaysItsNameOnceAndThenWhatWasTyped() {
        show {
            var query by remember { mutableStateOf("") }
            V5FindInPage(
                query = query,
                onQueryChange = { query = it },
                matchText = "",
                isCounting = false,
                canNavigate = false,
                labels = V5FindInPageLabels(
                    placeholder = NAME,
                    counting = "Counting",
                    previous = "Previous match",
                    next = "Next match",
                    done = "Done",
                ),
                onPrevious = {},
                onNext = {},
                onDone = {},
            )
        }
        checkField(V5FindInPageTestTags.Query)
    }

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { VolaV5Theme { Column { content() } } }
    }

    private fun checkField(tag: String) {
        val empty = editableNode()
        val emptyWords = spoken(empty)
        assertEquals("The empty field does not say «$NAME» once: ${dump(empty)}", 1, emptyWords.count { it == NAME })
        assertEquals("«$NAME» is said again elsewhere on screen: ${dump(root())}", 1, countOnScreen(root(), NAME))

        composeRule.onNodeWithTag(tag).performTextInput(TYPED)

        val filled = editableNode()
        assertEquals(dump(filled), TYPED, filled.text?.toString())
        assertTrue("The name is still said over the typed text: ${dump(filled)}", NAME !in spoken(filled))
    }

    private fun root(): AccessibilityNodeInfo {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(ATTEMPTS) {
            composeRule.waitForIdle()
            automation.rootInActiveWindow?.let { return it }
            Thread.sleep(POLL_MILLIS)
        }
        error("No window reached the accessibility tree")
    }

    private fun editableNode(): AccessibilityNodeInfo {
        repeat(ATTEMPTS) {
            find(root(), AccessibilityNodeInfo::isEditable)?.let { return it }
            Thread.sleep(POLL_MILLIS)
        }
        error("No editable node reached the accessibility tree")
    }

    private fun find(node: AccessibilityNodeInfo, matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (matches(node)) return node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { find(it, matches) }?.let { return it }
        }
        return null
    }

    /** The words TalkBack builds for [node]: its own, then those of children it does not stop on. */
    private fun spoken(node: AccessibilityNodeInfo): List<String> = buildList {
        node.contentDescription?.let { add(it.toString()) }
        node.text?.let { add(it.toString()) }
        if (node.isShowingHintText) node.hintText?.let { add(it.toString()) }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            if (!child.isFocusable && !child.isClickable && !child.isScreenReaderFocusable) addAll(spoken(child))
        }
    }.filter(String::isNotEmpty)

    private fun countOnScreen(node: AccessibilityNodeInfo, word: String): Int {
        var count = listOfNotNull(node.contentDescription, node.text).count { it.toString() == word }
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { count += countOnScreen(it, word) }
        }
        return count
    }

    private fun dump(node: AccessibilityNodeInfo, depth: Int = 0): String = buildString {
        append("\n").append("  ".repeat(depth))
        append("${node.className} text=${node.text} cd=${node.contentDescription} hint=${node.hintText}")
        append(" showingHint=${node.isShowingHintText} editable=${node.isEditable} focusable=${node.isFocusable}")
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { append(dump(it, depth + 1)) }
        }
    }

    private companion object {
        const val FIELD = "field"
        const val NAME = "Profile name"
        const val TYPED = "Northern"
        const val ATTEMPTS = 50
        const val POLL_MILLIS = 100L
    }
}
