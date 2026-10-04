package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.shared.settings.SettingSearchCandidate
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSearchPageInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun findsASettingByItsWordsAndOpensItsPage() {
        val opened = mutableListOf<SettingSearchCandidate>()
        composeRule.setContent {
            MaterialBrowserTheme {
                SettingsSearchPage(onOpen = opened::add, onBack = {})
            }
        }

        composeRule.onNodeWithTag(SettingsSearchTestTags.Field).performTextInput("https")
        composeRule.onNodeWithTag(SettingsSearchTestTags.result("https_only"))
            .assertIsDisplayed()
            .performClick()

        assertEquals(SettingsDestination.ProtectionAndData, opened.single().destination)
    }

    @Test
    fun saysWhenNothingIsFound() {
        composeRule.setContent {
            MaterialBrowserTheme {
                SettingsSearchPage(onOpen = {}, onBack = {})
            }
        }

        composeRule.onNodeWithTag(SettingsSearchTestTags.Field).performTextInput("zzqx")
        composeRule.onNodeWithTag(SettingsSearchTestTags.Empty).assertIsDisplayed()
    }
}
