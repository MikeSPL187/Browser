package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Bringing passwords in under v5: the same answers as the current pages, through the same tags. */
@RunWith(AndroidJUnit4::class)
class PasswordImportV5InstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aSourceOrAFileIsPicked() {
        val picked = mutableListOf<String>()
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordImportV5(onSource = { picked += it.name }, onPickFile = { picked += "file" }, onBack = {})
            }
        }

        composeRule.onNodeWithTag(PasswordImportTestTags.source(PasswordImportSource.Firefox)).performClick()
        composeRule.onNodeWithTag(PasswordImportTestTags.PickFile).performScrollTo().performClick()
        assertEquals(listOf("Firefox", "file"), picked)
    }

    @Test
    fun theGuideSaysWhatWentWrongAndPicksAgain() {
        var picks = 0
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordImportGuideV5(
                    source = PasswordImportSource.Chrome,
                    problem = PasswordImportProblem.Encrypted,
                    busy = false,
                    onPick = { picks++ },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(PasswordImportTestTags.Problem).assertExists()
        composeRule.onNodeWithTag(PasswordImportTestTags.PickFile).performScrollTo().performClick()
        assertEquals(1, picks)
    }

    @Test
    fun theResultDeletesTheFileAndGoesToThePasswords() {
        val done = mutableListOf<String>()
        composeRule.setContent {
            MaterialBrowserTheme {
                PasswordImportResultV5(
                    report = PasswordImportReport(
                        fileName = "passwords.csv",
                        fileSizeBytes = 2048,
                        added = 3,
                        duplicates = 0,
                        skipped = 0,
                        withTotp = 0,
                        addedIds = listOf("1", "2", "3"),
                    ),
                    fileState = PasswordImportFileState.Present,
                    healthIssues = 0,
                    onDeleteFile = { done += "delete" },
                    onCheck = { done += "check" },
                    onDone = { done += "done" },
                    conflictState = PasswordImportConflictState.Offered,
                    onUseFilePasswords = {},
                )
            }
        }

        composeRule.onNodeWithTag(PasswordImportTestTags.DeleteFile).performScrollTo().performClick()
        composeRule.onNodeWithTag(PasswordImportTestTags.Check).performScrollTo().performClick()
        composeRule.onNodeWithTag(PasswordImportTestTags.ToPasswords).performScrollTo().performClick()
        assertEquals(listOf("delete", "check", "done"), done)
    }
}
