package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5FeedbackNotice
import dev.sk2andy.materialbrowser.ui.v5.V5FeedbackNoticeTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5FeedbackVisuals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The v5 notice line: the message, its action and «Close» answer the snackbar's channel as before. */
@RunWith(AndroidJUnit4::class)
class V5FeedbackNoticeInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theActionAndCloseAnswerTheMessage() {
        val answers = mutableListOf<String>()
        val data = object : SnackbarData {
            override val visuals: SnackbarVisuals =
                V5FeedbackVisuals("Tab closed", actionLabel = "Undo", withDismissAction = true)

            override fun performAction() {
                answers += "action"
            }

            override fun dismiss() {
                answers += "dismiss"
            }
        }
        composeRule.setContent { VolaV5Theme { V5FeedbackNotice(data) } }

        composeRule.onNodeWithText("Tab closed").assertExists()
        composeRule.onNodeWithTag(V5FeedbackNoticeTestTags.Action).performClick()
        composeRule.onNodeWithTag(V5FeedbackNoticeTestTags.Close).performClick()

        assertEquals(listOf("action", "dismiss"), answers)
    }
}
