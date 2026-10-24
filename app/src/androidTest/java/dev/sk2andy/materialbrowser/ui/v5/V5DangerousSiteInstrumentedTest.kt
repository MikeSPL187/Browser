package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.ui.DangerousSiteTestTags
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 dangerous site screen (canvas «Danger»): «Back to safety» first, the reason opens in place
 * with the real site for a look-alike, and a site Safe Browsing reported has no way through.
 */
@RunWith(AndroidJUnit4::class)
class V5DangerousSiteInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(site: BlockedSite) {
        composeRule.setContent {
            VolaV5Theme {
                V5DangerousSite(
                    site = site,
                    onBackToSafety = { calls += "back" },
                    onOpenAnyway = { calls += "anyway" },
                    onOpenRealSite = { calls += "real" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theReasonOpensInPlaceAndOffersTheRealSite() {
        show(BlockedSite(url = "https://bank-exarnple.ru/", host = "bank-exarnple.ru", imitatedHost = "bank.example.ru"))

        composeRule.onNodeWithText("bank-exarnple.ru").assertIsDisplayed()
        composeRule.onNodeWithTag(V5DangerousSiteTestTags.WhyText).assertDoesNotExist()
        composeRule.onNodeWithTag(V5DangerousSiteTestTags.Why).performScrollTo().performClick()
        composeRule.onNodeWithTag(V5DangerousSiteTestTags.WhyText).assertExists()
        composeRule.onNodeWithTag(V5DangerousSiteTestTags.RealSite).performScrollTo().performClick()
        composeRule.onNodeWithTag(DangerousSiteTestTags.OpenAnyway).performScrollTo().performClick()
        composeRule.onNodeWithTag(DangerousSiteTestTags.Back).performScrollTo().performClick()
        assertEquals(listOf("real", "anyway", "back"), calls)
    }

    @Test
    fun aReportedSiteHasNoWayThrough() {
        show(BlockedSite(url = "https://login-bank-help.top/", host = "login-bank-help.top", reportedBySafeBrowsing = true))

        composeRule.onNodeWithTag(DangerousSiteTestTags.OpenAnyway).assertDoesNotExist()
        composeRule.onNodeWithTag(V5DangerousSiteTestTags.Why).performScrollTo().performClick()
        composeRule.onNodeWithTag(V5DangerousSiteTestTags.RealSite).assertDoesNotExist()
    }
}
