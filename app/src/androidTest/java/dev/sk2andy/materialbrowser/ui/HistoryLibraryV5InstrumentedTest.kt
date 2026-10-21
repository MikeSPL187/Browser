package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.BrowsingHistoryRules
import dev.sk2andy.materialbrowser.data.HistoryEntry
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 history on the device (audit 20.10, БИ-1): a swiped visit waits behind «Undo» and comes
 * back, and «Clear…» asks once more before it clears a period.
 */
@RunWith(AndroidJUnit4::class)
class HistoryLibraryV5InstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()
    private val visit = HistoryEntry(
        url = "https://polar-notes.example/night",
        title = "Polar night",
        lastVisitedAt = System.currentTimeMillis(),
        profileId = "personal",
    )

    private fun show() {
        composeRule.setContent {
            HistoryLibraryV5(
                profileId = "personal",
                history = listOf(visit),
                recallMatches = emptyList(),
                onRecallQuery = { _, _ -> },
                onDelete = { calls += "delete ${it.title}" },
                onClear = { calls += "clear" },
                onOpen = { calls += "open" },
                onSwitch = {},
                onClose = {},
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun aSwipedVisitComesBackWithUndo() {
        show()

        composeRule.onNodeWithTag(V5LibraryTestTags.row(BrowsingHistoryRules.entryKey(visit))).performTouchInput { swipeLeft() }
        composeRule.onNodeWithText(context.getString(R.string.action_undo)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(V5LibraryTestTags.row(BrowsingHistoryRules.entryKey(visit))).assertExists()
        assertTrue(calls.isEmpty())
    }

    @Test
    fun clearingAPeriodAsksFirst() {
        show()

        composeRule.onNodeWithText(context.getString(R.string.v5_history_clear)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.v5_history_clear_all)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.v5_history_clear_confirm_message)).assertExists()
        assertTrue(calls.isEmpty())
        composeRule.onNodeWithTag(HistoryLibraryV5TestTags.ClearConfirm).performClick()

        assertEquals(listOf("clear"), calls)
    }
}
