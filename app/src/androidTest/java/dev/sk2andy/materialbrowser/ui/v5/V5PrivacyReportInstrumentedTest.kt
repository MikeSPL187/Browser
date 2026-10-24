package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.ProtectionSiteTrackers
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.data.ProtectionTracker
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import java.text.NumberFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 privacy report on the device (PR-04): the period switch, the findings, «Clear» only after
 * its question and open whenever anything is kept (PR-01), and the big number read as one line.
 */
@RunWith(AndroidJUnit4::class)
class V5PrivacyReportInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val month = ProtectionSummary(
        total = 120,
        siteCount = 9,
        trackerCount = 14,
        daily = List(30) { 4 },
        topSites = emptyList(),
        topTrackers = listOf(ProtectionTracker("adtrack.example", blocked = 40, siteCount = 6)),
        sitesWithMostTrackers = listOf(ProtectionSiteTrackers("news.example", trackerCount = 7)),
        firstEpochDay = 0,
        lastEpochDay = 29,
    )

    private val emptyWeek = month.copy(
        total = 0,
        siteCount = 0,
        trackerCount = 0,
        daily = List(7) { 0 },
        topTrackers = emptyList(),
        sitesWithMostTrackers = emptyList(),
        lastEpochDay = 6,
    )

    private fun show(summary: ProtectionSummary, windowDays: Int, hasData: Boolean = summary.total > 0) {
        composeRule.setContent {
            VolaV5Theme {
                V5PrivacyReport(
                    visible = true,
                    summary = summary,
                    windowDays = windowDays,
                    onWindowChange = { calls += "window $it" },
                    onClear = { calls += "clear" },
                    onDismiss = { calls += "dismiss" },
                    hasData = hasData,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theFindingsShowAndClearAsksFirst() {
        show(month, windowDays = 30)

        composeRule.onNodeWithText("adtrack.example", substring = true).assertExists()
        composeRule.onNodeWithText("news.example", substring = true).assertExists()
        composeRule.onNodeWithTag(V5PrivacyReportTestTags.Clear).assertIsEnabled().performClick()
        assertEquals(emptyList<String>(), calls)

        composeRule.onNodeWithText(context.getString(R.string.protection_report_clear_title)).assertExists()
        composeRule.onNodeWithTag(V5PrivacyReportTestTags.ClearConfirm).performClick()
        assertEquals(listOf("clear"), calls)
    }

    @Test
    fun anEmptyWeekSaysSoButClearsWhatTheMonthKeeps() {
        show(emptyWeek, windowDays = 7, hasData = true)

        composeRule.onNodeWithText(context.getString(R.string.v5_report_empty)).assertExists()
        composeRule.onNodeWithTag(V5PrivacyReportTestTags.Clear).assertIsEnabled()
    }

    @Test
    fun nothingKeptLeavesClearOff() {
        show(emptyWeek, windowDays = 7, hasData = false)

        composeRule.onNodeWithTag(V5PrivacyReportTestTags.Clear).assertIsNotEnabled()
    }

    @Test
    fun theBigNumberReadsAsOneLine() {
        show(month, windowDays = 30)

        val count = NumberFormat.getIntegerInstance(context.resources.configuration.locales[0]).format(month.total)
        val line = context.resources.getQuantityString(R.plurals.v5_report_blocked, month.total, count)
        composeRule.onNodeWithTag(V5PrivacyReportTestTags.Total).assertContentDescriptionEquals(line)
    }
}
