package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserSurfaceStyle
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import eightbitlab.com.blurview.BlurTarget
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserContentBlurTargetInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statusOverlayIsSmallSiblingAndClearModeHasNoFullScreenBlurTarget() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clearHost = StatusBarStaticOverlayHost(
            context = context,
            browserContentBlurEnabled = false,
        )
        clearHost.updateOverlay(
            geometry = StatusBarStaticOverlayGeometry(
                statusBarHeightPx = 72,
                overlayHeightPx = 88,
            ),
            tint = android.graphics.Color.WHITE,
            visible = true,
        )

        val overlay = clearHost.findViewWithTag<android.view.View>(
            StatusBarStaticOverlayTestTags.Overlay,
        )
        assertTrue(clearHost.contentContainer !is BlurTarget)
        assertTrue(clearHost.blurTarget == null)
        assertSame(clearHost.contentContainer, clearHost.getChildAt(0))
        assertSame(overlay, clearHost.getChildAt(1))
        assertTrue(overlay.layoutParams.height == 88)

        clearHost.updateOverlay(
            geometry = StatusBarStaticOverlayGeometry(
                statusBarHeightPx = 72,
                overlayHeightPx = 88,
            ),
            tint = android.graphics.Color.rgb(18, 52, 86),
            visible = true,
            solid = true,
        )
        assertTrue(overlay.layoutParams.height == 72)

        val frostedHost = StatusBarStaticOverlayHost(
            context = context,
            browserContentBlurEnabled = true,
        )
        assertSame(frostedHost.contentContainer, frostedHost.blurTarget)
        assertTrue(frostedHost.blurTarget is BrowserChromeBlurTarget)
        assertTrue((frostedHost.blurTarget as BrowserChromeBlurTarget).captureEnabled)
    }

    @Test
    fun addressBarOnlyTransparencyStillProvidesBackdropTarget() {
        val attached = AtomicBoolean(false)

        composeRule.setContent {
            MaterialBrowserTheme(
                settings = AppearanceSettings(
                    surfaceStyle = BrowserSurfaceStyle.Frosted,
                    frostedTransparencyPercent = 0,
                    frostedAddressBarTransparencyPercent = 50,
                ),
            ) {
                BrowserContentBlurTarget(
                    enabled = true,
                    onTargetAttached = { attached.set(true) },
                    onTargetReleased = {},
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) { attached.get() }
        assertTrue(attached.get())
    }

    @Test
    fun outgoingBlankSourceCannotReleaseNewWebSourceDuringHandoff() {
        var blankVisible by mutableStateOf(true)
        val currentTarget = AtomicReference<BlurTarget?>()
        val webTarget = AtomicReference<BlurTarget?>()

        composeRule.setContent {
            MaterialBrowserTheme(
                settings = AppearanceSettings(surfaceStyle = BrowserSurfaceStyle.Frosted),
            ) {
                Box(Modifier.fillMaxSize()) {
                    BrowserContentBlurTarget(
                        enabled = !blankVisible,
                        onTargetAttached = { target ->
                            webTarget.set(target)
                            currentTarget.set(target)
                        },
                        onTargetReleased = { target ->
                            currentTarget.compareAndSet(target, null)
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(Modifier.fillMaxSize())
                    }
                    BrowserContentBlurTarget(
                        enabled = blankVisible,
                        onTargetAttached = currentTarget::set,
                        onTargetReleased = { target ->
                            currentTarget.compareAndSet(target, null)
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeRule.runOnIdle { blankVisible = false }
        composeRule.waitForIdle()

        assertSame(webTarget.get(), currentTarget.get())
    }

    private companion object {
        const val CUSTOM_RENDERER_TAG = "custom_chrome_renderer"
        const val SOURCE_TAG = "browser_content_blur_source"
    }
}
