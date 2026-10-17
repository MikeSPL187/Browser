package dev.sk2andy.materialbrowser.browser

import android.content.ComponentCallbacks2
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The browser hears Android's memory warnings through the application, as the system sends them:
 * a critical one lets go of every background session but keeps the selected tab, and the
 * person's limit of live tabs stays as it was.
 */
@RunWith(AndroidJUnit4::class)
class BrowserMemoryPressureInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun aCriticalWarningKeepsOnlyTheSelectedTabLive() {
        activityRule.scenario.onActivity { activity ->
            val controller = BrowserController(activity)
            val tabsBefore = controller.tabs.size
            repeat(4) { index -> controller.createTab("https://example.com/tab-$index") }
            val limit = controller.residentTabLimit
            assertTrue("the tabs took live sessions", controller.residentTabIdsForTesting().size > 1)

            @Suppress("DEPRECATION")
            activity.application.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)

            assertEquals(setOf(controller.selectedTabId), controller.residentTabIdsForTesting())
            assertEquals("the setting is untouched", limit, controller.residentTabLimit)
            assertEquals("every tab stays in the list", tabsBefore + 4, controller.tabs.size)
            controller.destroy()
        }
    }

    @Test
    fun hidingTheInterfaceTrimsNothingAndADestroyedBrowserStopsListening() {
        activityRule.scenario.onActivity { activity ->
            val controller = BrowserController(activity)
            repeat(2) { index -> controller.createTab("https://example.com/tab-$index") }
            val resident = controller.residentTabIdsForTesting()

            activity.application.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

            assertEquals(resident, controller.residentTabIdsForTesting())
            controller.destroy()
            @Suppress("DEPRECATION")
            activity.application.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        }
    }
}
