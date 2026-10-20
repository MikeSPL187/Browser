package dev.sk2andy.materialbrowser.ui.v5

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
 * The v5 password check on the device (PH-10): the findings by section, the leak check started only
 * by a touch, «Stop» while it runs (PH-04), what a failed check says, and a finding's page.
 */
@RunWith(AndroidJUnit4::class)
class V5PasswordHealthInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val reused = V5PasswordFinding("l1", "news.example", "ann", V5PasswordFindingKind.Reused, count = 2)
    private val actions = V5PasswordHealthActions(
        onBack = { calls += "back" },
        onFinding = { calls += "finding ${it.loginId}" },
        onCheckLeaks = { calls += "check" },
        onStopLeakCheck = { calls += "stop" },
    )

    private fun show(state: V5PasswordHealthState) {
        composeRule.setContent { VolaV5Theme { V5PasswordHealth(state = state, actions = actions) } }
        composeRule.waitForIdle()
    }

    @Test
    fun findingsOpenAndTheLeakCheckWaitsForATouch() {
        show(V5PasswordHealthState(total = 3, findings = listOf(reused), leakCheck = V5LeakCheck.NotChecked))

        composeRule.onNodeWithTag(V5PasswordHealthTestTags.Summary).assertExists()
        assertEquals(emptyList<String>(), calls)
        composeRule.onNodeWithTag(V5PasswordHealthTestTags.row(V5PasswordFindingKind.Reused, "l1")).performClick()
        composeRule.onNodeWithTag(V5PasswordHealthTestTags.LeakCheck).performClick()

        assertEquals(listOf("finding l1", "check"), calls)
    }

    @Test
    fun aRunningCheckCanBeStopped() {
        show(V5PasswordHealthState(total = 3, findings = emptyList(), leakCheck = V5LeakCheck.Checking, leakProgress = 0.4f))

        composeRule.onNodeWithTag(V5PasswordHealthTestTags.StopLeakCheck).performClick()
        assertEquals(listOf("stop"), calls)
    }

    @Test
    fun aFailedCheckSaysSoAndCanRunAgain() {
        show(
            V5PasswordHealthState(
                total = 3,
                findings = emptyList(),
                leakCheck = V5LeakCheck.Failed,
                checkedAt = "10:42",
                unchecked = 2,
            ),
        )

        composeRule.onNodeWithTag(V5PasswordHealthTestTags.AllFine).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.passwords_health_check_failed)).assertExists()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.v5_password_health_unchecked, 2, 2))
            .assertExists()
        composeRule.onNodeWithTag(V5PasswordHealthTestTags.LeakCheck).performClick()
        assertEquals(listOf("check"), calls)
    }

    @Test
    fun aFindingsPageOffersTheLoginAndAChange() {
        composeRule.setContent {
            VolaV5Theme {
                V5PasswordFindingPage(
                    finding = reused,
                    onBack = { calls += "back" },
                    onChange = { calls += "change" },
                    onOpenLogin = { calls += "login" },
                )
            }
        }

        composeRule.onNodeWithText("ann").assertExists()
        composeRule.onNodeWithTag(V5PasswordHealthTestTags.OpenLogin).performClick()
        composeRule.onNodeWithTag(V5PasswordHealthTestTags.Change).performClick()
        assertEquals(listOf("login", "change"), calls)
    }
}
