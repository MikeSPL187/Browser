package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 site protection panel on the device (SPR-05): the main switch, the gentle step on «Site not
 * working right?» with its reload note (SPR-01), the report, and reset only after its question (SPR-02).
 */
@RunWith(AndroidJUnit4::class)
class V5SiteProtectionInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val site = V5SiteProtectionState(
        host = "news.example",
        protectionOn = true,
        strict = false,
        blocked = 12,
        privateTab = false,
        thirdPartyCookiesAllowed = false,
        cookieBannersHidden = true,
        deAmp = true,
        fingerprintingProtection = true,
        httpsOnly = "Always",
    )

    private val actions = V5SiteProtectionActions(
        onProtectionChange = { calls += "protection $it" },
        onStrictChange = { calls += "strict $it" },
        onThirdPartyCookiesChange = { calls += "third-party $it" },
        onCookieBannersChange = { calls += "banners $it" },
        onDeAmpChange = { calls += "amp $it" },
        onFingerprintingChange = { calls += "fingerprinting $it" },
        onHttpsOnly = { calls += "https-only" },
        onCopyReport = { calls += "report" },
        onReset = { calls += "reset" },
        onSiteInfo = { calls += "site info" },
    )

    private fun show(state: V5SiteProtectionState) {
        composeRule.setContent {
            VolaV5Theme { V5SiteProtection(visible = true, state = state, actions = actions, onDismiss = { calls += "dismiss" }) }
        }
        composeRule.waitForIdle()
    }

    private fun text(id: Int) = context.getString(id)

    @Test
    fun theMainSwitchTurnsTheSiteOff() {
        show(site)

        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Toggle).performClick()
        assertEquals(listOf("protection false"), calls)
    }

    @Test
    fun theGentleStepAllowsThirdPartyCookiesAndSaysThePagesReload() {
        show(site)

        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Broken).performClick()
        composeRule.onNodeWithText(text(R.string.v5_site_reload_note)).assertExists()
        composeRule.onNodeWithTag(V5SiteProtectionTestTags.ThirdParty).performClick()
        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Back).performClick()
        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Toggle).assertExists()

        assertEquals(listOf("third-party true"), calls)
    }

    @Test
    fun strictLeavesNoThirdPartyCookies() {
        show(site.copy(strict = true))

        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Broken).performClick()
        composeRule.onNodeWithTag(V5SiteProtectionTestTags.ThirdParty).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.v5_site_third_party_strict)).assertExists()
    }

    @Test
    fun resetAsksFirstAndTheReportCopies() {
        show(site)

        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Details).performClick()
        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Report).performClick()
        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Reset).performClick()
        assertEquals(listOf("report"), calls)

        composeRule.onNodeWithText(context.getString(R.string.v5_site_reset_title, site.host)).assertExists()
        composeRule.onNodeWithTag(V5SiteProtectionTestTags.ResetConfirm).performClick()
        assertEquals(listOf("report", "reset"), calls)
    }

    @Test
    fun aPrivateTabSaysOnlyItsPageReloadsAndTheChangeLastsUntilItCloses() {
        show(site.copy(privateTab = true))

        composeRule.onNodeWithText(text(R.string.v5_site_toggle_private)).assertExists()
        composeRule.onNodeWithTag(V5SiteProtectionTestTags.Details).performClick()
        composeRule.onNodeWithText(text(R.string.v5_site_reload_note_private)).assertExists()
    }
}
