package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.mutableStateMapOf

/**
 * What each tab's page reports for the browser's chrome: the status bar backdrop under a fixed page
 * header, the page's theme-color for the v5 bar (system-v5.md, section 1.3), and whether the page
 * reads as an article, which puts «Read» in the v5 capsule (section 5.1).
 *
 * A new document drops the backdrop at once but keeps the theme-color until the new page reports
 * its own (it always does once its head is parsed, null when it has none): clearing it at the
 * start of every navigation would flash the neutral bar between two pages of one colored site.
 */
internal class WebContentPageSignals {
    private val statusBarBackdrops = mutableStateMapOf<String, WebContentStatusBarAppearance>()
    private val themeColors = mutableStateMapOf<String, Int>()
    private val readableTabs = mutableStateMapOf<String, Boolean>()

    fun statusBarBackdrop(tabId: String): WebContentStatusBarAppearance? = statusBarBackdrops[tabId]

    /** The page's theme-color as an opaque ARGB color, or null when it declares none. */
    fun themeColor(tabId: String): Int? = themeColors[tabId]

    /** Whether the page reads as an article; false until the page has said so. */
    fun readerAvailable(tabId: String): Boolean = readableTabs[tabId] == true

    fun onReaderAvailable(tabId: String, available: Boolean) {
        if (available) readableTabs[tabId] = true else readableTabs.remove(tabId)
    }

    fun onStatusBarBackdrop(tabId: String, reportedColor: String?) {
        val appearance = WebContentStatusBarAppearanceRules.fromReportedColor(reportedColor)
        if (appearance == null) statusBarBackdrops.remove(tabId) else statusBarBackdrops[tabId] = appearance
    }

    fun onThemeColor(tabId: String, reportedColor: String?) {
        val color = WebContentStatusBarAppearanceRules.fromReportedColor(reportedColor)?.colorArgb
        if (color == null) themeColors.remove(tabId) else themeColors[tabId] = color
    }

    /**
     * A same-document navigation keeps the theme-color, which the page reports again, and the reader
     * answer: an app-like site that swaps its article in place still holds one.
     */
    fun onSameDocumentNavigation(tabId: String) {
        statusBarBackdrops.remove(tabId)
    }

    /** A new document at [address]; only web pages report a theme-color, others drop it now. */
    fun onNavigation(tabId: String, address: String?) {
        statusBarBackdrops.remove(tabId)
        readableTabs.remove(tabId)
        if (!WebPageColorRules.reportsThemeColor(address)) themeColors.remove(tabId)
    }

    fun clear(tabId: String) {
        statusBarBackdrops.remove(tabId)
        themeColors.remove(tabId)
        readableTabs.remove(tabId)
    }

    fun clearAll() {
        statusBarBackdrops.clear()
        themeColors.clear()
        readableTabs.clear()
    }
}

internal object WebPageColorRules {
    /** The built-in extension's content scripts run on http and https pages only. */
    fun reportsThemeColor(address: String?): Boolean =
        address != null && (address.startsWith("https://", ignoreCase = true) ||
            address.startsWith("http://", ignoreCase = true))
}
