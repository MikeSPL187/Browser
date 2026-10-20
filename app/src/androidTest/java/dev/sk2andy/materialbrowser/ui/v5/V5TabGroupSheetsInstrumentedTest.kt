package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 tab group sheets on the device (TG-04): moving a tab, the group limit said before a name is
 * typed (TG-01), and a group's name and color, with a blank name keeping the old one.
 */
@RunWith(AndroidJUnit4::class)
class V5TabGroupSheetsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val groups = listOf(
        V5OverviewGroup(id = "work", name = "Work", tabIds = listOf("t1"), collapsed = false, tile = V5Tile.Blue),
        V5OverviewGroup(id = "trip", name = "Trip", tabIds = emptyList(), collapsed = false, tile = V5Tile.Green),
    )

    private fun showMove(currentGroupId: String?, groupLimit: Int? = null) {
        composeRule.setContent {
            VolaV5Theme {
                V5TabGroupMoveSheet(
                    visible = true,
                    tabTitle = "Northern lights",
                    groups = groups,
                    currentGroupId = currentGroupId,
                    onMove = { calls += "move $it" },
                    onNewGroup = { calls += "new group" },
                    onRemoveFromGroup = { calls += "remove" },
                    onDismiss = { calls += "dismiss" },
                    groupLimit = groupLimit,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun showEdit(onUngroup: (() -> Unit)?) {
        composeRule.setContent {
            VolaV5Theme {
                V5TabGroupEditSheet(
                    visible = true,
                    title = "Edit group",
                    initialName = "Work",
                    initialColorIndex = 0,
                    colors = listOf(V5Tile.Blue, V5Tile.Green, V5Tile.Red),
                    onSave = { name, color -> calls += "save $name $color" },
                    onDismiss = { calls += "dismiss" },
                    onUngroup = onUngroup,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun aTabMovesToAnotherGroupButNotToItsOwn() {
        showMove(currentGroupId = "work")

        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.moveTo("work")).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(R.string.v5_group_current)).assertExists()
        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.moveTo("trip")).performClick()

        assertEquals(listOf("dismiss", "move trip"), calls)
    }

    @Test
    fun aGroupedTabCanLeaveItsGroup() {
        showMove(currentGroupId = "work")

        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.RemoveFromGroup).performClick()
        assertEquals(listOf("dismiss", "remove"), calls)
    }

    @Test
    fun theGroupLimitShowsBeforeANameIsTyped() {
        showMove(currentGroupId = null, groupLimit = 2)

        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.NewGroup).assertIsNotEnabled()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.v5_group_limit, 2, 2)).assertExists()
        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.RemoveFromGroup).assertDoesNotExist()
    }

    @Test
    fun aGroupGetsANewNameAndColor() {
        showEdit(onUngroup = null)

        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.NameField).performTextClearance()
        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.NameField).performTextInput("Home")
        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.color(2)).performClick()
        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.Save).performClick()

        assertEquals(listOf("dismiss", "save Home 2"), calls)
        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.Ungroup).assertDoesNotExist()
    }

    @Test
    fun aBlankNameKeepsTheOldOneAndAGroupCanBeUngrouped() {
        showEdit(onUngroup = { calls += "ungroup" })

        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.NameField).performTextClearance()
        composeRule.onNodeWithTag(V5TabGroupSheetTestTags.Save).performClick()
        assertEquals(listOf("dismiss", "save Work 0"), calls)
    }
}
