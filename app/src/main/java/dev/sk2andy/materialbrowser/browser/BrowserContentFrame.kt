package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.Immutable

/**
 * How far the page host sits inside the window on each edge, in pixels. The framed browser chrome
 * shows the page as a card on the workspace aura; the engines subtract these edges from the safe
 * area and the keyboard inset they report to the page, so a site never pads for space the card
 * already keeps clear of.
 */
/**
 * An engine view that lets a bottom toolbar cover part of the page without resizing it: the
 * page keeps its layout and only moves bottom-fixed elements above the covered part. Engines
 * without such an API do not implement it, and the browser keeps the card at its smaller size.
 */
internal interface BrowserDynamicToolbarHost {
    /**
     * @param maxHeightPx how far the toolbar can reach into the page, 0 when it never does.
     * @param coveredPx how much of the page bottom it covers now, from 0 to [maxHeightPx].
     */
    fun updateDynamicToolbar(maxHeightPx: Int, coveredPx: Int)
}

@Immutable
data class BrowserContentFrame(
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int,
) {
    companion object {
        /** The page fills the window edge to edge. */
        val None = BrowserContentFrame(leftPx = 0, topPx = 0, rightPx = 0, bottomPx = 0)
    }
}
