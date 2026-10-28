package dev.sk2andy.materialbrowser

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.data.ReleaseNotesStore
import dev.sk2andy.materialbrowser.ui.ReleaseNotesTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5FirstRunTestTags
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InitialOnboardingPresentationInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        prepareInitialOnboarding()
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeRule.activityRule.scenario.close()
        clearPreferences()
    }

    @Test
    fun completingInitialOnboardingDoesNotOpenReleaseNotes() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setReleaseNotesVisible(true)
        }
        composeRule.onNodeWithTag(V5FirstRunTestTags.Welcome).assertExists()
        composeRule.onNodeWithTag(ReleaseNotesTestTags.Screen).assertDoesNotExist()

        // A new install: welcome, «Not now» to the default browser question if it is asked, then the
        // gesture card.
        composeRule.onNodeWithTag(V5FirstRunTestTags.Start).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag(V5FirstRunTestTags.DefaultBrowser).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag(V5FirstRunTestTags.Gestures).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithTag(V5FirstRunTestTags.DefaultBrowser).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag(V5FirstRunTestTags.NotNow).performClick()
        }
        composeRule.onNodeWithTag(V5FirstRunTestTags.GesturesDone).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag(V5FirstRunTestTags.Gestures).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(ReleaseNotesTestTags.Screen).assertDoesNotExist()
        assertEquals(
            BuildConfig.VERSION_CODE.toLong(),
            ReleaseNotesStore(context).lastHandledVersionCode(),
        )
    }

    private fun clearPreferences() {
        listOf(
            BrowserSessionStore.PREFERENCES_NAME,
            GestureOnboardingStore.PREFERENCES_NAME,
            ReleaseNotesStore.PREFERENCES_NAME,
        ).forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    private fun prepareInitialOnboarding() {
        clearPreferences()
        context.getSharedPreferences(
            GestureOnboardingStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit()
            .putBoolean(GestureOnboardingStore.KEY_HAS_STARTED, true)
            .commit()
    }
}
