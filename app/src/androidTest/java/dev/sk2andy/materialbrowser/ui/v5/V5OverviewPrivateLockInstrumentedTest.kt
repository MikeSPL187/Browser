package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Canvas «PrivateLock»: the private segment holds the lock card while its tabs are locked. */
@RunWith(AndroidJUnit4::class)
class V5OverviewPrivateLockInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(privateSegment: Boolean, unlockAvailable: Boolean = true) {
        composeRule.setContent {
            var segment by remember { mutableStateOf(privateSegment) }
            VolaV5Theme(dark = false) {
                V5TabOverview(
                    state = V5OverviewState(
                        cards = emptyList(),
                        privateSegment = segment,
                        counts = V5OverviewCounts(regular = 0, private = 2),
                        query = "",
                        profileLabel = "Personal",
                        privateLock = V5OverviewPrivateLock(count = 2, unlockAvailable = unlockAvailable),
                    ),
                    onQueryChange = {},
                    onSegmentChange = { segment = it },
                    onSelect = {},
                    onClose = {},
                    onNewTab = { calls += "new tab" },
                    onDone = {},
                    onCloseAll = {},
                    onUnlockPrivate = { calls += "unlock" },
                    onCloseAllPrivate = { calls += "close all private" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theLockedSegmentOffersUnlockAndCloseAllButNoNewTab() {
        show(privateSegment = true)

        composeRule.onNodeWithTag(V5OverviewPrivateLockTestTags.Card).assertExists()
        composeRule.onNodeWithTag(V5OverviewTestTags.NewTab).assertDoesNotExist()
        composeRule.onNodeWithTag(V5OverviewPrivateLockTestTags.Unlock).performScrollTo().performClick()
        composeRule.onNodeWithTag(V5OverviewPrivateLockTestTags.CloseAll).performScrollTo().performClick()

        assertEquals(listOf("unlock", "close all private"), calls)
    }

    @Test
    fun theTabsSegmentDoesNotShowTheLock() {
        show(privateSegment = false)

        composeRule.onNodeWithTag(V5OverviewPrivateLockTestTags.Card).assertDoesNotExist()
        composeRule.onNodeWithTag(V5OverviewTestTags.SegmentPrivate).performClick()
        composeRule.onNodeWithTag(V5OverviewPrivateLockTestTags.Card).assertExists()
    }

    @Test
    fun withoutBiometricsUnlockIsOffButCloseAllStays() {
        show(privateSegment = true, unlockAvailable = false)

        composeRule.onNodeWithTag(V5OverviewPrivateLockTestTags.Unlock).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(V5OverviewPrivateLockTestTags.CloseAll).performScrollTo().performClick()

        assertEquals(listOf("close all private"), calls)
    }
}
