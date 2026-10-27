package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 link preview on the device (LG-04): what the card offers, the actions it ends with, a
 * failed page that can be tried again, and a dangerous one that offers only a way back.
 */
@RunWith(AndroidJUnit4::class)
class V5LinkGlanceInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val page = V5LinkGlanceState(
        url = "https://news.example/story",
        progress = null,
        privateTab = false,
        message = null,
        canOpenInPrivate = true,
        canDownloadImage = false,
    )

    private val actions = V5LinkGlanceActions(
        onOpen = { calls += "open" },
        onOpenInBackground = { calls += "background" },
        onOpenInNewTab = { calls += "new tab" },
        onOpenInPrivate = { calls += "private" },
        onCopy = { calls += "copy" },
        onShare = { calls += "share" },
        onDownloadLink = { calls += "download link" },
        onDownloadImage = { calls += "download image" },
        onRetry = { calls += "retry" },
        onDismiss = { calls += "dismiss" },
    )

    private fun show(state: V5LinkGlanceState) {
        composeRule.setContent {
            VolaV5Theme {
                V5LinkGlance(
                    state = state,
                    actions = actions,
                    origin = null,
                    onPageScale = {},
                    entered = true,
                    preview = { modifier -> Box(modifier) },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun text(id: Int) = context.getString(id)

    @Test
    fun theRoundButtonsEndThePreviewWithTheirAction() {
        show(page)

        composeRule.onNodeWithText(text(R.string.glance_not_in_history)).assertExists()
        composeRule.onNodeWithTag(V5LinkGlanceTestTags.Copy).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("copy"), calls)
    }

    @Test
    fun moreKeepsTheRestAndPrivateOnlyWhereItWorks() {
        show(page.copy(canOpenInPrivate = false))

        composeRule.onNodeWithTag(V5LinkGlanceTestTags.More).performClick()
        composeRule.onNodeWithText(text(R.string.v5_glance_private)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.v5_glance_open_background)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("background"), calls)
    }

    @Test
    fun aSwipeDownClosesThePreview() {
        show(page)

        composeRule.onNodeWithTag(V5LinkGlanceTestTags.Card).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun aPageThatFailedCanBeTriedAgainInPlace() {
        show(page.copy(message = V5LinkGlanceMessage("No connection", "Check the network.", danger = false, retryable = true)))

        composeRule.onNodeWithText("No connection").assertExists()
        composeRule.onNodeWithTag(V5LinkGlanceTestTags.Retry).performClick()
        composeRule.onNodeWithTag(V5LinkGlanceTestTags.Card).assertExists()

        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun aDangerousSiteOffersOnlyAWayBack() {
        show(page.copy(message = V5LinkGlanceMessage("Dangerous site", "Vola stopped it.", danger = true, retryable = false)))

        composeRule.onNodeWithTag(V5LinkGlanceTestTags.Retry).assertDoesNotExist()
        composeRule.onNodeWithTag(V5LinkGlanceTestTags.Message).assertExists()
        composeRule.onNodeWithText(text(R.string.glance_close)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun aPrivateTabSaysThePreviewKeepsNothing() {
        show(page.copy(privateTab = true))

        composeRule.onNodeWithText(text(R.string.v5_glance_private_note)).assertExists()
    }
}
