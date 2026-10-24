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

    @Test
    fun `the host after the at sign is the one shown, never the name before it`() {
        assertEquals("evil.com", v5BarDomain("https://good.com@evil.com/"))
    }

    @Test
    fun `a query or a fragment right after the host ends it`() {
        assertEquals("example.com", v5BarDomain("https://example.com?x=1"))
        assertEquals("example.com", v5BarDomain("https://example.com#top"))
        assertEquals("example.com", v5BarDomain("https://WWW.EXAMPLE.COM"))
    }

    @Test
    fun `an empty or bare host shows nothing`() {
        assertNull(v5BarDomain("https://"))
        assertNull(v5BarDomain("https:///x"))
        assertNull(v5BarDomain("http://[]/"))
        assertNull(v5BarDomain("https://www./"))
    }

    @Test
    fun `a backslash ends the host as a browser reads it, so the name after it is never shown`() {
        assertEquals("evil.com", v5BarDomain("https://evil.com\\@good.com/"))
        assertEquals("evil.com", v5BarDomain("http://evil.com\\path"))
    }

    @Test
    fun `an address inside another scheme is not the page`() {
        assertNull(v5BarDomain("view-source:https://example.com"))
        assertNull(v5BarDomain("javascript:alert('https://example.com')"))
    }

    @Test
    fun `the closing dot of a host is dropped`() {
        assertEquals("example.com", v5BarDomain("https://example.com./"))
    }

    @Test
    fun `a look-alike international name shows in punycode, a genuine one in its letters`() {
        assertEquals("xn--80ak6aa92e.com", v5BarDomain("https://xn--80ak6aa92e.com/"))
        assertEquals("xn--80ak6aa92e.com", v5BarDomain("https://аррӏе.com/"))
        assertEquals("пример.рф", v5BarDomain("https://xn--e1afmkfd.xn--p1ai/"))
    }
}
