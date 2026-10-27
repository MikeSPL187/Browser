package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.ui.test.assertIsDisplayed
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

/** The password pages under v5 (canvas «ц4»–«ц7») answer as the current ones do: same callbacks, same tags. */
@RunWith(AndroidJUnit4::class)
class PasswordsV5InstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mail = login("1", "https://mail.example.com", "anna@example.com")
    private val bank = login("2", "https://bank.example.ru", "anna.k")

    @Test
    fun theListSearchesAndOpensALogin() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordsListV5(
                    logins = listOf(bank, mail),
                    onOpen = { opened += it.id },
                    onAdd = {},
                    onLock = {},
                    onBack = {},
                    systemFillNote = false,
                    passkeyNote = null,
                    health = PasswordsHealthCounts(weak = 0, reused = 0, breached = 0, issues = 0),
                    onHealth = {},
                    onImport = {},
                    busy = false,
                    onTurnOff = {},
                )
            }
        }

        composeRule.onNodeWithText("mail.example.com").assertIsDisplayed()
        composeRule.onNodeWithTag(PasswordsTestTags.Search).performTextInput("bank")
        composeRule.onNodeWithText("mail.example.com").assertDoesNotExist()
        composeRule.onNodeWithTag(PasswordsTestTags.login("2")).performClick()
        assertEquals(listOf("2"), opened)
    }

    @Test
    fun theLoginHidesThePasswordUntilAskedAndDeletingAsksFirst() {
        var deleted = 0
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordDetailV5(
                    login = mail,
                    onCopyUsername = {},
                    onCopyPassword = {},
                    onEdit = {},
                    onDelete = { deleted++ },
                    onBack = {},
                    onCopyCode = {},
                    onSetTotp = { false },
                    onRemoveTotp = {},
                )
            }
        }

        composeRule.onNodeWithText("correct horse").assertDoesNotExist()
        composeRule.onNodeWithTag(PasswordsTestTags.Reveal).performClick()
        composeRule.onNodeWithText("correct horse").assertExists()
        composeRule.onNodeWithTag(PasswordsTestTags.Reveal).performClick()
        composeRule.onNodeWithText("correct horse").assertDoesNotExist()

        composeRule.onNodeWithTag(PasswordsTestTags.Delete).performClick()
        assertEquals(0, deleted)
        composeRule.onNodeWithText(context.getString(R.string.passwords_delete_action)).performClick()
        composeRule.runOnIdle { assertEquals(1, deleted) }
    }

    @Test
    fun confirmingThePhraseHandsBackTheTypedWords() {
        val checked = mutableListOf<List<String>>()
        composeRule.setContent {
            MaterialBrowserTheme {
                RecoveryConfirmV5(positions = listOf(0, 5, 11), wrong = false, busy = false, onCheck = { checked += it }, onBack = {})
            }
        }

        composeRule.onNodeWithTag(PasswordsTestTags.confirmWord(0)).performTextInput("acid")
        composeRule.onNodeWithTag(PasswordsTestTags.confirmWord(5)).performTextInput("affix")
        composeRule.onNodeWithTag(PasswordsTestTags.confirmWord(11)).performTextInput("ahead")
        composeRule.onNodeWithTag(PasswordsTestTags.ConfirmCheck).performClick()
        assertEquals(listOf(listOf("acid", "affix", "ahead")), checked)
    }

    @Test
    fun thePhraseOpensTheVault() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            MaterialBrowserTheme {
                RecoveryEntryV5(wrong = false, busy = false, onOpen = { opened += it }, onBack = {})
            }
        }

        composeRule.onNodeWithTag(PasswordsTestTags.RecoverField).performTextInput("acid acorn")
        composeRule.onNodeWithTag(PasswordsTestTags.RecoverOpen).performClick()
        assertEquals(listOf("acid acorn"), opened)
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
