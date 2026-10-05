package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordsScreensInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mail = login("1", "https://mail.example.com", "anna@example.com")
    private val bank = login("2", "https://bank.example.ru", "anna.k")

    @Test
    fun listSearchesBySiteOrLoginAndOpensALogin() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordsListScreen(logins = listOf(bank, mail), onOpen = { opened += it.id }, onAdd = {}, onLock = {}, onBack = {})
            }
        }

        composeRule.onNodeWithText("mail.example.com").assertIsDisplayed()
        composeRule.onNodeWithTag(PasswordsTestTags.Search).performTextInput("bank")
        composeRule.onNodeWithText("mail.example.com").assertDoesNotExist()
        composeRule.onNodeWithTag(PasswordsTestTags.login("2")).performClick()
        assertEquals(listOf("2"), opened)
    }

    @Test
    fun detailHidesThePasswordUntilAsked() {
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordDetailScreen(
                    login = mail,
                    onCopyUsername = {},
                    onCopyPassword = {},
                    onEdit = {},
                    onDelete = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("correct horse").assertDoesNotExist()
        composeRule.onNodeWithTag(PasswordsTestTags.Reveal).performClick()
        composeRule.onNodeWithTag(PasswordsTestTags.PasswordValue).assertTextEquals("correct horse")
        composeRule.onNodeWithTag(PasswordsTestTags.Reveal).performClick()
        composeRule.onNodeWithText("correct horse").assertDoesNotExist()
    }

    @Test
    fun deletingAsksFirst() {
        var deleted = 0
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordDetailScreen(
                    login = mail,
                    onCopyUsername = {},
                    onCopyPassword = {},
                    onEdit = {},
                    onDelete = { deleted++ },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(PasswordsTestTags.Delete).performClick()
        assertEquals(0, deleted)
        composeRule.onNodeWithText(context.getString(R.string.passwords_delete_action)).performClick()
        assertEquals(1, deleted)
    }

    @Test
    fun confirmingThePhraseHandsBackTheTypedWords() {
        val checked = mutableListOf<List<String>>()
        composeRule.setContent {
            MaterialBrowserTheme {
                RecoveryConfirmScreen(positions = listOf(0, 5, 11), wrong = false, busy = false, onCheck = { checked += it }, onBack = {})
            }
        }

        composeRule.onNodeWithTag(PasswordsTestTags.confirmWord(0)).performTextInput("acid")
        composeRule.onNodeWithTag(PasswordsTestTags.confirmWord(5)).performTextInput("affix")
        composeRule.onNodeWithTag(PasswordsTestTags.confirmWord(11)).performTextInput("ahead")
        composeRule.onNodeWithTag(PasswordsTestTags.ConfirmCheck).performClick()
        assertEquals(listOf(listOf("acid", "affix", "ahead")), checked)
    }

    private fun login(id: String, origin: String, username: String) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = null,
        username = username,
        password = "correct horse",
        createdAtMillis = 0,
        updatedAtMillis = 0,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )
}
