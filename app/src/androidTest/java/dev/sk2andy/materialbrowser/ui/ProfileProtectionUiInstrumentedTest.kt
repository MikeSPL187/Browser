package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileProtectionUiInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun lockedOverlayHidesProfileBehindUnlockAction() {
        composeRule.setContent {
            MaterialBrowserTheme {
                ProfileLockedOverlay(
                    workspace = ProfileLockedPreviewWorkspace,
                    unlockAvailable = true,
                    canSwitchProfile = false,
                    onUnlock = {},
                    onSwitchProfile = {},
                )
            }
        }

        composeRule.onNodeWithTag(ProfileProtectionTestTags.LockedOverlay).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(
                R.string.profile_locked_title_named,
                ProfileLockedPreviewWorkspace.name,
            ),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.profile_unlock_action))
            .assertIsDisplayed()
    }

    @Test
    fun protectedExportWarnsThatZipIsReadable() {
        composeRule.setContent {
            MaterialTheme {
                AppDataExportWarningDialog(
                    hasProtectedProfiles = true,
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.data_archive_export_protected_warning_message),
        ).assertIsDisplayed()
    }
}
