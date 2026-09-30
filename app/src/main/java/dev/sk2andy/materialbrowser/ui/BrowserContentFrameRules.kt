package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserContentFrame
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import kotlin.math.roundToInt

/**
 * Where the page card of the framed shell sits in the window. The card keeps clear of the system
 * bars, leaves a thin aura gutter on the sides and stops just above the address bar; the Air style
 * and every fullscreen or chrome-less presentation keep the page edge to edge.
 */
internal object BrowserContentFrameRules {
    fun isFramed(
        chromeStyle: BrowserChromeStyle,
        browserChromeVisible: Boolean,
        isBlankPage: Boolean,
    ): Boolean = chromeStyle == BrowserChromeStyle.Frame && browserChromeVisible && !isBlankPage

    /**
     * @param safeLeftPx … [safeBottomPx] the window's safe drawing insets (system bars and cutouts).
     * @param addressBarReservePx the height of the address bar and its own bottom margin, or 0 when
     *   the bar does not sit under the page (docked to a side edge).
     */
    fun resolve(
        framed: Boolean,
        safeLeftPx: Int,
        safeTopPx: Int,
        safeRightPx: Int,
        safeBottomPx: Int,
        sideGutterPx: Float,
        barGapPx: Float,
        addressBarReservePx: Float,
    ): BrowserContentFrame {
        if (!framed) return BrowserContentFrame.None
        val gutter = sideGutterPx.coerceAtLeast(0f).roundToInt()
        val reserve = addressBarReservePx.coerceAtLeast(0f)
        val bottomGap = if (reserve > 0f) barGapPx.coerceAtLeast(0f) else sideGutterPx.coerceAtLeast(0f)
        return BrowserContentFrame(
            leftPx = safeLeftPx.coerceAtLeast(0) + gutter,
            // No gutter under the status bar: the card starts right below it, as on the boards.
            topPx = maxOf(safeTopPx, gutter),
            rightPx = safeRightPx.coerceAtLeast(0) + gutter,
            bottomPx = safeBottomPx.coerceAtLeast(0) + (reserve + bottomGap).roundToInt(),
        )
    }
}
