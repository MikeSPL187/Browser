package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.RoundedRectBlurRegion
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.sk2andy.materialbrowser.BuildConfig
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.browser.BrowserBackdropBlurRules
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.EdgeToEdgeSiteFixtureServer
import dev.sk2andy.materialbrowser.browser.EdgeToEdgeSiteMatrix
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.BrowserSurfaceStyle
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.data.ReleaseNotesStore
import dev.sk2andy.materialbrowser.dismissSystemNotResponding
import dev.sk2andy.materialbrowser.focusedWindow
import dev.sk2andy.materialbrowser.ui.BrowserContentFrameRules
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Gecko page in the Air layout since #123 (H4): the window layout moves the whole engine host
 * below the status bar ([BrowserContentFrameRules.belowStatusBar]), the engine view fills the host
 * down to the window's bottom edge, and the page gets no CSS top inset of its own. Candy drew the
 * page from the window's top and protected headers with a CSS inset; with that, taps landed a
 * status bar higher than the finger.
 */
@RunWith(AndroidJUnit4::class)
class GeckoEdgeToEdgeInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = instrumentation.targetContext
    private val store by lazy { BrowserSessionStore(context) }
    private val preferences by lazy {
        context.getSharedPreferences(BrowserSessionStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    @Before
    fun setUp() {
        preferences.edit().clear().commit()
        GestureOnboardingStore(context).markCompleted()
        store.saveStartupAnimationEnabled(false)
        // These tests measure the edge-to-edge page; the framed card would inset it.
        store.saveAppearanceSettings(AppearanceSettings(chromeStyle = BrowserChromeStyle.Air))
        ReleaseNotesStore(context).markHandled(BuildConfig.VERSION_CODE.toLong())
        val tab = BrowserTab(
            id = "gecko-edge-to-edge-fixture",
            lastAccessedAt = System.currentTimeMillis(),
            url = TEST_URL,
        )
        assertTrue(store.saveTabsImmediately(listOf(tab), tab.id))
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun selectedGeckoViewAndSurfaceSitBelowStatusBarAfterInsetDispatch() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).setAction(TEST_ACTIVITY_ACTION),
        ).use { scenario ->
            awaitViewReady(scenario)
            scenario.onActivity { activity ->
                val controller = activity.browserControllerForTesting()
                controller.onWindowInsetsChanged(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            Insets.of(0, STATUS_BAR_INSET_PX, 0, 0),
                        )
                        .setInsets(
                            WindowInsetsCompat.Type.navigationBars(),
                            Insets.of(0, 0, 0, NAVIGATION_BAR_INSET_PX),
                        )
                        .build(),
                )
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val controller = activity.browserControllerForTesting()
                val view = requireNotNull(controller.selectedGeckoViewForTesting())
                val hostTop = safeTopPx(activity)
                val engineTop = hostTop + engineTopMarginPx(hostTop)
                assertMargins(view, left = 0, top = 0, right = 0, bottom = 0)
                assertWindowTop(view, expectedTop = hostTop)
                assertWindowBottom(view, expectedBottom = activity.window.decorView.height)
                assertScreenTop(view, expectedTop = hostTop)
                assertMargins(
                    view.engineView(),
                    left = 0,
                    top = engineTopMarginPx(hostTop),
                    right = 0,
                    bottom = 0,
                )
                assertWindowTop(view.engineView(), expectedTop = engineTop)
                assertWindowBottom(
                    view.engineView(),
                    expectedBottom = activity.window.decorView.height,
                )
                assertScreenTop(view.engineView(), expectedTop = engineTop)
                val surfaceView = requireNotNull(view.findSurfaceView())
                assertWindowTop(surfaceView, expectedTop = engineTop)
                assertWindowBottom(surfaceView, expectedBottom = activity.window.decorView.height)
                assertScreenTop(surfaceView, expectedTop = engineTop)
                assertTrue(
                    "GeckoView must use SurfaceView to avoid copying every page frame",
                    view.hasSurfaceView(),
                )
            }
        }
    }

    @Test
    fun frostedGeckoKeepsDirectSurfaceViewAcrossNavigationBar() {
        val fixtureTitle = "Gecko backdrop fixture"
        EdgeToEdgeSiteFixtureServer {
            """
                <!doctype html>
                <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
                <title>$fixtureTitle</title>
                <style>
                  html, body { margin: 0; min-height: 2000px; background: #336699; }
                  body { padding-top: env(safe-area-inset-top); }
                </style>
                <main>Backdrop capture</main>
            """.trimIndent()
        }.use { server ->
            val tab = BrowserTab(
                id = "gecko-frosted-edge-to-edge-fixture",
                lastAccessedAt = System.currentTimeMillis(),
                url = server.url,
            )
            assertTrue(store.saveTabsImmediately(listOf(tab), tab.id))

            ActivityScenario.launch<MainActivity>(
                Intent(context, MainActivity::class.java).setAction(TEST_ACTIVITY_ACTION),
            ).use { scenario ->
                awaitViewReady(scenario)
                awaitSelectedTabTitle(
                    scenario,
                    fixtureTitle,
                )
                scenario.onActivity { activity ->
                    val controller = activity.browserControllerForTesting()
                    controller.updateAppearanceSettings(
                        AppearanceSettings(
                            chromeStyle = BrowserChromeStyle.Air,
                            surfaceStyle = BrowserSurfaceStyle.Frosted,
                            frostedTransparencyPercent = 0,
                            frostedAddressBarTransparencyPercent = 50,
                        ),
                    )
                    controller.onWindowInsetsChanged(
                        WindowInsetsCompat.Builder()
                            .setInsets(
                                WindowInsetsCompat.Type.statusBars(),
                                Insets.of(0, STATUS_BAR_INSET_PX, 0, 0),
                            )
                            .setInsets(
                                WindowInsetsCompat.Type.navigationBars(),
                                Insets.of(0, 0, 0, NAVIGATION_BAR_INSET_PX),
                            )
                            .build(),
                    )
                }
                awaitViewReady(scenario)
                instrumentation.waitForIdleSync()
                val nativeBlurRegion = if (Build.VERSION.SDK_INT >= 37) {
                    awaitNativeBlurRegion(scenario)
                } else {
                    null
                }

                scenario.onActivity { activity ->
                    val controller = activity.browserControllerForTesting()
                    val view = requireNotNull(controller.selectedGeckoViewForTesting())
                    assertTrue(
                        "Frosted GeckoView must not use a full-screen backdrop target",
                        view.parent !is eightbitlab.com.blurview.BlurTarget,
                    )
                    assertTrue(
                        "Frosted GeckoView must keep the direct compositor surface",
                        view.hasSurfaceView(),
                    )
                    assertTrue(
                        "Frosted GeckoView must never fall back to TextureView",
                        view.findTextureView() == null,
                    )
                    val surfaceView = requireNotNull(view.findSurfaceView())
                    val hostTop = safeTopPx(activity)
                    assertWindowTop(surfaceView, expectedTop = hostTop + engineTopMarginPx(hostTop))
                    assertWindowBottom(surfaceView, expectedBottom = activity.window.decorView.height)
                    if (Build.VERSION.SDK_INT >= 37) {
                        val region = requireNotNull(nativeBlurRegion)
                        val requestedRegion = requireNotNull(
                            controller.selectedBrowserBackdropBlurRegionForTesting(),
                        )
                        val locationInWindow = IntArray(2)
                        surfaceView.getLocationInWindow(locationInWindow)
                        val expectedRegion = requireNotNull(
                            BrowserBackdropBlurRules.regionInSurface(
                                region = requestedRegion,
                                surfaceLeftInWindowPx = locationInWindow[0],
                                surfaceTopInWindowPx = locationInWindow[1],
                                surfaceWidthPx = surfaceView.width,
                                surfaceHeightPx = surfaceView.height,
                            ),
                        )
                        assertEquals(expectedRegion.leftPx, region.left, 0.01f)
                        assertEquals(expectedRegion.topPx, region.top, 0.01f)
                        assertEquals(expectedRegion.rightPx, region.right, 0.01f)
                        assertEquals(expectedRegion.bottomPx, region.bottom, 0.01f)
                        assertEquals(expectedRegion.cornerRadiusPx, region.cornerRadius, 0.01f)
                        assertEquals(expectedRegion.blurRadiusPx, region.blurRadius, 0.01f)
                        assertTrue(region.right - region.left > 0f)
                        assertTrue(region.bottom - region.top > 0f)
                        assertTrue(region.blurRadius > 0f)
                    }
                    activity.browserControllerForTesting().updateAppearanceSettings(
                        AppearanceSettings(chromeStyle = BrowserChromeStyle.Air),
                    )
                }
                awaitViewReady(scenario)
                if (Build.VERSION.SDK_INT >= 37) {
                    awaitNativeBlurRegionCleared(scenario)
                }

                scenario.onActivity { activity ->
                    val view = requireNotNull(
                        activity.browserControllerForTesting().selectedGeckoViewForTesting(),
                    )
                    assertTrue(
                        "Non-frosted GeckoView must restore the direct compositor surface",
                        view.hasSurfaceView(),
                    )
                    if (Build.VERSION.SDK_INT >= 37) {
                        assertTrue(requireNotNull(view.findSurfaceView()).blurRegions.isEmpty())
                    }
                }
            }
        }
    }

    @Test
    fun forcedSafeAreaUsesInnerNativeMarginsWithoutShrinkingOuterHost() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).setAction(TEST_ACTIVITY_ACTION),
        ).use { scenario ->
            awaitViewReady(scenario)
            scenario.onActivity { activity ->
                val controller = activity.browserControllerForTesting()
                val view = requireNotNull(controller.selectedGeckoViewForTesting())
                assertTrue(controller.setForceSafeArea(controller.selectedTabId, true))
                controller.onWindowInsetsChanged(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            Insets.of(0, STATUS_BAR_INSET_PX, 0, 0),
                        )
                        .setInsets(
                            WindowInsetsCompat.Type.navigationBars(),
                            Insets.of(0, 0, 0, NAVIGATION_BAR_INSET_PX),
                        )
                        .build(),
                )

                assertMargins(view, left = 0, top = 0, right = 0, bottom = 0)
                assertWindowTop(view, expectedTop = safeTopPx(activity))
                // The forced safe area is native margins inside the host, less the part of the
                // status bar the host already sits below.
                assertMargins(
                    view.engineView(),
                    left = 0,
                    top = engineTopMarginPx(safeTopPx(activity)),
                    right = 0,
                    bottom = NAVIGATION_BAR_INSET_PX,
                )
            }

            scenario.onActivity { activity ->
                val controller = activity.browserControllerForTesting()
                assertTrue(controller.setForceSafeArea(controller.selectedTabId, false))
                controller.onWindowInsetsChanged(
                    WindowInsetsCompat.Builder()
                        .setInsets(
                            WindowInsetsCompat.Type.statusBars(),
                            Insets.of(0, STATUS_BAR_INSET_PX, 0, 0),
                        )
                        .setInsets(
                            WindowInsetsCompat.Type.navigationBars(),
                            Insets.of(0, 0, 0, NAVIGATION_BAR_INSET_PX),
                        )
                        .build(),
                )
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(LAYOUT_STABILITY_WINDOW_MILLIS)
            awaitViewReady(scenario)

            scenario.onActivity { activity ->
                val controller = activity.browserControllerForTesting()
                val view = requireNotNull(controller.selectedGeckoViewForTesting())
                assertEquals(0, controller.previewTopInsetPx(controller.selectedTabId))
                assertMargins(view.engineView(), left = 0, top = 0, right = 0, bottom = 0)
                assertWindowTop(view.engineView(), expectedTop = safeTopPx(activity))
                assertWindowBottom(
                    view.engineView(),
                    expectedBottom = activity.window.decorView.height,
                )
            }
        }
    }

    @Test
    fun requestedSiteLayoutsSitBelowStatusBarWithoutCssInset() {
        EdgeToEdgeSiteFixtureServer().use { server ->
            val tab = BrowserTab(
                id = "gecko-site-matrix-safe-area-fixture",
                lastAccessedAt = System.currentTimeMillis(),
                url = server.siteUrl(EdgeToEdgeSiteMatrix.allSites.first(), nativeTop = true),
            )
            assertTrue(store.saveTabsImmediately(listOf(tab), tab.id))

            ActivityScenario.launch<MainActivity>(
                Intent(context, MainActivity::class.java).setAction(TEST_ACTIVITY_ACTION),
            ).use { scenario ->
                awaitViewReady(scenario)
                EdgeToEdgeSiteMatrix.allSites.forEachIndexed { index, site ->
                    if (index > 0) {
                        scenario.onActivity { activity ->
                            assertTrue(
                                activity.browserControllerForTesting()
                                    .openUrl(server.siteUrl(site, nativeTop = true)),
                            )
                        }
                    }
                    awaitSelectedTabTitle(scenario, EdgeToEdgeSiteMatrix.readyTitle(site))
                    val documentRequestsBeforeScroll = server.documentRequestCount.get()
                    scenario.onActivity { activity ->
                        assertTrue(
                            activity.browserControllerForTesting()
                                .scrollSelectedBrowserEngineToVerticalOffset(SCROLL_OFFSET_PX),
                        )
                        assertTrue(
                            activity.browserControllerForTesting()
                                .scrollSelectedBrowserEngineToVerticalOffset(0),
                        )
                    }
                    SystemClock.sleep(LAYOUT_STABILITY_WINDOW_MILLIS)

                    scenario.onActivity { activity ->
                        val controller = activity.browserControllerForTesting()
                        val view = requireNotNull(controller.selectedGeckoViewForTesting())
                        assertEquals(0, controller.previewTopInsetPx(controller.selectedTabId))
                        assertMargins(view, left = 0, top = 0, right = 0, bottom = 0)
                        assertMargins(
                            view.engineView(),
                            left = 0,
                            top = 0,
                            right = 0,
                            bottom = 0,
                        )
                        assertWindowTop(view, expectedTop = safeTopPx(activity))
                        assertWindowTop(view.engineView(), expectedTop = safeTopPx(activity))
                        assertWindowBottom(view, expectedBottom = activity.window.decorView.height)
                        assertWindowBottom(
                            view.engineView(),
                            expectedBottom = activity.window.decorView.height,
                        )
                        assertEquals(
                            EdgeToEdgeSiteMatrix.readyTitle(site),
                            controller.selectedTabForTesting().title,
                        )
                    }
                    assertEquals(
                        documentRequestsBeforeScroll,
                        server.documentRequestCount.get(),
                    )
                }
            }
        }
    }

    @Test
    fun youtubeAndGoogleTouchFocusKeepSearchBelowStatusBarWhileImeResizes() {
        EdgeToEdgeSiteFixtureServer().use { server ->
            val tab = BrowserTab(
                id = "gecko-focused-search-safe-area-fixture",
                lastAccessedAt = System.currentTimeMillis(),
                url = server.siteUrl(EdgeToEdgeSiteMatrix.allSites.first(), nativeTop = true),
            )
            assertTrue(store.saveTabsImmediately(listOf(tab), tab.id))

            ActivityScenario.launch<MainActivity>(
                Intent(context, MainActivity::class.java).setAction(TEST_ACTIVITY_ACTION),
            ).use { scenario ->
                awaitViewReady(scenario)
                awaitSelectedTabTitle(
                    scenario,
                    EdgeToEdgeSiteMatrix.readyTitle(EdgeToEdgeSiteMatrix.allSites.first()),
                )

                EdgeToEdgeSiteMatrix.focusedSearchSites
                    .filter { site -> site.name == "YouTube" || site.name == "Google" }
                    .forEach { site ->
                    scenario.onActivity { activity ->
                        assertTrue(
                            activity.browserControllerForTesting()
                                .openUrl("${server.siteUrl(site, nativeTop = true)}#${site.name}"),
                        )
                    }
                    awaitSelectedTabTitle(
                        scenario,
                        "Candy focused search ready: ${site.name}",
                    )
                    val focusedTitle = "Candy focused search safe: ${site.name}"
                    device.dismissSystemNotResponding()
                    tapSearchField(scenario)
                    if (!titleReached(scenario, focusedTitle, TAP_RESPONSE_MILLIS)) {
                        // A tap that lands before the page takes input is lost; a user taps again.
                        device.dismissSystemNotResponding()
                        tapSearchField(scenario)
                    }
                    awaitSelectedTabTitle(scenario, focusedTitle)
                    awaitImeVisibility(scenario, expectedVisible = true)
                    scenario.onActivity { activity ->
                        val controller = activity.browserControllerForTesting()
                        val view = requireNotNull(controller.selectedGeckoViewForTesting())
                        assertWindowTop(
                            view.engineView(),
                            safeTopPx(activity) +
                                controller.previewTopInsetPx(controller.selectedTabId),
                        )
                        WindowCompat.getInsetsController(
                            activity.window,
                            activity.window.decorView,
                        ).hide(WindowInsetsCompat.Type.ime())
                    }
                    awaitImeVisibility(scenario, expectedVisible = false)
                }
            }
        }
    }

    private fun awaitViewReady(
        scenario: ActivityScenario<MainActivity>,
    ) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            var ready = false
            scenario.onActivity { activity ->
                ready = activity.browserControllerForTesting()
                    .selectedGeckoViewForTesting()
                    ?.let { view ->
                        view.isAttachedToWindow &&
                            view.width > 0 &&
                            view.height > 0 &&
                            view.hasSurfaceView()
                    } == true
            }
            if (ready) return
            SystemClock.sleep(POLL_MILLIS)
        }
        assertTrue(
            "Gecko SurfaceView did not become ready",
            false,
        )
    }

    /**
     * What the test reads of a native blur region. The API 37 type stays inside the method body:
     * in a signature it made JUnit's reflection fail to load the whole class on older devices, so
     * none of these tests ran (an initializationError on the API 35 CI emulator).
     */
    private data class ObservedBlurRegion(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val cornerRadius: Float,
        val blurRadius: Float,
    )

    @RequiresApi(37)
    private fun awaitNativeBlurRegion(
        scenario: ActivityScenario<MainActivity>,
    ): ObservedBlurRegion {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        var lastRequestedRegion = "null"
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            var observedRegion: ObservedBlurRegion? = null
            scenario.onActivity { activity ->
                val controller = activity.browserControllerForTesting()
                lastRequestedRegion = controller
                    .selectedBrowserBackdropBlurRegionForTesting()
                    .toString()
                val region = controller.selectedGeckoViewForTesting()
                    ?.findSurfaceView()
                    ?.blurRegions
                    ?.singleOrNull() as? RoundedRectBlurRegion
                observedRegion = region?.let {
                    ObservedBlurRegion(
                        left = it.bounds.left,
                        top = it.bounds.top,
                        right = it.bounds.right,
                        bottom = it.bounds.bottom,
                        cornerRadius = it.cornerRadii[0],
                        blurRadius = it.blurRadius,
                    )
                }
            }
            observedRegion?.let { return it }
            SystemClock.sleep(POLL_MILLIS)
        }
        throw AssertionError(
            "Gecko SurfaceView did not receive a blur region; requested=$lastRequestedRegion",
        )
    }

    @RequiresApi(37)
    private fun awaitNativeBlurRegionCleared(
        scenario: ActivityScenario<MainActivity>,
    ) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            var cleared = false
            scenario.onActivity { activity ->
                cleared = activity.browserControllerForTesting()
                    .selectedGeckoViewForTesting()
                    ?.findSurfaceView()
                    ?.blurRegions
                    ?.isEmpty() == true
            }
            if (cleared) return
            SystemClock.sleep(POLL_MILLIS)
        }
        throw AssertionError("Gecko SurfaceView did not clear its blur region")
    }

    private fun awaitSelectedTabTitle(
        scenario: ActivityScenario<MainActivity>,
        expectedTitle: String,
    ) {
        if (titleReached(scenario, expectedTitle, TIMEOUT_MILLIS)) return
        val seen = seenTitles.joinToString(" → ")
        // The system UI's «isn't responding» dialog takes the window's focus; it is not the app's.
        val focusedWindow = device.focusedWindow()
        if ("Not Responding" in focusedWindow) {
            device.dismissSystemNotResponding()
            if (titleReached(scenario, expectedTitle, TIMEOUT_MILLIS)) return
        }
        throw AssertionError(
            "Selected Gecko tab did not reach title $expectedTitle; last title was $lastSeenTitle; " +
                "titles: $seen; " +
                "${engineState(scenario)}; focused window: $focusedWindow",
        )
    }

    /** The titles the page went through while [titleReached] waited, with their times. */
    private val seenTitles = mutableListOf<String>()
    private var lastSeenTitle: String? = null

    private fun titleReached(
        scenario: ActivityScenario<MainActivity>,
        expectedTitle: String,
        timeoutMillis: Long,
    ): Boolean {
        val started = SystemClock.elapsedRealtime()
        val deadline = started + timeoutMillis
        seenTitles.clear()
        lastSeenTitle = null
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            var title = ""
            scenario.onActivity { activity ->
                title = activity.browserControllerForTesting().selectedTabForTesting().title
            }
            if (title == expectedTitle) return true
            if (title != lastSeenTitle) {
                lastSeenTitle = title
                seenTitles += "${SystemClock.elapsedRealtime() - started} ms ${title.take(TITLE_CHARS)}"
            }
            SystemClock.sleep(POLL_MILLIS)
        }
        return false
    }

    private fun engineState(scenario: ActivityScenario<MainActivity>): String {
        var state = ""
        scenario.onActivity { activity ->
            val controller = activity.browserControllerForTesting()
            val engine = controller.selectedGeckoViewForTesting()?.engineView()
            val location = IntArray(2).also { engine?.getLocationInWindow(it) }
            state = "activity ${activity.lifecycle.currentState}, window focus " +
                "${activity.hasWindowFocus()}, url ${controller.selectedTabForTesting().url}, " +
                "engine shown ${engine?.isShown}, top ${location[1]}, " +
                "size ${engine?.width}x${engine?.height}, input focus ${engine?.hasFocus()}"
        }
        return state
    }

    private fun tapSearchField(scenario: ActivityScenario<MainActivity>) {
        val coordinates = FloatArray(2)
        scenario.onActivity { activity ->
            val engineView = requireNotNull(
                activity.browserControllerForTesting().selectedGeckoViewForTesting(),
            ).engineView()
            val location = IntArray(2)
            engineView.getLocationOnScreen(location)
            val density = activity.resources.displayMetrics.density
            // The engine view already starts below the status bar: the page's top is its top.
            coordinates[0] = location[0] + SEARCH_TAP_X_CSS_PX * density
            coordinates[1] = location[1] + SEARCH_TAP_Y_CSS_PX * density
        }
        val downTime = SystemClock.uptimeMillis()
        injectTouch(
            action = MotionEvent.ACTION_DOWN,
            downTime = downTime,
            eventTime = downTime,
            x = coordinates[0],
            y = coordinates[1],
        )
        injectTouch(
            action = MotionEvent.ACTION_UP,
            downTime = downTime,
            eventTime = SystemClock.uptimeMillis(),
            x = coordinates[0],
            y = coordinates[1],
        )
        instrumentation.waitForIdleSync()
    }

    private fun injectTouch(
        action: Int,
        downTime: Long,
        eventTime: Long,
        x: Float,
        y: Float,
    ) {
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0).also { event ->
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue(
                    "Input injection failed for ${MotionEvent.actionToString(action)}",
                    instrumentation.uiAutomation.injectInputEvent(event, true),
                )
            } finally {
                event.recycle()
            }
        }
    }

    private fun awaitImeVisibility(
        scenario: ActivityScenario<MainActivity>,
        expectedVisible: Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            var visible = false
            scenario.onActivity { activity ->
                visible = ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            if (visible == expectedVisible) return
            SystemClock.sleep(POLL_MILLIS)
        }
        assertTrue("Expected IME visible=$expectedVisible", false)
    }

    private fun View.hasSurfaceView(): Boolean =
        findSurfaceView() != null

    private fun View.findSurfaceView(): SurfaceView? = when (this) {
        is SurfaceView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { index ->
            getChildAt(index).findSurfaceView()
        }
        else -> null
    }

    private fun View.findTextureView(): TextureView? = when (this) {
        is TextureView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { index ->
            getChildAt(index).findTextureView()
        }
        else -> null
    }

    private fun View.engineView(): View = (this as ViewGroup).getChildAt(0)

    private fun assertMargins(
        view: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        val margins = view.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(left, margins.leftMargin)
        assertEquals(top, margins.topMargin)
        assertEquals(right, margins.rightMargin)
        assertEquals(bottom, margins.bottomMargin)
    }

    /** Where the window layout puts the Gecko host: below the real status bar and cutout. */
    private fun safeTopPx(activity: MainActivity): Int =
        requireNotNull(ViewCompat.getRootWindowInsets(activity.window.decorView)) {
            "The window has no insets yet"
        }.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            .top

    /** The engine's own top margin: the dispatched status bar less what the host sits below. */
    private fun engineTopMarginPx(hostTop: Int): Int =
        (STATUS_BAR_INSET_PX - hostTop).coerceAtLeast(0)

    private fun assertWindowTop(view: View, expectedTop: Int) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        assertEquals(expectedTop, location[1])
    }

    private fun assertWindowBottom(view: View, expectedBottom: Int) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        assertEquals(expectedBottom, location[1] + view.height)
    }

    private fun assertScreenTop(view: View, expectedTop: Int) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        assertEquals(expectedTop, location[1])
    }

    private companion object {
        const val TEST_ACTIVITY_ACTION = "dev.sk2andy.materialbrowser.test.GECKO_EDGE_TO_EDGE"
        const val TEST_URL = "https://example.invalid/candy-edge-to-edge"
        const val STATUS_BAR_INSET_PX = 96
        const val NAVIGATION_BAR_INSET_PX = 48
        const val SCROLL_OFFSET_PX = 600
        const val LAYOUT_STABILITY_WINDOW_MILLIS = 1_600L
        const val SEARCH_TAP_X_CSS_PX = 100f
        const val SEARCH_TAP_Y_CSS_PX = 16f
        const val TIMEOUT_MILLIS = 15_000L
        const val TAP_RESPONSE_MILLIS = 5_000L
        const val TITLE_CHARS = 80
        const val POLL_MILLIS = 50L
    }
}
