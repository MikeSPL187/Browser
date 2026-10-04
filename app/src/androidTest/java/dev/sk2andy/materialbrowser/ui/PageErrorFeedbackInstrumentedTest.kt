package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
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
                    url = "https://north-guide.ru/old",
                )
            }
        }

        composeRule.onNodeWithText("Page not found").assertExists()
        composeRule.onNodeWithText("north-guide.ru", substring = true).assertExists()
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Retry).performClick()

        assertEquals(1, reloads.get())
    }

    @Test
    fun unreachablePageNamesTheSiteButNotTheEngineError() {
        composeRule.setContent {
            MaterialBrowserTheme {
                PageErrorFeedback(
                    state = PageErrorFeedbackState.Error("NS_ERROR_NET_TIMEOUT"),
                    onRetry = {},
                    url = "https://www.north-guide.ru/routes",
                )
            }
        }

        composeRule.onNodeWithText("Site isn’t responding").assertExists()
        composeRule.onNodeWithText("north-guide.ru", substring = true).assertExists()
        composeRule.onNodeWithText("NS_ERROR_NET_TIMEOUT", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Retry).assertExists()
    }

    @Test
    fun offlinePageOffersRetryAndNetworkSettings() {
        val reloads = AtomicInteger()
        composeRule.setContent {
            MaterialBrowserTheme {
                PageErrorFeedback(
                    state = PageErrorFeedbackState.Offline(),
                    onRetry = reloads::incrementAndGet,
                )
            }
        }

        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Offline).assertExists()
        composeRule.onNodeWithText("No connection")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.NetworkSettings).assertExists()
        composeRule.onNodeWithTag(PageErrorFeedbackTestTags.Retry).performClick()

        assertEquals(1, reloads.get())
    }
}
