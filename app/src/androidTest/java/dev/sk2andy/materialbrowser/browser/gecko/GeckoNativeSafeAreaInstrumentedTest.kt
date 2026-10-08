package dev.sk2andy.materialbrowser.browser.gecko

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.EdgeToEdgeSiteFixtureServer
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The top safe area of a Gecko page is a native margin of the engine view (#123, H4): the page is
 * drawn below the status bar and gets no CSS top inset, so a tap lands where the finger is.
 * Before H4 the renderer got the top as a CSS safe area and GeckoView mapped touches through that
 * edge a second time. Native layout contract only; no scroll performance assertion.
 */
@RunWith(AndroidJUnit4::class)
class GeckoNativeSafeAreaInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun nativeTopMarginMovesThePageBelowTheStatusBarWithoutCssInset() {
        val title = AtomicReference<String?>(null)
        EdgeToEdgeSiteFixtureServer { target ->
            if (target.startsWith("/site-matrix/native-safe-area")) HTML else "<!doctype html><title>Fixture resource</title>"
        }.use { server ->
            ActivityScenario.launch(GeckoScrollTestActivity::class.java).use { scenario ->
                lateinit var session: GeckoBrowserSession
                lateinit var view: View
                scenario.onActivity { activity ->
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                    session = GeckoRuntimeOwner.getOrCreate(activity).createSession(
                        profileId = "native-safe-area-${UUID.randomUUID()}",
                        isPrivate = false,
                        privacyPolicy = GeckoPrivacyPolicy.Disabled,
                    )
                    session.setStateListener { state -> title.set(state.title) }
                    view = session.createView(activity)
                    activity.setContentView(view)
                    session.setActive(true)
                    updateNativeTop(view, 0)
                    assertTrue(session.loadUrl(server.fixtureUrl("/site-matrix/native-safe-area")))
                }
                try {
                    val edgeToEdge = awaitReport(title, "the page to report") { true }
                    val fullHeightPx = edgeToEdge.getDouble("viewportHeight") * edgeToEdge.getDouble("density")
                    var lastSequence = edgeToEdge.getInt("sequence")
                    for (topPx in listOf(144, 216, 0)) {
                        scenario.onActivity { updateNativeTop(view, topPx) }
                        val report = awaitReport(title, "a viewport $topPx px shorter") { candidate ->
                            candidate.getInt("sequence") > lastSequence && kotlin.math.abs(
                                candidate.getDouble("viewportHeight") * candidate.getDouble("density") -
                                    (fullHeightPx - topPx),
                            ) <= VIEWPORT_TOLERANCE_PX
                        }
                        lastSequence = report.getInt("sequence")
                        // The page itself starts at its own top: nothing is under the status bar.
                        assertEquals(0.0, report.getDouble("envInset"), 0.5)
                        assertEquals(0.0, report.getDouble("awareFixedTop"), 0.5)
                        assertEquals(0.0, report.getDouble("unawareFixedTop"), 0.5)
                        assertEquals(0.0, report.getDouble("unawareFlowTop"), 0.5)
                        assertEquals(0.0, report.getDouble("awareFlowContentTop"), 0.5)
                        assertEquals(0.0, report.getDouble("candyInset"), 0.5)
                        assertEquals(0, report.getInt("candyOwnedElements"))
                        scenario.onActivity { activity ->
                            val windowHeight = activity.window.decorView.height
                            assertSpans(view, top = 0, windowHeight = windowHeight)
                            assertSpans((view as ViewGroup).getChildAt(0), top = topPx, windowHeight = windowHeight)
                        }
                    }
                    assertEquals("Native inset changes must not reload the document", 1, server.documentRequestCount.get())
                } finally {
                    scenario.onActivity {
                        session.releaseView(view)
                        session.setActive(false)
                        session.close()
                    }
                }
            }
        }
    }

    private fun updateNativeTop(view: View, topPx: Int) {
        (view as GeckoViewInsetHost).updateInsets(
            GeckoViewInsetRules.resolve(
                safeArea = GeckoViewInsets(left = 0, top = topPx, right = 0, bottom = 0),
                forceNativeSafeArea = false,
                forceNativeTopSafeArea = false,
                isFullscreenContent = false,
                isInsideSafeDrawingHost = false,
            ),
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, topPx, 0, 0))
                .build(),
        )
    }

    private fun awaitReport(
        title: AtomicReference<String?>,
        what: String,
        predicate: (JSONObject) -> Boolean,
    ): JSONObject {
        val deadline = SystemClock.elapsedRealtime() + 30_000
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            val current = title.get().orEmpty()
            val report = if (current.startsWith(REPORT_PREFIX) && current.length < 2_048) {
                runCatching { JSONObject(current.removePrefix(REPORT_PREFIX)) }.getOrNull()
            } else {
                null
            }
            if (report != null && predicate(report)) return report
            SystemClock.sleep(50)
        }
        throw AssertionError("The page did not report $what; last title=${title.get()}")
    }

    /** The view starts [top] px down the window and reaches its bottom edge. */
    private fun assertSpans(view: View, top: Int, windowHeight: Int) {
        val margins = view.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(top, margins.topMargin)
        assertEquals(0, margins.bottomMargin)
        val location = IntArray(2)
        view.getLocationInWindow(location)
        assertEquals(top, location[1])
        assertEquals(windowHeight, location[1] + view.height)
    }

    private companion object {
        const val REPORT_PREFIX = "Candy native safe-area: "

        /** CSS pixels round the viewport height; one device pixel either way is the same layout. */
        const val VIEWPORT_TOLERANCE_PX = 2.0
        val HTML = """
            <!doctype html>
            <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
            <title>Preparing native safe-area</title>
            <style>
              html, body { margin: 0; min-height: 2000px; }
              #probe { position: absolute; padding-top: env(safe-area-inset-top); }
              #aware-fixed { position: fixed; top: env(safe-area-inset-top); left: 80px; }
              #unaware-fixed { position: fixed; top: 0; left: 160px; }
              #aware-flow { position: absolute; top: 0; padding-top: env(safe-area-inset-top); }
            </style>
            <body>
              <div id="probe"></div>
              <button id="aware-fixed">Aware</button>
              <button id="unaware-fixed">Unaware</button>
              <div id="unaware-flow">Unaware flow</div>
              <main id="aware-flow"><div id="flow-content">Aware flow</div></main>
            </body>
            <script>
              let lastReport = '';
              let sequence = 0;
              function sample() {
                const data = {
                  density: devicePixelRatio,
                  viewportHeight: innerHeight,
                  envInset: parseFloat(getComputedStyle(document.querySelector('#probe')).paddingTop),
                  awareFixedTop: document.querySelector('#aware-fixed').getBoundingClientRect().top,
                  unawareFixedTop: document.querySelector('#unaware-fixed').getBoundingClientRect().top,
                  unawareFlowTop: document.querySelector('#unaware-flow').getBoundingClientRect().top,
                  awareFlowContentTop: document.querySelector('#flow-content').getBoundingClientRect().top,
                  candyInset: parseFloat(document.documentElement.style.getPropertyValue(
                    '--candy-browser-content-top-inset')) || 0,
                  candyOwnedElements: document.querySelectorAll(
                    '[data-candy-browser-top-inset-sticky],[data-candy-browser-top-inset-offset]').length,
                };
                const report = JSON.stringify(data);
                if (report !== lastReport) {
                  lastReport = report;
                  document.title = '$REPORT_PREFIX' + JSON.stringify({ ...data, sequence: ++sequence });
                }
                requestAnimationFrame(sample);
              }
              requestAnimationFrame(sample);
            </script>
        """.trimIndent()
    }
}
