package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserContentFrame
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserContentFrameRulesTest {
    @Test
    fun `only the frame style with visible chrome and a page frames the page`() {
        assertTrue(framed())
        assertFalse(framed(chromeStyle = BrowserChromeStyle.Air))
        assertFalse(framed(browserChromeVisible = false))
        assertFalse(framed(isBlankPage = true))
    }

    @Test
    fun `an unframed page starts below the status bar, moved by the window layout`() {
        assertEquals(
            BrowserContentFrame(leftPx = 0, topPx = 128, rightPx = 0, bottomPx = 0),
            BrowserContentFrameRules.belowStatusBar(safeTopPx = 128),
        )
        assertEquals(BrowserContentFrame.None, BrowserContentFrameRules.belowStatusBar(safeTopPx = 0))
    }

    @Test
    fun `compact mode lets the page fill the screen even in the frame style`() {
        assertFalse(framed(compactMode = true))
    }

    @Test
    fun `card sits below the status bar with side gutters and above the address bar`() {
        // 3x density: 6 dp gutter, 8 dp gap, 56 dp bar + 12 dp margin.
        val frame = resolve(addressBarReservePx = 204f)

        assertEquals(
            BrowserContentFrame(leftPx = 18, topPx = 72, rightPx = 18, bottomPx = 48 + 204 + 24),
            frame,
        )
    }

    @Test
    fun `docked address bar leaves only a gutter below the card`() {
        val frame = resolve(addressBarReservePx = 0f)

        assertEquals(48 + 18, frame.bottomPx)
    }

    @Test
    fun `display cutouts widen the side the camera is on`() {
        val frame = resolve(safeLeftPx = 96, addressBarReservePx = 204f)

        assertEquals(96 + 18, frame.leftPx)
        assertEquals(18, frame.rightPx)
    }

    @Test
    fun `hidden status bar still keeps a gutter above the card`() {
        assertEquals(18, resolve(safeTopPx = 0, addressBarReservePx = 204f).topPx)
    }

    @Test
    fun `unframed pages stay edge to edge`() {
        assertEquals(
            BrowserContentFrame.None,
            resolve(framed = false, addressBarReservePx = 204f),
        )
    }

    private fun framed(
        chromeStyle: BrowserChromeStyle = BrowserChromeStyle.Frame,
        browserChromeVisible: Boolean = true,
        isBlankPage: Boolean = false,
        compactMode: Boolean = false,
    ) = BrowserContentFrameRules.isFramed(chromeStyle, browserChromeVisible, isBlankPage, compactMode)

    private fun resolve(
        framed: Boolean = true,
        safeLeftPx: Int = 0,
        safeTopPx: Int = 72,
        addressBarReservePx: Float,
    ) = BrowserContentFrameRules.resolve(
        framed = framed,
        safeLeftPx = safeLeftPx,
        safeTopPx = safeTopPx,
        safeRightPx = 0,
        safeBottomPx = 48,
        sideGutterPx = 18f,
        barGapPx = 24f,
        addressBarReservePx = addressBarReservePx,
    )
}
