package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.StartupAddressFocusMode
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.BrowserSurfaceStyle
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The new tab keeps its header below the status bar however the chrome is drawn. With frosted
 * chrome the page lives in the blur target's own ComposeView, which gets no window insets: the
 * workspace name and the date ran under the clock until the insets were passed in from outside.
 */
@RunWith(AndroidJUnit4::class)
class NewTabSystemBarsInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        clearPreferences()
        GestureOnboardingStore(context).markCompleted()
        BrowserSessionStore(context).apply {
            saveStartupAnimationEnabled(false)
            saveStartupAddressFocusMode(StartupAddressFocusMode.Never)
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeRule.activityRule.scenario.close()
        clearPreferences()
    }

    @Test
    fun frostedFrameKeepsHeaderBelowStatusBar() {
        assertHeaderBelowStatusBar(BrowserSurfaceStyle.Frosted, BrowserChromeStyle.Frame)
    }

    @Test
    fun frostedAirKeepsHeaderBelowStatusBar() {
        assertHeaderBelowStatusBar(BrowserSurfaceStyle.Frosted, BrowserChromeStyle.Air)
    }

    @Test
    fun frostedDarkKeepsHeaderBelowStatusBar() {
        assertHeaderBelowStatusBar(
            BrowserSurfaceStyle.Frosted,
            BrowserChromeStyle.Frame,
            BrowserAppearanceMode.Dark,
        )
    }

    @Test
    fun clearFrameKeepsHeaderBelowStatusBar() {
        assertHeaderBelowStatusBar(BrowserSurfaceStyle.Clear, BrowserChromeStyle.Frame)
    }

    @Test
    fun clearAirKeepsHeaderBelowStatusBar() {
        assertHeaderBelowStatusBar(BrowserSurfaceStyle.Clear, BrowserChromeStyle.Air)
    }

    @Test
    fun switchingToFrostedLiveKeepsHeaderBelowStatusBar() {
        assertHeaderBelowStatusBar(BrowserSurfaceStyle.Clear, BrowserChromeStyle.Frame)
        assertHeaderBelowStatusBar(BrowserSurfaceStyle.Frosted, BrowserChromeStyle.Frame)
        assertHeaderBelowStatusBar(BrowserSurfaceStyle.Clear, BrowserChromeStyle.Frame)
    }

    private fun assertHeaderBelowStatusBar(
        surfaceStyle: BrowserSurfaceStyle,
        chromeStyle: BrowserChromeStyle,
        appearanceMode: BrowserAppearanceMode = BrowserAppearanceMode.Light,
    ) {
        composeRule.runOnIdle {
            composeRule.activity.browserControllerForTesting().updateAppearanceSettings(
                AppearanceSettings(
                    appearanceMode = appearanceMode,
                    chromeStyle = chromeStyle,
                    surfaceStyle = surfaceStyle,
                ),
            )
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            runCatching {
                composeRule.onNodeWithTag(NewTabPageTestTags.Header).fetchSemanticsNode()
            }.isSuccess
        }
        val statusBarTop = composeRule.runOnIdle {
            val activity = composeRule.activity
            assertEquals(BLANK_URL, activity.browserControllerForTesting().selectedTab.url)
            ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())
                ?.top
                ?: 0
        }
        assertTrue("The emulator must draw a status bar", statusBarTop > 0)

        val header = composeRule.onNodeWithTag(NewTabPageTestTags.Header).fetchSemanticsNode()
        assertTrue(
            "Header top ${header.boundsInWindow.top} must be below the status bar ($statusBarTop px) " +
                "with $surfaceStyle chrome in $chromeStyle, $appearanceMode",
            header.boundsInWindow.top >= statusBarTop,
        )
    }

    private fun clearPreferences() {
        context.getSharedPreferences("browser_session", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
