package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 profile sheet on the device (PF-06): a new profile is made by «Create» with its name, symbol
 * and accent; an existing one keeps a blank name as it was, and its switches and «Delete» answer.
 */
@RunWith(AndroidJUnit4::class)
class V5ProfileEditSheetInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val icons = listOf(
        V5ProfileIconChoice("person", VolaIcons.Star, "Person"),
        V5ProfileIconChoice("work", VolaIcons.Workspaces, "Work"),
    )

    private fun show(creating: Boolean, onDelete: (() -> Unit)? = null) {
        composeRule.setContent {
            VolaV5Theme {
                V5ProfileEditSheet(
                    visible = true,
                    creating = creating,
                    initial = V5ProfileDraft(name = "Personal", iconKey = "person", accent = V5Accent.entries.first()),
                    icons = icons,
                    privacy = V5ProfilePrivacy(ownStorage = true, biometricLock = false),
                    onSave = { draft -> calls += "save ${draft.name} ${draft.iconKey} ${draft.accent.ordinal}" },
                    onOwnStorageChange = { calls += "storage $it" },
                    onBiometricLockChange = { calls += "lock $it" },
                    onDismiss = { calls += "dismiss" },
                    onDelete = onDelete,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun aNewProfileIsMadeWithItsNameSymbolAndAccent() {
        show(creating = true)

        composeRule.onNodeWithTag(V5ProfileEditTestTags.Name).performTextClearance()
        composeRule.onNodeWithTag(V5ProfileEditTestTags.Name).performTextInput("Work")
        composeRule.onNodeWithTag(V5ProfileEditTestTags.icon(1)).performClick()
        composeRule.onNodeWithTag(V5ProfileEditTestTags.color(1)).performClick()
        composeRule.onNodeWithTag(V5ProfileEditTestTags.Save).performClick()

        assertEquals(listOf("dismiss", "save Work work 1"), calls)
        composeRule.onNodeWithTag(V5ProfileEditTestTags.Delete).assertDoesNotExist()
    }

    @Test
    fun aBlankNameKeepsTheOldOneAndTheSwitchesAndDeleteAnswer() {
        show(creating = false, onDelete = { calls += "delete" })

        composeRule.onNodeWithTag(V5ProfileEditTestTags.Isolation).performClick()
        composeRule.onNodeWithTag(V5ProfileEditTestTags.Protection).performClick()
        composeRule.onNodeWithTag(V5ProfileEditTestTags.Delete).performClick()
        composeRule.onNodeWithTag(V5ProfileEditTestTags.Name).performTextClearance()
        composeRule.onNodeWithTag(V5ProfileEditTestTags.Save).performClick()

        assertEquals(listOf("storage false", "lock true", "delete", "dismiss", "save Personal person 0"), calls)
    }

    @Test
    fun closingAnExistingProfileAfterAChangeKeepsItAsItWas() {
        show(creating = false)

        composeRule.onNodeWithTag(V5ProfileEditTestTags.Name).performTextClearance()
        composeRule.onNodeWithTag(V5ProfileEditTestTags.Name).performTextInput("Work")
        composeRule.onNodeWithTag(V5ProfileEditTestTags.color(2)).performClick()
        composeRule.onNodeWithTag(V5EditHeaderTestTags.Cancel).performClick()

        // «Close» is the way out of a change (ПР-1): only «Done» saves.
        assertEquals(listOf("dismiss"), calls)
    }
}
