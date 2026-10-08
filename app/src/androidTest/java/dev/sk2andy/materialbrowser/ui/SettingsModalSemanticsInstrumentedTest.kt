package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.StartupAddressFocusMode
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.shared.ui.BrowserMainMenuTestTags
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Settings cover the whole screen, drawn over the browser in the same window. The page and the
 * address bar under them must leave the accessibility tree while they are open, or TalkBack
 * walks from the last setting into a new tab nobody sees (found by the screen audit).
 */
@RunWith(AndroidJUnit4::class)
class SettingsModalSemanticsInstrumentedTest {
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
    fun browserUnderOpenSettingsIsNotInTheAccessibilityTree() {
        val newTabHeader = hasTestTag(NewTabPageTestTags.Header)
        val tabButton = hasTestTag(AddressBarTestTags.TabButton)
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(newTabHeader) && exists(tabButton) }

        openSettings()
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(SettingsSearchTestTags.Open)) }
        composeRule.waitForIdle()
        assertTrue("The new tab under settings is in the accessibility tree", !exists(newTabHeader))
        assertTrue("The address bar under settings is in the accessibility tree", !exists(tabButton))

        Espresso.pressBack()
        composeRule.waitUntil(TIMEOUT_MILLIS) { !exists(hasTestTag(SettingsSearchTestTags.Open)) }
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(newTabHeader) && exists(tabButton) }
    }

    private fun openSettings() {
        click(hasContentDescription(context.getString(R.string.cd_more_options)))
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(BrowserMainMenuTestTags.Menu)) }
        if (!exists(hasTestTag(BrowserMainMenuTestTags.Settings))) {
            click(hasTestTag(BrowserMainMenuTestTags.More))
            composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(BrowserMainMenuTestTags.Settings)) }
        }
        click(hasTestTag(BrowserMainMenuTestTags.Settings))
    }

    private fun click(matcher: SemanticsMatcher) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(matcher) }
        composeRule.onAllNodes(matcher).onFirst().performClick()
        composeRule.waitForIdle()
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
