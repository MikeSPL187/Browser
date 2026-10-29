package dev.sk2andy.materialbrowser.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.data.SnoozedTab
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnoozeScreensInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var controller: BrowserController? = null

    @After
    fun tearDown() {
        composeRule.mainClock.autoAdvance = true
        composeRule.runOnIdle {
            controller?.destroy()
            controller = null
        }
    }

    @Test
    fun regularTabPresetSubmitsFutureTimeExactlyOnce() {
        val submitted = AtomicLong(0L)
        val calls = AtomicInteger()
        composeRule.setContent {
            MaterialBrowserTheme {
                SnoozeTabDialog(
                    tab = BrowserTab("tab", 1L, title = "Example"),
                    onSnooze = { wakeAt ->
                        calls.incrementAndGet()
                        submitted.set(wakeAt)
                        true
                    },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithTag(SnoozeTestTags.PresetGroup).assertIsDisplayed()
        composeRule.onNodeWithTag(SnoozeTestTags.Tomorrow)
            .assertIsEnabled()
            .performClick()

        assertEquals(1, calls.get())
        assertTrue(submitted.get() > System.currentTimeMillis())
    }

    @Test
    fun privateTabShowsPersistenceBoundaryAndDisablesChoices() {
        composeRule.setContent {
            MaterialBrowserTheme {
                SnoozeTabDialog(
                    tab = BrowserTab("private", 1L, isIncognito = true),
                    onSnooze = { false },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.snooze_unavailable_private))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SnoozeTestTags.LaterToday).assertIsNotEnabled()
        composeRule.onNodeWithTag(SnoozeTestTags.Custom).assertIsNotEnabled()
    }

    @Test
    fun managementExposesOpenDeleteAndProfileMetadata() {
        val openCalls = AtomicInteger()
        val deleteCalls = AtomicInteger()
        val backCalls = AtomicInteger()
        composeRule.setContent {
            MaterialBrowserTheme {
                SnoozedTabsScreen(
                    snoozedTabs = listOf(
                        SnoozedTab(
                            tab = BrowserTab(
                                id = "saved",
                                lastAccessedAt = 1L,
                                profileId = "work",
                                title = "Saved page",
                                url = "https://example.com",
                            ),
                            wakeAtMillis = System.currentTimeMillis() + 60_000L,
                            createdAtMillis = 1L,
                        ),
                    ),
                    profiles = listOf(BrowserProfile("work", "💼")),
                    onBack = { backCalls.incrementAndGet() },
                    onReschedule = { _, _ -> true },
                    onOpenNow = {
                        openCalls.incrementAndGet()
                        true
                    },
                    onDelete = {
                        deleteCalls.incrementAndGet()
                        true
                    },
                )
            }
        }

        composeRule.onNodeWithTag(SnoozeTestTags.card("saved")).assertIsDisplayed()
        composeRule.onNodeWithTag(SnoozeTestTags.workspaceIcon("saved")).assertIsDisplayed()
        // «Open now» is the row's icon; Delete waits in its «⋮» menu (board W-Snoozed).
        composeRule.onNodeWithContentDescription(context.getString(R.string.action_open_now))
            .performClick()
        composeRule.onNodeWithTag(SnoozeTestTags.more("saved")).performClick()
        composeRule.onNodeWithText(context.getString(R.string.action_delete)).performClick()

        assertEquals(1, openCalls.get())
        assertEquals(1, backCalls.get())
        // A deleted tab waits behind «Undo» (M-09) and is deleted when the window closes.
        composeRule.onNodeWithText(context.getString(R.string.snoozed_tab_deleted)).assertExists()
        assertEquals(0, deleteCalls.get())
        composeRule.mainClock.advanceTimeBy(LibraryRules.UNDO_WINDOW_MILLIS * 4)
        composeRule.waitUntil(UNDO_TIMEOUT_MILLIS) { deleteCalls.get() == 1 }
    }

    @Test
    fun snoozeFeedbackShowsUndoAndRunsActionOnce() {
        val hostState = SnackbarHostState()
        val undoCalls = AtomicInteger()
        lateinit var scope: CoroutineScope
        composeRule.setContent {
            scope = rememberCoroutineScope()
            MaterialBrowserTheme { SnackbarHost(hostState) }
        }

        composeRule.runOnIdle {
            scope.launch {
                showSnoozeUndoFeedback(
                    hostState = hostState,
                    message = context.getString(R.string.snooze_confirmation),
                    undoLabel = context.getString(R.string.action_undo),
                    onUndo = { undoCalls.incrementAndGet() },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.snooze_confirmation))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.action_undo)).performClick()
        composeRule.waitUntil { undoCalls.get() == 1 }
        assertEquals(1, undoCalls.get())
    }

    private companion object {
        const val UNDO_TIMEOUT_MILLIS = 5_000L
    }
}
