package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.ProfileProtection
import dev.sk2andy.materialbrowser.sync.SyncDeviceIconCatalog
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileCreationSheetInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val profileEmojis by lazy {
        context.assets.open("candy_sync_device_icons_v1.json").use { input ->
            SyncDeviceIconCatalog.decode(input.bufferedReader().readText()).icons.map { it.emoji }
        }
    }

    @Test
    fun createsWorkspaceWithChosenIconStorageAndLock() {
        val created = AtomicReference<Submission?>()
        setSheet { emoji, isolationEnabled, options ->
            created.set(Submission(emoji, isolationEnabled, options.protection))
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.workspace_icon_work))
            .performClick()
        composeRule.onNodeWithTag(ProfileCreationTestTags.Isolation).performScrollTo().performClick()
        composeRule.onNodeWithTag(ProfileCreationOptionTestTags.Protection)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.action_save)).performClick()
        composeRule.onNodeWithTag(ProfileCreationTestTags.CreateButton)
            .assertIsEnabled()
            .performClick()

        val submission = requireNotNull(created.get())
        assertEquals("💼", submission.emoji)
        assertTrue(submission.isolationEnabled)
        assertTrue(submission.protection != null)
    }

    @Test
    fun aNewWorkspaceStartsWithAnIconChosen() {
        val created = AtomicReference<String?>()
        setSheet { emoji, _, _ -> created.set(emoji) }

        composeRule.onNodeWithTag(ProfileCreationTestTags.CreateButton)
            .assertIsEnabled()
            .performClick()

        assertEquals(profileEmojis.first(), created.get())
    }

    @Test
    fun theCreateButtonStaysPutWhileTheOptionsScroll() {
        setSheet { _, _, _ -> }
        val button = composeRule.onNodeWithTag(ProfileCreationTestTags.CreateButton)
        val before = button.fetchSemanticsNode().boundsInRoot
        val scroll = composeRule.onNodeWithTag(ProfileCreationTestTags.IconScroll)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(scroll.bottom <= before.top)
        composeRule.onNodeWithTag(ProfileCreationOptionTestTags.Protection).performScrollTo()
        composeRule.waitForIdle()

        val after = button.fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, 0.5f)
        assertEquals(before.bottom, after.bottom, 0.5f)
    }

    private fun setSheet(onCreate: (String, Boolean, ProfileCreationOptions) -> Unit) {
        composeRule.setContent {
            MaterialBrowserTheme {
                NewWorkspaceSheet(
                    visible = true,
                    isolationSupported = true,
                    profileProtectionSupported = true,
                    icons = profileEmojis,
                    onCreate = onCreate,
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private data class Submission(
        val emoji: String,
        val isolationEnabled: Boolean,
        val protection: ProfileProtection?,
    )
}
