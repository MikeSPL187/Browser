package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewTestTags
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserRootBackInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var controller: BrowserController? = null
    private val incomingBrowserNavigationRequestId = mutableIntStateOf(0)

    @After
    fun tearDown() {
        composeRule.runOnIdle {
            controller?.destroy()
            controller = null
            incomingBrowserNavigationRequestId.intValue = 0
            clearSession()
        }
    }

    @Test
    fun systemBackOnRootTabWithSiblingClosesTabAndOpensOverview() {
        val browserController = createController()
        val closingTabId = browserController.createTab()
        setBrowserContent(browserController)

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            browserController.tabs.none { it.id == closingTabId }
        }
        composeRule.onNodeWithTag(V5OverviewTestTags.Overview).assertExists()
    }

    @Test
    fun singleRootTabLeavesSystemBackUnclaimed() {
        val browserController = createController()
        val tabId = browserController.selectedTabId
        setBrowserContent(browserController)

        composeRule.activityRule.scenario.onActivity { activity ->
            assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
        composeRule.runOnIdle {
            assertEquals(tabId, browserController.selectedTabId)
            assertTrue(browserController.tabs.any { it.id == tabId })
        }
    }

    @Test
    fun systemBackOnExternallyOpenedRootTabReturnsWithoutClosingTab() {
        val browserController = createController()
        val externalTabId = browserController.selectedTabId
        val returnCalls = AtomicInteger(0)
        setBrowserContent(
            browserController = browserController,
            externalLaunchTabId = externalTabId,
            onReturnToExternalApp = { returnCalls.incrementAndGet() },
        )

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, returnCalls.get())
            assertTrue(browserController.tabs.any { it.id == externalTabId })
        }
        composeRule.onNodeWithTag(V5OverviewTestTags.Overview).assertDoesNotExist()
    }

    @Test
    fun tabOverviewRequestsPortraitUntilClosed() {
        val browserController = createControllerWithSibling()
        val portraitLocked = AtomicBoolean(false)
        setBrowserContent(
            browserController = browserController,
            onTabOverviewPortraitLockChanged = portraitLocked::set,
        )

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) { portraitLocked.get() }
        composeRule.onNodeWithTag(V5OverviewTestTags.Overview).assertExists()
        composeRule.runOnIdle { assertTrue(portraitLocked.get()) }

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertFalse(portraitLocked.get())
        }
    }

    @Test
    fun incomingBrowserNavigationOpensNewTabAndClosesOverview() {
        val browserController = createControllerWithSibling()
        setBrowserContent(browserController)

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag(V5OverviewTestTags.Overview).assertExists()

        val existingTab = browserController.selectedTab
        val existingTabCount = browserController.tabs.size
        composeRule.runOnIdle {
            browserController.openUrl(TARGET_URL, inNewTab = true)
            incomingBrowserNavigationRequestId.intValue++
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            browserController.tabs.size == existingTabCount + 1 &&
                browserController.selectedTab.url == TARGET_URL
        }
        composeRule.onNodeWithTag(V5OverviewTestTags.Overview).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(
                existingTab.url,
                browserController.tabs.single { it.id == existingTab.id }.url,
            )
            assertNotEquals(existingTab.id, browserController.selectedTabId)
        }
    }

    private fun createController(): BrowserController {
        lateinit var browserController: BrowserController
        composeRule.runOnIdle {
            clearSession()
            browserController = BrowserController(composeRule.activity)
            controller = browserController
        }
        return browserController
    }

    /**
     * Back on a lone root tab goes to the system, which finishes the activity; with a sibling it
     * closes the tab and opens the overview. The tab is made on the main thread: the controller's
     * sync observer persists the tab list there, and a write from the test thread meanwhile threw
     * ConcurrentModificationException in persistableTabs (nightly run 39102575230).
     */
    private fun createControllerWithSibling(): BrowserController =
        createController().also { browserController ->
            composeRule.runOnIdle { browserController.createTab() }
        }

    private fun setBrowserContent(
        browserController: BrowserController,
        externalLaunchTabId: String? = null,
        onReturnToExternalApp: () -> Unit = {},
        onTabOverviewPortraitLockChanged: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialBrowserTheme {
                BrowserScreen(
                    controller = browserController,
                    incomingBrowserNavigationRequestId =
                        incomingBrowserNavigationRequestId.intValue,
                    externalLaunchTabId = externalLaunchTabId,
                    onReturnToExternalApp = onReturnToExternalApp,
                    onTabOverviewPortraitLockChanged = onTabOverviewPortraitLockChanged,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun clearSession() {
        InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences(
            BrowserSessionStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
    }

    private companion object {
        const val TARGET_URL = "https://incoming.example.test/path"
    }
}
