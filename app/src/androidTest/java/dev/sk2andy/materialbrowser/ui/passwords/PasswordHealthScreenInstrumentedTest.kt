package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordHealthScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theCheckListsProblemsAndAsksToChangeThem() {
        val logins = listOf(login("1", "https://forum.example.org", "Tr0ub4dor&3-horse!"), login("2", "https://cinema.example.ru", "sunshine"))
        val changed = mutableListOf<String>()
        val toggles = mutableListOf<Boolean>()
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordHealthScreen(
                    report = PasswordHealthRules.report(logins, mapOf("1" to 7)),
                    leakCheck = LeakCheckStatus.Done,
                    onLeakCheckChange = { toggles += it },
                    onOpen = {},
                    onChange = { changed += it.id },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("forum.example.org").assertIsDisplayed()
        composeRule.onNodeWithTag(PasswordHealthTestTags.change("2")).performScrollTo().performClick()
        composeRule.onNodeWithTag(PasswordHealthTestTags.LeakCheck).performScrollTo().performClick()
        assertEquals(listOf("2"), changed)
        assertEquals(listOf(false), toggles)
    }

    private fun login(id: String, origin: String, password: String) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = null,
        username = "anna",
        password = password,
        createdAtMillis = 0,
        updatedAtMillis = 0,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )
}
