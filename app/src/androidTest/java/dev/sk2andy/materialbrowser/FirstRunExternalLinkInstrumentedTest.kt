package dev.sk2andy.materialbrowser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.data.ReleaseNotesStore
import dev.sk2andy.materialbrowser.ui.FirstRunTestTags
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A new install opened from a link shows the page; the first run waits for the launcher (FR-03). */
@RunWith(AndroidJUnit4::class)
class FirstRunExternalLinkInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun setUp() = clearPreferences()

    @After
    fun tearDown() = clearPreferences()

    @Test
    fun coldLinkLaunchShowsThePageAndTheFirstRunWaitsForTheLauncher() {
        val link = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/"))
            .setClass(context, MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(link).use { scenario ->
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(FirstRunTestTags.Welcome).assertDoesNotExist()
            val onboarding = context.getSharedPreferences(
                GestureOnboardingStore.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )
            // An existing session makes the store treat the install as an update, so show it on failure.
            assertTrue(
                "onboarding=${onboarding.all}, session keys=" +
                    context.getSharedPreferences(BrowserSessionStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
                        .all.keys.sorted(),
                onboarding.getBoolean(GestureOnboardingStore.KEY_HAS_STARTED, false),
            )

            // MainActivity is singleTask, so a launcher tap reaches it as a new intent. Starting it
            // again from the test left the scenario unable to close the activity.
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(
                    activity,
                    Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setClass(activity, MainActivity::class.java),
                )
            }
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithTag(FirstRunTestTags.Welcome).fetchSemanticsNodes().isNotEmpty()
            }
        }
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
}
