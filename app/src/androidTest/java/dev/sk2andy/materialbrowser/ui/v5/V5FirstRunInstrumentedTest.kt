package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 first run on the device (FR-02): the welcome's promises and its two ways on, «Open links in
 * Vola?» answered either way, and the gesture card's «Done» reachable by scrolling (FR-01).
 */
@RunWith(AndroidJUnit4::class)
class V5FirstRunInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    @Test
    fun theWelcomeShowsItsPromisesAndGoesOnOrMoves() {
        composeRule.setContent {
            VolaV5Theme {
                V5FirstRunWelcome(
                    promises = listOf(
                        V5FirstRunPromise(VolaIcons.VerifiedUser, V5Tile.Green, "Protected from the start", "Ads and trackers blocked."),
                    ),
                    onStart = { calls += "start" },
                    onImport = { calls += "import" },
                )
            }
        }

        composeRule.onNodeWithText("Protected from the start").assertExists()
        composeRule.onNodeWithTag(V5FirstRunTestTags.Import).performClick()
        composeRule.onNodeWithTag(V5FirstRunTestTags.Start).performClick()
        assertEquals(listOf("import", "start"), calls)
    }

    @Test
    fun theDefaultBrowserQuestionAnswersEitherWay() {
        composeRule.setContent {
            VolaV5Theme {
                V5FirstRunDefaultBrowser(onMakeDefault = { calls += "make default" }, onNotNow = { calls += "not now" })
            }
        }

        composeRule.onNodeWithTag(V5FirstRunTestTags.MakeDefault).performClick()
        composeRule.onNodeWithTag(V5FirstRunTestTags.NotNow).performClick()
        assertEquals(listOf("make default", "not now"), calls)
    }

    @Test
    fun theGestureCardsDoneIsReachedByScrolling() {
        composeRule.setContent { VolaV5Theme { V5FirstRunGestures(onDone = { calls += "done" }) } }

        composeRule.onNodeWithTag(V5FirstRunTestTags.GesturesDone).performScrollTo().performClick()
        assertEquals(listOf("done"), calls)
    }
}
