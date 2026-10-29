package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.HttpAuthPrompt
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.v5.V5SheetTestTags
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HttpAuthPromptDialogInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun submitsEnteredCredentialsForNamedChallenge() {
        val submitted = AtomicReference<Pair<String, String>?>()
        composeRule.setContent {
            MaterialBrowserTheme {
                HttpAuthPromptDialog(
                    prompt = HttpAuthPrompt(
                        id = 1L,
                        tabId = "tab-a",
                        host = "auth.example.com",
                        realm = "Members",
                        isPageSecure = true,
                    ),
                    onSubmit = { username, password -> submitted.set(username to password) },
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithTag(HttpAuthPromptTestTags.Dialog).assertExists()
        // The v5 prompt sheet says the host and the realm in one message.
        composeRule.onNodeWithText(
            context.getString(R.string.http_auth_dialog_message, "auth.example.com"),
            substring = true,
        ).assertExists()
        composeRule.onNodeWithText(
            context.getString(R.string.http_auth_dialog_realm, "Members"),
            substring = true,
        ).assertExists()
        composeRule.onNodeWithTag(HttpAuthPromptTestTags.Username).performTextInput("candy")
        composeRule.onNodeWithTag(HttpAuthPromptTestTags.Password).performTextInput("secret")
        composeRule.onNodeWithTag(HttpAuthPromptTestTags.Submit).performClick()

        assertEquals("candy" to "secret", submitted.get())
    }

    @Test
    fun warnsForHttpPageAndCancels() {
        val canceled = AtomicBoolean()
        composeRule.setContent {
            MaterialBrowserTheme {
                HttpAuthPromptDialog(
                    prompt = HttpAuthPrompt(
                        id = 2L,
                        tabId = "tab-a",
                        host = "192.168.1.10",
                        realm = null,
                        isPageSecure = false,
                    ),
                    onSubmit = { _, _ -> },
                    onCancel = { canceled.set(true) },
                )
            }
        }

        // The warning closes the sheet's message; «Close» cancels.
        composeRule.onNodeWithText(
            context.getString(R.string.http_auth_dialog_insecure_warning),
            substring = true,
        ).assertExists()
        composeRule.onNodeWithTag(V5SheetTestTags.Close).performClick()

        assertTrue(canceled.get())
    }
}
