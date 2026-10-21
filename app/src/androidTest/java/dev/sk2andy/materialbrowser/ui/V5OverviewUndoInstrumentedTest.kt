package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.StartupAddressFocusMode
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.ui.theme.v5.UiV5Flag
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewTestTags
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * О-9 (audit 20.10): a tab closed from the v5 overview offers «Undo», and the line shows over the
 * overview, above its bottom bar, not hidden under it. The line is drawn by the screen's shared
 * host, so only the real activity shows where it lands.
 */
@RunWith(AndroidJUnit4::class)
class V5OverviewUndoInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        clearPreferences()
        UiV5Flag.setEnabled(context, true)
        GestureOnboardingStore(context).markCompleted()
        BrowserSessionStore(context).apply {
            saveStartupAnimationEnabled(false)
            saveStartupAddressFocusMode(StartupAddressFocusMode.Never)
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeRule.activityRule.scenario.close()
        clearPreferences()
    }

    @Test
    fun undoAfterClosingFromTheOverviewShowsAboveItsBar() {
        var closingId = ""
        composeRule.runOnIdle {
            closingId = composeRule.activity.browserControllerForTesting().createTab()
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(AddressBarTestTags.TabButton)) }
        composeRule.onNodeWithTag(AddressBarTestTags.TabButton).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(V5OverviewTestTags.close(closingId))) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(V5OverviewTestTags.close(closingId)).performClick()

        val closed = hasText(context.getString(R.string.tab_closed), substring = true)
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(closed) }
        composeRule.waitForIdle()
        assertTrue("The overview closed with the tab", exists(hasTestTag(V5OverviewTestTags.Overview)))

        val notice = composeRule.onAllNodes(closed).fetchSemanticsNodes(atLeastOneRootRequired = false)
            .first { it.size.height > 0 }
        val bar = composeRule.onNodeWithTag(V5OverviewTestTags.NewTab).fetchSemanticsNode()
        val window = composeRule.activity.window.decorView
        assertTrue(
            "«Tab closed» is off the screen: ${notice.boundsInRoot} in ${window.width}×${window.height}",
            notice.boundsInRoot.top >= 0f && notice.boundsInRoot.bottom <= window.height,
        )
        assertTrue(
            "«Tab closed» (${notice.boundsInRoot}) lies under the overview's bar (${bar.boundsInRoot})",
            notice.boundsInRoot.bottom <= bar.boundsInRoot.top,
        )
    }

    private fun exists(matcher: SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    private fun clearPreferences() {
        context.getSharedPreferences("browser_session", Context.MODE_PRIVATE).edit().clear().commit()
        // The runner starts every test on the current interface.
        UiV5Flag.setEnabled(context, false)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
