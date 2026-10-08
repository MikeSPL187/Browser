package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.StartupAddressFocusMode
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The closed tab overview stays composed under the page, drawn fully transparent. Its tab cards
 * must not stay in the accessibility tree with it: TalkBack read «New tab, current tab» on the
 * new tab, a card nobody could see. The other way round, the page under the open overview
 * leaves the tree while the overview covers it, and the address island resting on the dock's
 * new-tab button does not read as a second «New tab» (all found by the screen audit).
 */
@RunWith(AndroidJUnit4::class)
class TabOverviewHiddenSemanticsInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        clearPreferences()
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
    fun closedOverviewCardsAreNotInTheAccessibilityTree() {
        val currentLabel = context.getString(R.string.tab_card_current)
        val currentTabCard = hasContentDescription(currentLabel, substring = true) or
            hasText(currentLabel, substring = true)
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(NewTabPageTestTags.Header)) }
        assertTrue("A closed overview's card is in the accessibility tree", !exists(currentTabCard))

        composeRule.onNodeWithTag(AddressBarTestTags.TabButton).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(TabOverviewChromeTestTags.Root)) }
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(currentTabCard) }
        assertTrue(
            "The page under the open overview is in the accessibility tree",
            !exists(hasTestTag(NewTabPageTestTags.Header)),
        )
        // The address island lands on the dock's new-tab button: TalkBack must read it once.
        val newTabButtons = composeRule
            .onAllNodes(hasContentDescription(context.getString(R.string.cd_new_tab)))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        assertEquals("«New tab» buttons in the open overview", 1, newTabButtons.size)

        Espresso.pressBack()
        composeRule.waitUntil(TIMEOUT_MILLIS) { !exists(hasTestTag(TabOverviewChromeTestTags.Root)) }
        composeRule.waitForIdle()
        assertTrue("The overview's card stayed in the tree after closing it", !exists(currentTabCard))
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(NewTabPageTestTags.Header)) }
    }

    private fun exists(matcher: SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    private fun clearPreferences() {
        context.getSharedPreferences("browser_session", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
