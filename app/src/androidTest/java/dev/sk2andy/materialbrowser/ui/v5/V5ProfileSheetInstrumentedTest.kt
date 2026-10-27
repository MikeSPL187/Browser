package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The v5 profiles sheet (canvas «Profiles»): «Edit» turns a touch on a profile into editing it. */
@RunWith(AndroidJUnit4::class)
class V5ProfileSheetInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val profiles = listOf(
        V5ProfileItem("personal", VolaIcons.Home, "Personal", V5Accent.Blue, tabCount = 3, active = true),
        V5ProfileItem("work", VolaIcons.Workspaces, "Work", V5Accent.Orange, tabCount = 5, active = false),
        V5ProfileItem("bank", VolaIcons.Lock, "Bank", V5Accent.Green, tabCount = 1, active = false, locked = true),
    )

    private fun show() {
        composeRule.setContent {
            VolaV5Theme {
                V5ProfileSheet(
                    visible = true,
                    profiles = profiles,
                    onSelect = { calls += "select $it" },
                    onDismiss = { calls += "dismiss" },
                    onNew = { calls += "new" },
                    onEdit = { calls += "edit $it" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun aTouchChoosesAProfileUntilEditStarts() {
        show()

        composeRule.onNodeWithText(context.getString(R.string.v5_profiles_explainer)).assertExists()
        composeRule.onNodeWithTag(V5ProfileSheetTestTags.profile("work")).performClick()
        composeRule.onNodeWithTag(V5ProfileSheetTestTags.Edit).performClick()
        composeRule.onNodeWithText(context.getString(R.string.action_done)).assertExists()
        composeRule.onNodeWithTag(V5ProfileSheetTestTags.profile("work")).performClick()
        // A locked profile is still unlocked first, not edited.
        composeRule.onNodeWithTag(V5ProfileSheetTestTags.profile("bank")).performClick()

        assertEquals(listOf("select work", "edit work", "select bank"), calls)
    }
}
