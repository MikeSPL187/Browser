package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The data transfer window under v5: what is happening with a spinner, then a failed recovery without one. */
@RunWith(AndroidJUnit4::class)
class AppDataTransferStatusV5InstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theWorkShowsWithASpinnerAndAFailedRecoveryWithout() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var state by mutableStateOf(AppDataTransferStatusState(R.string.data_archive_export_progress, export = true))
        composeRule.setContent { AppDataTransferStatusV5(state) }

        composeRule.onNodeWithText(context.getString(R.string.data_archive_export_progress)).assertExists()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()

        state = state.copy(message = R.string.data_archive_recovery_failed, failed = true)
        composeRule.onNodeWithText(context.getString(R.string.data_archive_recovery_failed)).assertExists()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }
}
