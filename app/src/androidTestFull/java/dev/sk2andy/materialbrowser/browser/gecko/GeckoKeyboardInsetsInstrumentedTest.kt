package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.BuildConfig
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.awaitWindowFocusForTests
import dev.sk2andy.materialbrowser.focusedWindowForTests
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.EdgeToEdgeSiteFixtureServer
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.data.ReleaseNotesStore
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeckoKeyboardInsetsInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val store = BrowserSessionStore(context)
    private val preferences = context.getSharedPreferences(
        BrowserSessionStore.PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun forwardedKeyboardInsetsResizeVisualViewportAndRespectChromeOwnership() {
        withFixture { scenario ->
            val initial = awaitReport(scenario) { true }
            val restingMargin = engineBottomMargin(scenario)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, KEYBOARD_HEIGHT_PX))
                .setVisible(WindowInsetsCompat.Type.ime(), true)
                .build()

            scenario.onActivity { it.browserControllerForTesting().onWindowInsetsChanged(insets) }
            // The keyboard first covers what lies below the page card; the engine gives up only
            // the rest, as a bottom margin, and the visual viewport shrinks by exactly that.
            var keyboardMargin = 0
            awaitReport(scenario, describe = { "margin=$restingMargin->$keyboardMargin" }) {
                keyboardMargin = engineBottomMargin(scenario)
                val expectedHeight = initial.getDouble("visualHeight") -
                    (keyboardMargin - restingMargin) / initial.getDouble("density")
                keyboardMargin in (restingMargin + 1)..(restingMargin + KEYBOARD_HEIGHT_PX) &&
                    kotlin.math.abs(it.getDouble("visualHeight") - expectedHeight) < 1
            }

            scenario.onActivity { assertTrue(it.browserControllerForTesting().openFindInPage()) }
            awaitImeVisibility(scenario, true)
            awaitReport(scenario) {
                kotlin.math.abs(it.getDouble("visualHeight") - initial.getDouble("visualHeight")) < 1
            }

            scenario.onActivity { activity ->
                activity.browserControllerForTesting().closeFindInPage()
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .hide(WindowInsetsCompat.Type.ime())
            }
            awaitImeVisibility(scenario, false)
            awaitReport(scenario) {
                kotlin.math.abs(it.getDouble("visualHeight") - initial.getDouble("visualHeight")) < 1
            }
        }
    }

    @Test
    fun lowerWebsiteInputStaysVisibleWithKeyboardAndRestoresViewport() {
        assertLowerInputVisible(immersive = false)
    }

    @Test
    fun immersiveLowerWebsiteInputStaysVisibleWithoutShrinkingNativeHost() {
        assertLowerInputVisible(immersive = true)
    }

    private fun assertLowerInputVisible(immersive: Boolean) {
        withFixture(immersive) { scenario ->
            val initial = awaitReport(scenario) { true }
            val coordinates = FloatArray(2)
            var initialHostHeight = 0
            var initialHostTop = 0
            scenario.onActivity { activity ->
                val host = requireNotNull(activity.browserControllerForTesting().selectedGeckoViewForTesting())
                initialHostHeight = host.height
                initialHostTop = IntArray(2).also(host::getLocationInWindow)[1]
                val view = (host as ViewGroup).getChildAt(0)
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val density = initial.getDouble("density").toFloat()
                coordinates[0] = location[0] + 100f * density
                coordinates[1] = location[1] +
                    (initial.getDouble("inputTop").toFloat() + 20f) * density
                // Gecko installs one root listener per view. Losing that listener when another
                // engine view detaches must not break Candy's explicit per-session forwarding.
                ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView.rootView, null)
            }
            // The keyboard shows only while the window holds the focus; a hung System UI dialog
            // can hold it on the CI emulator (#356).
            lateinit var activity: MainActivity
            scenario.onActivity { activity = it }
            awaitWindowFocusForTests(activity)
            // A tap before Gecko takes input is lost: tap again until the field has focus.
            var tapFocused = false
            var taps = 0
            while (!tapFocused && taps < TAP_ATTEMPTS) {
                taps++
                tap(coordinates[0], coordinates[1])
                tapFocused = runCatching {
                    awaitReport(scenario, timeoutMillis = TAP_SETTLE_MILLIS) { it.getBoolean("focused") }
                }.isSuccess
            }
            awaitImeVisibility(scenario, true)
            val focused = awaitReport(scenario) {
                it.getBoolean("focused") &&
                    it.getDouble("visualHeight") < initial.getDouble("visualHeight") - 100 &&
                    it.getDouble("inputTop") >= it.getDouble("visualTop") - 1 &&
                    it.getDouble("inputBottom") <=
                    it.getDouble("visualTop") + it.getDouble("visualHeight") + 1
            }
            assertTrue(focused.getBoolean("focused"))
            instrumentation.sendStringSync("candy")
            awaitReport(scenario) { it.getString("value") == "candy" && it.getBoolean("focused") }
            scenario.onActivity { activity ->
                val host = requireNotNull(activity.browserControllerForTesting().selectedGeckoViewForTesting())
                if (immersive) assertEquals(initialHostHeight, host.height)
                // The page sits below the status bar since H4 (#123); the keyboard must not move it.
                val location = IntArray(2)
                host.getLocationInWindow(location)
                assertEquals(initialHostTop, location[1])
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .hide(WindowInsetsCompat.Type.ime())
            }
            awaitImeVisibility(scenario, false)
            awaitReport(scenario) {
                kotlin.math.abs(it.getDouble("visualHeight") - initial.getDouble("visualHeight")) < 1
            }
        }
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).use { it.readBytes() }
    }

    private fun withFixture(
        immersive: Boolean = false,
        block: (ActivityScenario<MainActivity>) -> Unit,
    ) {
        EdgeToEdgeSiteFixtureServer { HTML }.use { server ->
            preferences.edit().clear().commit()
            GestureOnboardingStore(context).markCompleted()
            store.saveStartupAnimationEnabled(false)
            store.saveAndroidBrowserEngineKind(AndroidBrowserEngineKind.GeckoView)
            store.saveFullImmersiveModeEnabled(immersive)
            // The first app to go immersive on a device gets Android's "Viewing full screen" cling,
            // a system window that takes input focus: the keyboard is then served to the cling, not
            // to the page. A user dismisses it once; the CI emulator starts fresh every run.
            if (immersive) shell("settings put secure immersive_mode_confirmations confirmed")
            ReleaseNotesStore(context).markHandled(BuildConfig.VERSION_CODE.toLong())
            val tab = BrowserTab(
                id = "gecko-keyboard-fixture",
                lastAccessedAt = System.currentTimeMillis(),
                url = server.url,
            )
            assertTrue(store.saveTabsImmediately(listOf(tab), tab.id))
            ActivityScenario.launch<MainActivity>(
                Intent(context, MainActivity::class.java).setAction(TEST_ACTIVITY_ACTION),
            ).use { scenario ->
                awaitReport(scenario) { true }
                scenario.onActivity { activity ->
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .hide(WindowInsetsCompat.Type.ime())
                }
                awaitImeVisibility(scenario, false)
                // At rest the visual viewport fills the engine view, less at most the band the
                // address bar may cover (Gecko's dynamic toolbar), never more.
                var engineHeight = 0
                var dynamicBandHeight = 0
                awaitReport(scenario, describe = { "engine=$engineHeight band=$dynamicBandHeight" }) { report ->
                    scenario.onActivity { activity ->
                        val host = requireNotNull(
                            activity.browserControllerForTesting().selectedGeckoViewForTesting(),
                        ) as CandyGeckoView
                        engineHeight = host.getChildAt(0).height
                        dynamicBandHeight = host.dynamicToolbarMaxHeightPx
                    }
                    val shortfall = engineHeight -
                        report.getDouble("visualHeight") * report.getDouble("density")
                    shortfall > -2 && shortfall < dynamicBandHeight + 2
                }
                block(scenario)
            }
        }
    }

    /** The bottom margin Vola gives the selected engine view, in pixels. */
    private fun engineBottomMargin(scenario: ActivityScenario<MainActivity>): Int {
        var margin = 0
        scenario.onActivity { activity ->
            val host = activity.browserControllerForTesting().selectedGeckoViewForTesting() as CandyGeckoView
            margin = host.domDiagnosticInsets().getInt("marginBottomPx")
        }
        return margin
    }

    private fun awaitReport(
        scenario: ActivityScenario<MainActivity>,
        describe: () -> String = { "" },
        timeoutMillis: Long = TIMEOUT_MILLIS,
        matches: (JSONObject) -> Boolean,
    ): JSONObject {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        var lastTitle = ""
        while (SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity { lastTitle = it.browserControllerForTesting().selectedTabForTesting().title }
            if (lastTitle.startsWith(REPORT_PREFIX)) {
                val report = JSONObject(lastTitle.removePrefix(REPORT_PREFIX))
                if (matches(report)) return report
            }
            SystemClock.sleep(50)
        }
        // The summary keeps a failure's first 160 characters: the numbers that matter come first.
        throw AssertionError(
            "Keyboard viewport did not settle at line ${callerLine()} ${describe()} " +
                "${compact(lastTitle)}; last report=$lastTitle",
        )
    }

    private fun awaitImeVisibility(scenario: ActivityScenario<MainActivity>, visible: Boolean) {
        var lastTitle = ""
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            var matches = false
            scenario.onActivity {
                matches = ViewCompat.getRootWindowInsets(it.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == visible
                lastTitle = it.browserControllerForTesting().selectedTabForTesting().title
            }
            if (matches) return
            SystemClock.sleep(50)
        }
        throw AssertionError(
            "Expected keyboard visible=$visible at line ${callerLine()} tap=$lastTap ${compact(lastTitle)}; " +
                "focus is on ${focusedWindowForTests()}",
        )
    }

    /** Where the last tap went, in screen pixels, for failure messages. */
    private var lastTap = "none"

    /** The line in this test that started the wait that timed out. */
    private fun callerLine(): Int = Throwable().stackTrace.firstOrNull { frame ->
        frame.className.startsWith(GeckoKeyboardInsetsInstrumentedTest::class.java.name) &&
            frame.methodName.substringBefore('$') !in WAIT_HELPERS
    }?.lineNumber ?: -1

    /** The report's numbers in a few characters: visual height and top, input top and bottom, focus. */
    private fun compact(title: String): String = runCatching {
        val report = JSONObject(title.removePrefix(REPORT_PREFIX))
        "vh=%.1f vt=%.1f in=%.1f..%.1f f=%s".format(
            report.getDouble("visualHeight"),
            report.getDouble("visualTop"),
            report.getDouble("inputTop"),
            report.getDouble("inputBottom"),
            report.getBoolean("focused"),
        )
    }.getOrDefault("no report")

    private fun tap(x: Float, y: Float) {
        lastTap = "%.0f,%.0f".format(x, y)
        val downTime = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
    }

    private companion object {
        const val TEST_ACTIVITY_ACTION = "dev.sk2andy.materialbrowser.test.GECKO_KEYBOARD_INSETS"
        const val KEYBOARD_HEIGHT_PX = 600
        const val TIMEOUT_MILLIS = 20_000L
        const val TAP_ATTEMPTS = 5
        const val TAP_SETTLE_MILLIS = 3_000L
        const val REPORT_PREFIX = "Candy keyboard: "
        val WAIT_HELPERS = setOf("callerLine", "awaitReport", "awaitImeVisibility")
        val HTML = """
            <!doctype html>
            <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
            <style>
              html, body { margin: 0; min-height: 2000px; }
              input { position: absolute; top: 70vh; left: 20px; width: 250px;
                      height: 40px; font-size: 18px; }
            </style>
            <input id="target" aria-label="Keyboard regression input" placeholder="Type here">
            <script>
              const target = document.querySelector('#target');
              let lastReport = '';
              function sample() {
                const rect = target.getBoundingClientRect();
                const report = JSON.stringify({
                  density: devicePixelRatio,
                  visualHeight: visualViewport.height,
                  visualTop: visualViewport.offsetTop,
                  inputTop: rect.top,
                  inputBottom: rect.bottom,
                  focused: document.activeElement === target,
                  value: target.value,
                });
                if (report !== lastReport) {
                  lastReport = report;
                  document.title = '$REPORT_PREFIX' + report;
                }
                requestAnimationFrame(sample);
              }
              requestAnimationFrame(sample);
            </script>
        """.trimIndent()
    }
}
