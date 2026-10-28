package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserscriptManagementScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editorStartsFromTemplateAndDisplaysValidationFailure() {
        composeRule.setContent {
            MaterialBrowserTheme {
                UserscriptManagementScreen(
                    scripts = emptyList(),
                    onToggle = { _, _, onResult -> onResult(null) },
                    onSave = { _, _, onResult -> onResult("Missing @match") },
                    onDelete = { _, onResult -> onResult(null) },
                    onImport = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag(UserscriptManagementTestTags.Add).performClick()
        composeRule.onNodeWithTag(UserscriptManagementTestTags.Editor).assertExists()
        composeRule.onNodeWithTag(UserscriptManagementTestTags.EditorSource)
            .assertTextContains("@match", substring = true)
            .performTextClearance()
        composeRule.onNodeWithTag(UserscriptManagementTestTags.EditorSource)
            .performTextInput("invalid source")
        composeRule.onNodeWithTag(UserscriptManagementTestTags.EditorSave).performClick()
        composeRule.onNodeWithText("Missing @match").assertExists()
    }

    private fun testScript() = UserscriptUiItem(
        id = "reader-theme",
        name = "Reader theme",
        source = "// source",
        enabled = true,
        runAtLabel = "document-end",
        urlPatterns = listOf("https://example.com/*"),
    )
}
