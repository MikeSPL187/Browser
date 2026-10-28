package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** System-v5.md 8: a card says where it is in the grid, its title and its group. */
@RunWith(AndroidJUnit4::class)
class V5TabCardAccessibilityInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aCardSaysItsPlaceTitleAndGroup() {
        composeRule.setContent {
            VolaV5Theme(dark = false) {
                V5TabOverview(
                    state = V5OverviewState(
                        cards = listOf(
                            V5TabCard("soup", "Soup", selected = true, preview = null, favicon = null),
                            V5TabCard("news", "News", selected = false, preview = null, favicon = null),
                        ),
                        privateSegment = false,
                        counts = V5OverviewCounts(regular = 2, private = 0),
                        query = "",
                        profileLabel = "Personal",
                        groups = listOf(
                            V5OverviewGroup("dinner", "Dinner", tabIds = listOf("soup"), collapsed = false, tile = V5Tile.Orange),
                        ),
                    ),
                    onQueryChange = {},
                    onSegmentChange = {},
                    onSelect = {},
                    onClose = {},
                    onNewTab = {},
                    onDone = {},
                    onCloseAll = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(V5OverviewTestTags.card("soup"))
            .assertContentDescriptionEquals(context.getString(R.string.v5_overview_card_in_group, 1, 2, "Soup", "Dinner"))
            .assertIsSelected()
        composeRule.onNodeWithTag(V5OverviewTestTags.card("news"))
            .assertContentDescriptionEquals(context.getString(R.string.v5_overview_card, 2, 2, "News"))
    }
}
