package dev.sk2andy.materialbrowser.browser

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.actions.BrowserContentTargetListener
import dev.sk2andy.materialbrowser.browser.engine.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.engine.BrowserEnginePreviewCapture
import dev.sk2andy.materialbrowser.browser.engine.GeckoFindResult
import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaCommand
import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaSessionStateListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoNavigationRequestListener
import dev.sk2andy.materialbrowser.browser.engine.GeckoPrivacyPolicy
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineCommand
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineCommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The browser hears Android's memory warnings through the application, as the system sends them:
 * a critical one closes every background session but keeps the selected tab, and the person's
 * limit of live tabs stays as it was.
 */
@RunWith(AndroidJUnit4::class)
class BrowserMemoryPressureInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun aCriticalWarningKeepsOnlyTheSelectedTabLive() {
        activityRule.scenario.onActivity { activity ->
            val controller = BrowserController(activity)
            val sessions = liveTabs(controller, count = 4)
            val limit = controller.residentTabLimit

            @Suppress("DEPRECATION")
            activity.application.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)

            assertEquals(setOf(controller.selectedTabId), controller.residentTabIdsForTesting())
            assertFalse("the selected tab stays live", sessions.getValue(controller.selectedTabId).closed)
            assertTrue("the background tabs are closed", sessions.filterKeys { it != controller.selectedTabId }.values.all { it.closed })
            assertEquals("the setting is untouched", limit, controller.residentTabLimit)
            assertTrue("every tab stays in the list", controller.tabs.map { it.id }.containsAll(sessions.keys))
            controller.destroy()
        }
    }

    @Test
    fun hidingTheInterfaceTrimsNothingAndADestroyedBrowserStopsListening() {
        activityRule.scenario.onActivity { activity ->
            val controller = BrowserController(activity)
            val sessions = liveTabs(controller, count = 3)

            activity.application.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

            assertEquals(sessions.keys, controller.residentTabIdsForTesting())
            controller.destroy()
            @Suppress("DEPRECATION")
            activity.application.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        }
    }

    /** [count] tabs with a live session each; the last one is selected. */
    private fun liveTabs(controller: BrowserController, count: Int): Map<String, IdleSession> {
        val sessions = LinkedHashMap<String, IdleSession>()
        repeat(count) { index ->
            val tabId = if (index == 0) controller.selectedTabId else controller.createTab("https://example.com/tab-$index")
            sessions[tabId] = IdleSession(tabId).also(controller::installGeckoEngineSessionForTesting)
        }
        assertEquals(sessions.keys, controller.residentTabIdsForTesting())
        return sessions
    }

    /** A live session that only remembers whether the browser closed it. */
    private class IdleSession(override val tabId: String) : AndroidBrowserEngineSessionPort {
        var closed = false

        override fun execute(command: BrowserEngineCommand) {
            if (command.type == BrowserEngineCommandType.Close) closed = true
        }

        override fun setAudioMuted(muted: Boolean) = Unit

        override fun createView(context: Context): View = View(context)

        override fun awaitContentPresented(listener: () -> Unit) = listener()

        override fun releaseView(view: View) = Unit

        override fun capturePreview(
            targetWidthPx: Int,
            visibleViewHeightPx: Int,
            maximumTargetHeightPx: Int,
            onComplete: (Bitmap?) -> Unit,
        ): BrowserEnginePreviewCapture? = null

        override fun findInPage(
            query: String,
            forward: Boolean,
            onComplete: (GeckoFindResult?) -> Unit,
        ) = onComplete(null)

        override fun clearFindInPage() = Unit

        override fun printPage(): Boolean = false

        override fun setDesktopMode(enabled: Boolean) = Unit

        override fun setActive(active: Boolean) = Unit

        override fun setMediaStateListener(listener: GeckoMediaSessionStateListener?) = Unit

        override fun setScrollListener(listener: BrowserEngineScrollListener?) = Unit

        override fun setContentTargetListener(listener: BrowserContentTargetListener?) = Unit

        override fun setNavigationRequestListener(listener: GeckoNavigationRequestListener?) = Unit

        override fun setVideoAutoplayBlocked(blocked: Boolean) = Unit

        override fun executeMediaCommand(command: GeckoMediaCommand) = Unit

        override fun seekMedia(positionMillis: Long) = Unit

        override fun goToHistoryIndex(index: Int) = Unit

        override fun historyUrlAtOffset(offset: Int): String? = null

        override fun extractPageForReader(onComplete: (String?) -> Unit) = onComplete(null)

        override fun updatePrivacyPolicy(
            policy: GeckoPrivacyPolicy,
            reloadOnCookiePermissionChange: Boolean,
            onReady: () -> Unit,
        ) = onReady()
    }
}
