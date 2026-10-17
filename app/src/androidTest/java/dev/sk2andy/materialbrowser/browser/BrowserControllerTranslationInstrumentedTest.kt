package dev.sk2andy.materialbrowser.browser

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.actions.BrowserContentTargetListener
import dev.sk2andy.materialbrowser.browser.gecko.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.gecko.BrowserEnginePreviewCapture
import dev.sk2andy.materialbrowser.browser.gecko.GeckoFindResult
import dev.sk2andy.materialbrowser.browser.gecko.GeckoMediaCommand
import dev.sk2andy.materialbrowser.browser.gecko.GeckoMediaSessionStateListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoNavigationRequestListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPrivacyPolicy
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationListener
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationPort
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationResult
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationState
import dev.sk2andy.materialbrowser.browser.translation.TranslationPair
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineCommand
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEvent
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserControllerTranslationInstrumentedTest {
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
    fun aTabFollowsItsPageTranslationUntilTheTabCloses() {
        createController()
        val port = RecordingTranslationPort()
        var tabId = ""
        activityRule.scenario.onActivity { _ ->
            val browserController = requireNotNull(controller)
            tabId = browserController.selectedTabForTesting().id
            browserController.installGeckoEngineSessionForTesting(TranslatingGeckoSession(tabId, port))

            val listener = requireNotNull(port.listener) { "the browser did not follow the tab's translation" }
            listener.onStateChanged(GERMAN_PAGE)
            assertEquals("de", browserController.onDeviceTranslation.stateOf(tabId).page.pageLanguage)

            browserController.closeTab(tabId)
            assertNull(port.listener)
            assertEquals(PageTranslationState(), browserController.onDeviceTranslation.stateOf(tabId).page)
        }
    }

    @Test
    fun aClosedEngineSessionStopsTheTabsTranslation() {
        createController()
        val port = RecordingTranslationPort()
        activityRule.scenario.onActivity { _ ->
            val browserController = requireNotNull(controller)
            val tabId = browserController.selectedTabForTesting().id
            browserController.installGeckoEngineSessionForTesting(TranslatingGeckoSession(tabId, port))
            port.listener?.onStateChanged(GERMAN_PAGE)

            browserController.dispatchGeckoEngineEventForTesting(
                BrowserEngineEvent(
                    tabId = tabId,
                    type = BrowserEngineEventType.Closed,
                    address = null,
                    title = null,
                    canGoBack = false,
                    canGoForward = false,
                    failureDescription = null,
                    isLoading = false,
                ),
            )

            assertNull(port.listener)
            assertNull(browserController.onDeviceTranslation.stateOf(tabId).page.pageLanguage)
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

    /** Records the listener the browser hands the tab's translation port. */
    private class RecordingTranslationPort : PageTranslationPort {
        var listener: PageTranslationListener? = null
            private set

        override fun setListener(listener: PageTranslationListener?) {
            this.listener = listener
        }

        override fun translate(
            pair: TranslationPair,
            allowModelDownload: Boolean,
            onResult: (PageTranslationResult) -> Unit,
        ) = onResult(PageTranslationResult.Done)

        override fun restoreOriginal(onResult: (PageTranslationResult) -> Unit) = onResult(PageTranslationResult.Done)

        override fun neverTranslateSite(onResult: (Boolean?) -> Unit) = onResult(false)

        override fun setNeverTranslateSite(never: Boolean, onResult: (PageTranslationResult) -> Unit) =
            onResult(PageTranslationResult.Done)
    }

    private class TranslatingGeckoSession(
        override val tabId: String,
        private val translation: PageTranslationPort,
    ) : AndroidBrowserEngineSessionPort {
        override fun pageTranslation(): PageTranslationPort = translation

        private val commands = CopyOnWriteArrayList<BrowserEngineCommand>()

        override fun execute(command: BrowserEngineCommand) {
            commands += command
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

    private companion object {
        val GERMAN_PAGE = PageTranslationState(pageLanguage = "de", pageLanguageSupported = true, userLanguage = "en")
    }
}
