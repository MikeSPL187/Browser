package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.mutableStateMapOf

/**
 * The colors each tab's page reports for the browser's chrome: the status bar backdrop under a
 * fixed page header, and the page's theme-color for the v5 bar (system-v5.md, section 1.3).
 *
 * A new document drops the backdrop at once but keeps the theme-color until the new page reports
 * its own (it always does once its head is parsed, null when it has none): clearing it at the
 * start of every navigation would flash the neutral bar between two pages of one colored site.
 */
internal class WebContentPageColors {
    private val statusBarBackdrops = mutableStateMapOf<String, WebContentStatusBarAppearance>()
    private val themeColors = mutableStateMapOf<String, Int>()

    fun statusBarBackdrop(tabId: String): WebContentStatusBarAppearance? = statusBarBackdrops[tabId]

    /** The page's theme-color as an opaque ARGB color, or null when it declares none. */
    fun themeColor(tabId: String): Int? = themeColors[tabId]

    fun onStatusBarBackdrop(tabId: String, reportedColor: String?) {
        val appearance = WebContentStatusBarAppearanceRules.fromReportedColor(reportedColor)
        if (appearance == null) statusBarBackdrops.remove(tabId) else statusBarBackdrops[tabId] = appearance
    }

    fun onThemeColor(tabId: String, reportedColor: String?) {
        val color = WebContentStatusBarAppearanceRules.fromReportedColor(reportedColor)?.colorArgb
        if (color == null) themeColors.remove(tabId) else themeColors[tabId] = color
    }

    /** A same-document navigation keeps the theme-color; the page reports it again. */
    fun onSameDocumentNavigation(tabId: String) {
        statusBarBackdrops.remove(tabId)
    }

    /** A new document at [address]; only web pages report a theme-color, others drop it now. */
    fun onNavigation(tabId: String, address: String?) {
        statusBarBackdrops.remove(tabId)
        if (!WebPageColorRules.reportsThemeColor(address)) themeColors.remove(tabId)
    }

    fun clear(tabId: String) {
        statusBarBackdrops.remove(tabId)
        themeColors.remove(tabId)
    }

    fun clearAll() {
        statusBarBackdrops.clear()
        themeColors.clear()
    }
}

internal object WebPageColorRules {
    /** The built-in extension's content scripts run on http and https pages only. */
    fun reportsThemeColor(address: String?): Boolean =
        address != null && (address.startsWith("https://", ignoreCase = true) ||
            address.startsWith("http://", ignoreCase = true))
}
