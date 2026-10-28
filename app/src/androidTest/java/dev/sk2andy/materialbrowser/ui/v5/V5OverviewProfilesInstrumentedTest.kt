package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Paging through profiles in the v5 overview: a swipe on the bar, or the chip's TalkBack actions. */
@RunWith(AndroidJUnit4::class)
class V5OverviewProfilesInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val steps = mutableListOf<Int>()

    private fun show(active: Int) {
        composeRule.setContent {
            VolaV5Theme(dark = false) {
                V5TabOverview(
                    state = V5OverviewState(
                        cards = listOf(V5TabCard("t", "Tab", selected = true, preview = null, favicon = null)),
                        privateSegment = false,
                        counts = V5OverviewCounts(regular = 1, private = 0),
                        query = "",
                        profileLabel = "Personal",
                        profileDots = V5ProfileDots(count = 3, active = active),
                    ),
                    onQueryChange = {},
                    onSegmentChange = {},
                    onSelect = {},
                    onClose = {},
                    onNewTab = {},
                    onDone = {},
                    onCloseAll = {},
                    onProfiles = {},
                    onProfileStep = { steps += it },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun customActionLabels(): List<String> =
        composeRule.onNodeWithTag(V5OverviewTestTags.Profile).fetchSemanticsNode()
            .config.getOrElse(SemanticsActions.CustomActions) { emptyList() }
            .map { it.label }

    @Test
    fun aSwipeOnTheBarTurnsToTheNextProfile() {
        show(active = 0)

        // From the chip far past its own edge, quickly: a sideways swipe on the bar.
        composeRule.onNodeWithTag(V5OverviewTestTags.Profile).performTouchInput {
            swipe(start = center, end = Offset(center.x - width * 3, center.y), durationMillis = 120)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(1), steps)
    }

    @Test
    fun theFirstProfileOffersOnlyTheNextOneToTalkBack() {
        show(active = 0)
        assertEquals(listOf(context.getString(R.string.v5_overview_next_profile)), customActionLabels())
    }

    @Test
    fun aMiddleProfileOffersBothWaysToTalkBack() {
        show(active = 1)
        assertEquals(
            listOf(
                context.getString(R.string.v5_overview_next_profile),
                context.getString(R.string.v5_overview_previous_profile),
            ),
            customActionLabels(),
        )
    }

    @Test
    fun theLastProfileDoesNotTurnFurther() {
        show(active = 2)

        composeRule.onNodeWithTag(V5OverviewTestTags.Profile).performTouchInput {
            swipe(start = center, end = Offset(center.x - width * 3, center.y), durationMillis = 120)
        }
        composeRule.waitForIdle()

        assertEquals(emptyList<Int>(), steps)
    }
}
