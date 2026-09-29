package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageErrorFeedbackInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun notFoundPageOffersReloadWithoutTechnicalErrorText() {
        val reloads = AtomicInteger()
        composeRule.setContent {
            MaterialBrowserTheme {
                PageErrorFeedback(
                    state = PageErrorFeedbackState.NotFound,
                    onRetry = reloads::incrementAndGet,
                )
            }
        }

        composeRule.onNodeWithText("This page was snacked away").assertExists()
        composeRule.onNodeWithContentDescription("Destination not found").assertExists()
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Retry).performClick()

        assertEquals(1, reloads.get())
    }

    @Test
    fun unreachablePageUsesFriendlyMissingDestinationLayout() {
        composeRule.setContent {
            MaterialBrowserTheme {
                PageErrorFeedback(
                    state = PageErrorFeedbackState.Error("unknown host"),
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Website not found").assertExists()
        composeRule.onNodeWithContentDescription("Destination not found").assertExists()
        composeRule.onNodeWithText("unknown host").assertDoesNotExist()
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Retry).assertExists()
    }

    @Test
    fun offlinePageWaitsForTheConnectionWithoutReloadButton() {
        composeRule.setContent {
            MaterialBrowserTheme {
                PageErrorFeedback(
                    state = PageErrorFeedbackState.Offline(),
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Offline).assertExists()
        composeRule.onNodeWithText("No connection").assertExists()
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.OfflinePill).assertExists()
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Retry).assertDoesNotExist()
    }

    @Test
    fun reconnectAnnouncesItselfAndLoadsOnlyAfterButtonClick() {
        val reloads = AtomicInteger()
        val state = mutableStateOf<PageErrorFeedbackState>(PageErrorFeedbackState.Offline())
        composeRule.setContent {
            MaterialBrowserTheme {
                PageErrorFeedback(
                    state = state.value,
                    onRetry = reloads::incrementAndGet,
                )
            }
        }

        composeRule.runOnIdle {
            state.value = PageErrorFeedbackState.Offline(isOnlineReady = true)
        }
        composeRule.onNodeWithText("Back online")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.OfflinePill).assertDoesNotExist()
        assertEquals(0, reloads.get())

        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Retry).performClick()

        assertEquals(1, reloads.get())
    }
}
