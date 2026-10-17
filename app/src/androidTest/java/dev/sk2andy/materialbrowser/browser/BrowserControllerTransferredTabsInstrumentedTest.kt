package dev.sk2andy.materialbrowser.browser

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserControllerTransferredTabsInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(ComponentActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var controller: BrowserController? = null

    @After
    fun tearDown() {
        activityRule.scenario.onActivity { activity ->
            controller?.onPause()
            controller?.onStop()
            controller?.destroy()
            controller = null
            clearState(activity)
        }
    }

    @Test
    fun transferredTabsOpenUnloadedInOneGroupNamedAfterTheDevice() {
        createController()
        activityRule.scenario.onActivity { _ ->
            val browserController = requireNotNull(controller)
            val selectedBefore = browserController.selectedTabForTesting().id
            val transferred = listOf(
                LocalTransferTab("https://example.com/a", "Page A"),
                LocalTransferTab("https://example.org/b", "Page B"),
            )

            val opened = browserController.openTransferredTabs(transferred, deviceName = " Pixel 8 ")

            assertEquals(2, opened)
            val tabs = browserController.tabs.filter { tab -> transferred.any { it.url == tab.url } }
            assertEquals(listOf("Page A", "Page B"), tabs.map { it.title })
            tabs.forEach { tab -> assertFalse("${tab.url} started loading before it was shown", tab.isLoading) }
            assertEquals("the selected tab must stay", selectedBefore, browserController.selectedTabForTesting().id)
            val group = browserController.tabStacks.single { it.name == "Pixel 8" }
            assertEquals(tabs.map { it.id }.toSet(), group.tabIds.toSet())
        }
    }

    @Test
    fun nothingToOpenMakesNoGroup() {
        createController()
        activityRule.scenario.onActivity { _ ->
            val browserController = requireNotNull(controller)
            val tabCount = browserController.tabs.size

            assertEquals(0, browserController.openTransferredTabs(emptyList(), deviceName = "Pixel 8"))
            assertEquals(tabCount, browserController.tabs.size)
            assertFalse(browserController.tabStacks.any { it.name == "Pixel 8" })
        }
    }

    private fun createController() {
        activityRule.scenario.onActivity { activity ->
            clearState(activity)
            controller = BrowserController(activity).also { browserController ->
                browserController.onStart()
                browserController.onResume()
            }
        }
        instrumentation.waitForIdleSync()
    }

    private fun clearState(context: Context) {
        context.getSharedPreferences(BrowserSessionStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
