package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebContentPageColorsTest {
    private val colors = WebContentPageColors()

    @Test
    fun `the reported theme-color becomes an opaque color and none clears it`() {
        colors.onThemeColor(TAB, "#1da1f2")
        assertEquals(0xFF1DA1F2.toInt(), colors.themeColor(TAB))
        colors.onThemeColor(TAB, null)
        assertNull(colors.themeColor(TAB))
        colors.onThemeColor(TAB, "red")
        assertNull(colors.themeColor(TAB))
    }

    @Test
    fun `a new web page keeps the theme-color until it reports its own`() {
        colors.onThemeColor(TAB, "#1f4e5f")
        colors.onStatusBarBackdrop(TAB, "#1f4e5f")
        colors.onNavigation(TAB, "https://example.com/next")
        assertEquals(0xFF1F4E5F.toInt(), colors.themeColor(TAB))
        assertNull(colors.statusBarBackdrop(TAB))
    }

    @Test
    fun `a page without the content scripts drops the theme-color at once`() {
        colors.onThemeColor(TAB, "#1f4e5f")
        colors.onNavigation(TAB, "about:reader?url=x")
        assertNull(colors.themeColor(TAB))
        colors.onThemeColor(TAB, "#1f4e5f")
        colors.onNavigation(TAB, null)
        assertNull(colors.themeColor(TAB))
    }

    @Test
    fun `a same-document navigation keeps the theme-color and drops the backdrop`() {
        colors.onThemeColor(TAB, "#e50914")
        colors.onStatusBarBackdrop(TAB, "#e50914")
        colors.onSameDocumentNavigation(TAB)
        assertEquals(0xFFE50914.toInt(), colors.themeColor(TAB))
        assertNull(colors.statusBarBackdrop(TAB))
    }

    @Test
    fun `clearing a tab leaves other tabs`() {
        colors.onThemeColor(TAB, "#e50914")
        colors.onThemeColor(OTHER, "#34c759")
        colors.clear(TAB)
        assertNull(colors.themeColor(TAB))
        assertEquals(0xFF34C759.toInt(), colors.themeColor(OTHER))
        colors.clearAll()
        assertNull(colors.themeColor(OTHER))
    }

    @Test
    fun `only http and https pages report a theme-color`() {
        assertTrue(WebPageColorRules.reportsThemeColor("https://example.com/"))
        assertTrue(WebPageColorRules.reportsThemeColor("HTTP://example.com/"))
        assertFalse(WebPageColorRules.reportsThemeColor("about:blank"))
        assertFalse(WebPageColorRules.reportsThemeColor("file:///sdcard/a.html"))
        assertFalse(WebPageColorRules.reportsThemeColor(null))
    }

    private companion object {
        const val TAB = "tab"
        const val OTHER = "other"
    }
}
