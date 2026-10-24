package dev.sk2andy.materialbrowser.browser

/**
 * Choosing distracting items on a tab's page to hide (v5 V20, after Safari). While choosing, a
 * tap outlines a block of the page and a second tap hides it; the page's own taps, links and
 * forms get nothing, scrolling still works. Each hidden block comes back as a CSS selector for the
 * page's site, which [HiddenDistractionsController] keeps and sends back with every page policy.
 *
 * GeckoView implements it with Vola's built-in extension. System WebView keeps [Unavailable]; the
 * UI hides the feature by [AndroidBrowserEngineCapabilities.hideDistractions].
 */
internal interface PageDistractionsPort {
    /**
     * Starts choosing on the current page, outlined in [highlightArgb]; false when the page cannot
     * (not loaded yet, closed, or the engine has no such mode). Choosing ends by [stopPicking] or
     * when the tab goes to another page, which [PageDistractionsListener.onPickingEnded] reports.
     */
    fun startPicking(highlightArgb: Int, listener: PageDistractionsListener): Boolean

    fun stopPicking()

    /**
     * Answers the choice card for the outlined block (canvas screen «HideMode»): hide it, take the
     * larger block around it, or let it go. Nothing happens when nothing is outlined.
     */
    fun act(action: DistractionAction) = Unit

    /** The engine without the mode (System WebView). */
    object Unavailable : PageDistractionsPort {
        override fun startPicking(highlightArgb: Int, listener: PageDistractionsListener) = false

        override fun stopPicking() = Unit
    }
}

/** What choosing reports; both run on the main thread. */
internal interface PageDistractionsListener {
    /** A block of the page on [pageHost] was hidden; [selector] finds it again. */
    fun onHidden(pageHost: String, selector: String)

    /**
     * A tap outlined a block, which a second tap hides ([outlined]), or outlined nothing; [larger]
     * says a larger block is around it, for «Choose larger».
     */
    fun onOutlined(outlined: Boolean, larger: Boolean = false) = Unit

    /** The page left choosing by itself: it navigated, or its tab closed. */
    fun onPickingEnded()
}

/** What the choice card asks for the outlined block. */
internal enum class DistractionAction { Hide, Larger, Cancel }
