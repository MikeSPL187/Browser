package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.browser.WebContentTopInsetTransitionState
import org.junit.Assert.assertEquals
import org.junit.Test

class GeckoViewInsetRulesTest {
    @Test
    fun `keyboard reserves native surface space below the native safe area`() {
        val safeArea = GeckoViewInsets(left = 8, top = 72, right = 6, bottom = 48)
        val layout = GeckoViewInsetRules.resolve(
            safeArea = safeArea,
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
            keyboardBottomInsetPx = 600,
        )

        assertEquals(GeckoViewInsets(8, 72, 6, 600), layout.margins)
        assertEquals(GeckoViewInsets.Zero, layout.rendererSafeAreaOverride)
    }

    @Test
    fun `keyboard and forced native bottom safe area overlap rather than add`() {
        val safeArea = GeckoViewInsets(left = 8, top = 72, right = 6, bottom = 48)
        for ((keyboardInset, expectedBottom) in listOf(600 to 600, 0 to 48, -10 to 48)) {
            val layout = GeckoViewInsetRules.resolve(
                safeArea = safeArea,
                forceNativeSafeArea = true,
                forceNativeTopSafeArea = false,
                isFullscreenContent = false,
                isInsideSafeDrawingHost = false,
                keyboardBottomInsetPx = keyboardInset,
            )

            assertEquals(GeckoViewInsets(8, 72, 6, expectedBottom), layout.margins)
            assertEquals(GeckoViewInsets.Zero, layout.rendererSafeAreaOverride)
        }
    }

    @Test
    fun `fullscreen still reserves keyboard space for focused content`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(0, 72, 0, 48),
            forceNativeSafeArea = true,
            forceNativeTopSafeArea = false,
            isFullscreenContent = true,
            isInsideSafeDrawingHost = false,
            keyboardBottomInsetPx = 600,
        )

        assertEquals(GeckoViewInsets(0, 0, 0, 600), layout.margins)
        assertEquals(GeckoViewInsets(0, 72, 0, 0), layout.rendererSafeAreaOverride)
    }

    @Test
    fun `safe drawing host already owns keyboard space`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(0, 72, 0, 48),
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = true,
            keyboardBottomInsetPx = 600,
        )

        assertEquals(GeckoViewInsets.Zero, layout.margins)
        assertEquals(GeckoViewInsets.Zero, layout.rendererSafeAreaOverride)
    }

    @Test
    fun `system webview retains document top inset and other renderer edges`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 8, top = 72, right = 6, bottom = 48),
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
            useNativeCssSafeArea = false,
        )

        assertEquals(GeckoViewInsets.Zero, layout.margins)
        assertEquals(GeckoViewInsets(left = 8, top = 0, right = 6, bottom = 48), layout.rendererSafeAreaOverride)
        assertEquals(72, layout.scrollableTopInsetPx)
    }

    @Test
    fun `edges that move the page become native margins so touches stay under the finger`() {
        // #123, H4: with the top safe area in the renderer, GeckoView drew the page below the
        // status bar but placed taps a status bar higher than the finger.
        val safeArea = GeckoViewInsets(left = 8, top = 96, right = 6, bottom = 34)
        val layout = GeckoViewInsetRules.resolve(
            safeArea = safeArea,
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
        )

        assertEquals(GeckoViewInsets(left = 8, top = 96, right = 6, bottom = 0), layout.margins)
        assertEquals(GeckoViewInsets(left = 0, top = 0, right = 0, bottom = 34), layout.rendererSafeAreaOverride)
        assertEquals(0, layout.scrollableTopInsetPx)
        assertEquals(WebContentTopInsetTransitionState.Document, layout.topInsetTransitionState)
    }

    @Test
    fun `gesture navigation safe area stays with the edge to edge renderer`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 0, top = 72, right = 0, bottom = 34),
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
        )

        assertEquals(
            GeckoViewInsets(left = 0, top = 72, right = 0, bottom = 0),
            layout.margins,
        )
        assertEquals(
            GeckoViewInsets(left = 0, top = 0, right = 0, bottom = 34),
            layout.rendererSafeAreaOverride,
        )
        assertEquals(0, layout.scrollableTopInsetPx)
    }

    @Test
    fun `three button navigation also remains edge to edge at the bottom`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 0, top = 72, right = 0, bottom = 48),
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
        )

        assertEquals(
            GeckoViewInsets(left = 0, top = 72, right = 0, bottom = 0),
            layout.margins,
        )
        assertEquals(
            GeckoViewInsets(left = 0, top = 0, right = 0, bottom = 48),
            layout.rendererSafeAreaOverride,
        )
        assertEquals(0, layout.scrollableTopInsetPx)
    }

    @Test
    fun `forced safe area uses native margins and clears renderer insets`() {
        val safeArea = GeckoViewInsets(left = 8, top = 72, right = 6, bottom = 48)

        val layout = GeckoViewInsetRules.resolve(
            safeArea = safeArea,
            forceNativeSafeArea = true,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
        )

        assertEquals(safeArea, layout.margins)
        assertEquals(GeckoViewInsets.Zero, layout.rendererSafeAreaOverride)
    }

    @Test
    fun `automatic fallback keeps top and sides in native margins`() {
        val safeArea = GeckoViewInsets(left = 8, top = 72, right = 6, bottom = 48)

        val layout = GeckoViewInsetRules.resolve(
            safeArea = safeArea,
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = true,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
        )

        assertEquals(
            GeckoViewInsets(left = 8, top = 72, right = 6, bottom = 0),
            layout.margins,
        )
        assertEquals(
            GeckoViewInsets(left = 0, top = 0, right = 0, bottom = 48),
            layout.rendererSafeAreaOverride,
        )
        assertEquals(0, layout.scrollableTopInsetPx)
        assertEquals(WebContentTopInsetTransitionState.Other, layout.topInsetTransitionState)
    }

    @Test
    fun `website header marks native top margin as smooth transition owner`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 8, top = 72, right = 6, bottom = 48),
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = true,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
            nativeTopHeaderSafeArea = true,
        )

        assertEquals(WebContentTopInsetTransitionState.WebContentHeader, layout.topInsetTransitionState)
    }

    @Test
    fun `fullscreen suppresses website header transition ownership`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 8, top = 72, right = 6, bottom = 48),
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = true,
            isFullscreenContent = true,
            isInsideSafeDrawingHost = false,
            nativeTopHeaderSafeArea = true,
        )

        assertEquals(WebContentTopInsetTransitionState.Other, layout.topInsetTransitionState)
    }

    @Test
    fun `forced native safe area rejects negative synthetic inset values`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = -8, top = -1, right = -6, bottom = -48),
            forceNativeSafeArea = true,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
        )

        assertEquals(GeckoViewInsets.Zero, layout.margins)
        assertEquals(GeckoViewInsets.Zero, layout.rendererSafeAreaOverride)
    }

    @Test
    fun `fullscreen keeps renderer edge to edge despite site native override`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 18, top = 72, right = 6, bottom = 48),
            forceNativeSafeArea = true,
            forceNativeTopSafeArea = false,
            isFullscreenContent = true,
            isInsideSafeDrawingHost = false,
        )

        assertEquals(GeckoViewInsets.Zero, layout.margins)
        assertEquals(null, layout.rendererSafeAreaOverride)
        assertEquals(0, layout.scrollableTopInsetPx)
    }

    @Test
    fun `safe drawing host owns safe area without a second renderer inset`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 18, top = 72, right = 6, bottom = 48),
            forceNativeSafeArea = true,
            forceNativeTopSafeArea = false,
            isFullscreenContent = true,
            isInsideSafeDrawingHost = true,
        )

        assertEquals(GeckoViewInsets.Zero, layout.margins)
        assertEquals(GeckoViewInsets.Zero, layout.rendererSafeAreaOverride)
        assertEquals(0, layout.scrollableTopInsetPx)
    }

    @Test
    fun `framed host removes the safe area it already keeps clear of`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 0, top = 72, right = 0, bottom = 48),
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
            hostFrame = GeckoViewInsets(left = 18, top = 72, right = 18, bottom = 252),
        )

        assertEquals(GeckoViewInsets.Zero, layout.margins)
        assertEquals(GeckoViewInsets.Zero, layout.rendererSafeAreaOverride)
        assertEquals(0, layout.scrollableTopInsetPx)
    }

    @Test
    fun `framed host keeps a display cutout wider than the frame`() {
        val layout = GeckoViewInsetRules.resolve(
            safeArea = GeckoViewInsets(left = 96, top = 72, right = 0, bottom = 48),
            forceNativeSafeArea = true,
            forceNativeTopSafeArea = false,
            isFullscreenContent = false,
            isInsideSafeDrawingHost = false,
            hostFrame = GeckoViewInsets(left = 18, top = 72, right = 18, bottom = 252),
        )

        assertEquals(GeckoViewInsets(left = 78, top = 0, right = 0, bottom = 0), layout.margins)
    }

    @Test
    fun `framed host reserves only the keyboard part that covers the card`() {
        val frame = GeckoViewInsets(left = 18, top = 72, right = 18, bottom = 252)
        for ((keyboardInset, expectedBottom) in listOf(900 to 648, 200 to 0)) {
            val layout = GeckoViewInsetRules.resolve(
                safeArea = GeckoViewInsets(left = 0, top = 72, right = 0, bottom = 48),
                forceNativeSafeArea = false,
                forceNativeTopSafeArea = false,
                isFullscreenContent = false,
                isInsideSafeDrawingHost = false,
                keyboardBottomInsetPx = keyboardInset,
                hostFrame = frame,
            )

            assertEquals(GeckoViewInsets(0, 0, 0, expectedBottom), layout.margins)
        }
    }

    @Test
    fun `fullscreen content ignores the host frame`() {
        val safeArea = GeckoViewInsets(left = 0, top = 72, right = 0, bottom = 48)
        val framed = GeckoViewInsetRules.resolve(
            safeArea = safeArea,
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = true,
            isInsideSafeDrawingHost = false,
            hostFrame = GeckoViewInsets(left = 18, top = 72, right = 18, bottom = 252),
        )
        val unframed = GeckoViewInsetRules.resolve(
            safeArea = safeArea,
            forceNativeSafeArea = false,
            forceNativeTopSafeArea = false,
            isFullscreenContent = true,
            isInsideSafeDrawingHost = false,
        )

        assertEquals(unframed, framed)
    }
}
