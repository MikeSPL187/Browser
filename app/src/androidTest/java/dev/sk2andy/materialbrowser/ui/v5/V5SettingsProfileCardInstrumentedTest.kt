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

/** Canvas «Settings»: the profile card names the profile, tells its sync and opens the profiles. */
@RunWith(AndroidJUnit4::class)
class V5SettingsProfileCardInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(sync: V5ProfileSync, onClick: () -> Unit = {}) {
        composeRule.setContent {
            VolaV5Theme(dark = false) {
                V5SettingsProfileCard(V5SettingsProfile("Personal", icon = null, sync = sync), onClick = onClick)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theCardNamesTheProfileAndItsLastSync() {
        var taps = 0
        show(V5ProfileSync.Synced("2 min ago")) { taps++ }

        composeRule.onNodeWithText(context.getString(R.string.v5_settings_profile_title, "Personal"), substring = true).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.v5_settings_profile_synced, "2 min ago"), substring = true).assertExists()
        composeRule.onNodeWithTag(V5SettingsProfileCardTestTags.Card).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun aBuildWithoutTheOwnServerSaysNothingAboutSync() {
        show(V5ProfileSync.Unavailable)

        composeRule.onNodeWithText(context.getString(R.string.v5_settings_profile_sync_off), substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.v5_webdav_never_synced), substring = true).assertDoesNotExist()
    }
}
