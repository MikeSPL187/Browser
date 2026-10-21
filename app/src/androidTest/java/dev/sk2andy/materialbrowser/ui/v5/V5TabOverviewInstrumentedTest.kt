package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 tab overview on the device (audit 20.10, О-2…О-5): «+» only where the segment can have a
 * tab, an empty grid that says why with its one action, «Close all» off when only pinned tabs are in
 * front, each «×» naming its tab, and a pinned card that refuses a swipe out loud.
 */
@RunWith(AndroidJUnit4::class)
class V5TabOverviewInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private fun card(id: String, title: String, closable: Boolean = true) =
        V5TabCard(id = id, title = title, selected = false, preview = null, favicon = null, closable = closable)

    private fun show(cards: List<V5TabCard>, privateSegment: Boolean, canOpenPrivate: Boolean = true, query: String = "") {
        composeRule.setContent {
            VolaV5Theme {
                V5TabOverview(
                    state = V5OverviewState(
                        cards = cards,
                        privateSegment = privateSegment,
                        counts = V5OverviewCounts(regular = if (privateSegment) 2 else cards.size, private = if (privateSegment) cards.size else 0),
                        query = query,
                        profileLabel = "Personal",
                        canOpenPrivate = canOpenPrivate,
                    ),
                    onQueryChange = {},
                    onSegmentChange = {},
                    onSelect = { calls += "select $it" },
                    onClose = { calls += "close $it" },
                    onNewTab = { calls += "new tab" },
                    onDone = { calls += "done" },
                    onCloseAll = { calls += "close all" },
                    onCloseRefused = { calls += "refused" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun privateSegmentWithoutPrivateTabsHasNoPlusAndSaysWhy() {
        show(emptyList(), privateSegment = true, canOpenPrivate = false)

        composeRule.onNodeWithTag(V5OverviewTestTags.NewTab).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.v5_overview_private_unavailable)).assertExists()
        composeRule.onNodeWithTag(V5OverviewTestTags.EmptyNewTab).assertDoesNotExist()
    }

    @Test
    fun emptyPrivateSegmentOffersANewPrivateTab() {
        show(emptyList(), privateSegment = true)

        composeRule.onNodeWithText(context.getString(R.string.v5_overview_private_empty_title)).assertExists()
        composeRule.onNodeWithTag(V5OverviewTestTags.EmptyNewTab).performClick()

        assertEquals(listOf("new tab"), calls)
    }

    @Test
    fun aSearchWithoutMatchesNamesTheQuery() {
        show(emptyList(), privateSegment = false, query = "aurora")

        composeRule.onNodeWithText(context.getString(R.string.v5_overview_no_matches, "aurora")).assertExists()
    }

    @Test
    fun closeAllIsOffWithOnlyPinnedTabsInFront() {
        show(listOf(card("pinned", "Mail", closable = false)), privateSegment = false)

        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_more_options)).performClick()
        composeRule.onNodeWithTag(V5OverviewTestTags.CloseAll).assertIsNotEnabled()
    }

    @Test
    fun eachCloseButtonNamesItsTab() {
        show(listOf(card("a", "Northern lights"), card("b", "Recipes")), privateSegment = false)

        composeRule.onNodeWithContentDescription(context.getString(R.string.v5_overview_close_named, "Recipes")).performClick()

        assertEquals(listOf("close b"), calls)
    }

    @Test
    fun aSwipedPinnedCardStaysAndSaysWhy() {
        show(listOf(card("pinned", "Mail", closable = false)), privateSegment = false)

        composeRule.onNodeWithTag(V5OverviewTestTags.card("pinned")).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(listOf("refused"), calls)
    }
}
