package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V5BottomBarTest {
    @Test
    fun `the bar shows the host without scheme, www and path`() {
        assertEquals("polar-notes.ru", v5BarDomain("https://www.polar-notes.ru/recipes?id=3#top"))
        assertEquals("news.example.com", v5BarDomain("http://user:pass@News.Example.com:8080/a"))
    }

    @Test
    fun `an IPv6 host keeps its brackets`() {
        assertEquals("[::1]", v5BarDomain("http://[::1]:8080/"))
    }

    @Test
    fun `addresses without a host show nothing`() {
        assertNull(v5BarDomain("about:blank"))
        assertNull(v5BarDomain("file:///sdcard/page.html"))
    }
}
