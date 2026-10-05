package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginAccount
import dev.sk2andy.materialbrowser.browser.credentials.VaultLoginRequest
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultLoginSheetsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun signInSheetOffersTheAccountsAndAnswersWithThePickedOne() {
        val picked = mutableListOf<String>()
        var dismissed = 0
        composeRule.setContent {
            MaterialBrowserTheme {
                VaultSelectSheetContent(
                    request = VaultLoginRequest.Select(
                        id = 1,
                        windowId = 0,
                        site = "mail.example.com",
                        accounts = listOf(
                            VaultLoginAccount("anna@example.com", null),
                            VaultLoginAccount("anna.work@example.com", null),
                        ),
                    ),
                    onPick = { picked += it },
                    onDismiss = { dismissed++ },
                )
            }
        }

        composeRule.onNodeWithText("anna@example.com").assertIsDisplayed()
        composeRule.onNodeWithTag(VaultLoginSheetTestTags.account(1)).performClick()
        composeRule.onNodeWithTag(VaultLoginSheetTestTags.SelectDismiss).performClick()
        assertEquals(listOf("anna.work@example.com"), picked)
        assertEquals(1, dismissed)
    }

    @Test
    fun saveSheetSavesOrWaits() {
        var saved = 0
        var dismissed = 0
        composeRule.setContent {
            MaterialBrowserTheme {
                VaultSaveSheetContent(
                    request = VaultLoginRequest.Save(1, 0, "mail.example.com", "anna@example.com", update = true),
                    onSave = { saved++ },
                    onDismiss = { dismissed++ },
                )
            }
        }

        composeRule.onNodeWithTag(VaultLoginSheetTestTags.SaveConfirm).performClick()
        composeRule.onNodeWithTag(VaultLoginSheetTestTags.SaveDismiss).performClick()
        assertEquals(1, saved)
        assertEquals(1, dismissed)
    }
}
