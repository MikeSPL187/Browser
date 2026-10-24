package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5LinkGlanceRulesTest {
    private val rest = Rect(16f, 92f, 374f, 640f)

    @Test
    fun `the card starts small at the touch point and settles at rest`() {
        val touch = Offset(100f, 500f)
        val start = V5LinkGlanceRules.flight(touch, rest, progress = 0f, originSizePx = 35.8f, arcPx = 20f)
        assertEquals(0.1f, start.scale, 0.001f)
        assertEquals(touch.x - rest.center.x, start.dx, 0.001f)
        assertEquals(touch.y - rest.center.y, start.dy, 0.001f)

        val end = V5LinkGlanceRules.flight(touch, rest, progress = 1f, originSizePx = 35.8f, arcPx = 20f)
        assertEquals(1f, end.scale, 0.001f)
        assertEquals(0f, end.dx, 0.001f)
        assertEquals(0f, end.dy, 0.001f)
    }

    @Test
    fun `halfway the card is lifted by the whole arc`() {
        val touch = Offset(rest.center.x, rest.center.y)
        val half = V5LinkGlanceRules.flight(touch, rest, progress = 0.5f, originSizePx = 0f, arcPx = 20f)
        assertEquals(-20f, half.dy, 0.001f)
    }

    @Test
    fun `without a touch point the card grows from its own middle`() {
        val start = V5LinkGlanceRules.flight(null, rest, progress = 0f, originSizePx = 35.8f, arcPx = 20f)
        assertEquals(0f, start.dx, 0.001f)
        assertEquals(0f, start.dy, 0.001f)
    }

    @Test
    fun `the page shrinks to 0_97 about its center`() {
        assertEquals(1f, V5LinkGlanceRules.pageScale(0f), 0.0001f)
        assertEquals(0.97f, V5LinkGlanceRules.pageScale(1f), 0.0001f)
        // A spring's overshoot never shrinks the page further.
        assertEquals(0.97f, V5LinkGlanceRules.pageScale(1.2f), 0.0001f)
        val page = V5LinkGlanceRules.scaledAboutCenter(Rect(0f, 0f, 400f, 800f), 0.5f)
        assertEquals(Rect(100f, 200f, 300f, 600f), page)
    }

    @Test
    fun `opening grows the card into the window`() {
        val window = Rect(0f, 0f, 390f, 844f)
        assertEquals(rest, V5LinkGlanceRules.opening(rest, window, 0f))
        assertEquals(window, V5LinkGlanceRules.opening(rest, window, 1f))
    }

    @Test
    fun `the page comes in over the flight after the card leaves the link`() {
        assertEquals(0f, V5LinkGlanceRules.pageAlpha(0f), 0.0001f)
        assertEquals(0f, V5LinkGlanceRules.pageAlpha(V5LinkGlanceRules.PAGE_FADE_FROM), 0.0001f)
        assertEquals(0.5f, V5LinkGlanceRules.pageAlpha(0.65f), 0.0001f)
        assertEquals(1f, V5LinkGlanceRules.pageAlpha(1.05f), 0.0001f)
    }

    @Test
    fun `the buttons come in over the second half`() {
        assertEquals(0f, V5LinkGlanceRules.actionsAlpha(0.4f), 0.0001f)
        assertEquals(0.5f, V5LinkGlanceRules.actionsAlpha(0.75f), 0.0001f)
        assertEquals(1f, V5LinkGlanceRules.actionsAlpha(1.1f), 0.0001f)
    }

    @Test
    fun `the address line is host and path without query or www`() {
        assertEquals("polar-notes.ru/prognoz", V5LinkGlanceRules.address("https://www.polar-notes.ru/prognoz/?utm_source=x#top"))
        assertEquals("example.com", V5LinkGlanceRules.address("https://example.com/"))
        assertEquals("xn--80ak6aa92e.com/login", V5LinkGlanceRules.address("https://xn--80ak6aa92e.com/login"))
        assertEquals("not a url", V5LinkGlanceRules.address("not a url"))
    }

    @Test
    fun `only https is secure`() {
        assertTrue(V5LinkGlanceRules.secure("https://example.com"))
        assertFalse(V5LinkGlanceRules.secure("http://example.com"))
        assertFalse(V5LinkGlanceRules.secure("::"))
    }

    @Test
    fun `a swipe up opens, down closes, a short slow one settles back`() {
        val swipe = { offset: Float, velocity: Float -> V5LinkGlanceRules.swipe(offset, velocity, commitPx = 100f, minVelocityPx = 1000f) }
        assertEquals(V5LinkGlanceRules.Swipe.Open, swipe(-120f, 0f))
        assertEquals(V5LinkGlanceRules.Swipe.Close, swipe(120f, 0f))
        assertEquals(V5LinkGlanceRules.Swipe.Open, swipe(-20f, -1500f))
        assertEquals(V5LinkGlanceRules.Swipe.Close, swipe(20f, 1500f))
        assertEquals(V5LinkGlanceRules.Swipe.Stay, swipe(-40f, -200f))
        // A fling back against the pull does not commit the other way.
        assertEquals(V5LinkGlanceRules.Swipe.Stay, swipe(30f, -1500f))
    }
}
