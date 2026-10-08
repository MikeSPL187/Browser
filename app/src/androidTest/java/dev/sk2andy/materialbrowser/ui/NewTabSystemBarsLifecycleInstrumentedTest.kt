package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.audit.AuditConfig
import dev.sk2andy.materialbrowser.audit.AuditEnvironment
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserSurfaceStyle
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The new tab's header against the status bar through a whole session with the defaults a fresh
 * install has (startup animation on): launch, frosted chrome, recreation, immersive mode on and
 * off, background and back, and a second new tab. Every state is measured and the test reports
 * all of them at once, so one run shows which state lets the page run under the clock.
 */
@RunWith(AndroidJUnit4::class)
class NewTabSystemBarsLifecycleInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val measurements = mutableListOf<String>()
    private val violations = mutableListOf<String>()

    @After
    fun tearDown() {
        clearPreferences()
    }

    @Test
    fun headerStaysBelowStatusBarThroughTheSession() {
        clearPreferences()
        GestureOnboardingStore(context).markCompleted()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            measure(scenario, "launch, defaults")

            updateAppearance(scenario, AppearanceSettings(surfaceStyle = BrowserSurfaceStyle.Frosted))
            measure(scenario, "frosted")

            scenario.recreate()
            measure(scenario, "frosted, recreated")

            scenario.onActivity { activity ->
                activity.browserControllerForTesting().updateFullImmersiveModeEnabled(true)
            }
            measure(scenario, "frosted, immersive on")
            scenario.onActivity { activity ->
                activity.browserControllerForTesting().updateFullImmersiveModeEnabled(false)
            }
            measure(scenario, "frosted, immersive off")

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            measure(scenario, "frosted, back from background")

            scenario.onActivity { activity ->
                activity.browserControllerForTesting().createTab(isIncognito = false)
            }
            measure(scenario, "frosted, second new tab")

            updateAppearance(scenario, AppearanceSettings(surfaceStyle = BrowserSurfaceStyle.Clear))
            measure(scenario, "clear again")

            scenario.recreate()
            measure(scenario, "clear, recreated")
        }
        assertTrue(
            "The header ran under the status bar in: $violations. All states: $measurements",
            violations.isEmpty(),
        )
    }

    /**
     * With double font size the header was filed under the status bar by the screen audit.
     * Reports where the page's scroll container sits, how far it is scrolled and where the
     * header is, so the failure names the cause.
     */
    @Test
    fun largeFontHeaderStaysBelowStatusBar() {
        AuditEnvironment.enter(context, AuditConfig.EnglishDarkLargeFont)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                measure(scenario, "200% font")
                val header = composeRule.onNodeWithTag(NewTabPageTestTags.Header).fetchSemanticsNode()
                var container = header.parent
                while (container != null &&
                    !container.config.contains(SemanticsProperties.VerticalScrollAxisRange)
                ) {
                    container = container.parent
                }
                val range = container?.config?.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                measurements += "scroll container ${container?.boundsInWindow}, " +
                    "scrolled ${range?.value?.invoke()} of ${range?.maxValue?.invoke()}, " +
                    "header ${header.boundsInWindow}"
            }
        } finally {
            AuditEnvironment.reset(context)
        }
        assertTrue(
            "The header ran under the status bar in: $violations. All states: $measurements",
            violations.isEmpty(),
        )
    }

    private fun updateAppearance(
        scenario: ActivityScenario<MainActivity>,
        settings: AppearanceSettings,
    ) {
        scenario.onActivity { activity ->
            activity.browserControllerForTesting().updateAppearanceSettings(settings)
        }
    }

    private fun measure(scenario: ActivityScenario<MainActivity>, state: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            runCatching {
                composeRule.onNodeWithTag(NewTabPageTestTags.Header).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.waitForIdle()
        val statusBar = AtomicReference<Pair<Boolean, Int>>()
        scenario.onActivity { activity ->
            val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            statusBar.set(
                (insets?.isVisible(WindowInsetsCompat.Type.statusBars()) == true) to
                    (insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0),
            )
        }
        val (statusBarVisible, statusBarTop) = statusBar.get()
        val headerTop = composeRule.onNodeWithTag(NewTabPageTestTags.Header)
            .fetchSemanticsNode()
            .boundsInWindow
            .top
        measurements += "$state: header $headerTop, status bar $statusBarTop (visible $statusBarVisible)"
        if (statusBarVisible && headerTop < statusBarTop) violations += state
    }

    private fun clearPreferences() {
        context.getSharedPreferences("browser_session", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000L
    }
}
