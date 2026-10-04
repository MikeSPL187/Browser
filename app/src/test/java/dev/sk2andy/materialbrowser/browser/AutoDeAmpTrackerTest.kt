package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoDeAmpTrackerTest {
    private var now = 1_000L
    private val tracker = AutoDeAmpTracker(now = { now })

    @Test
    fun `only the newest request of a tab is current`() {
        val first = tracker.nextRequest("tab")
        val second = tracker.nextRequest("tab")
        assertFalse(tracker.isLatestRequest("tab", first))
        assertTrue(tracker.isLatestRequest("tab", second))
    }

    @Test
    fun `the same publisher page right after a replacement is a loop until the guard expires`() {
        tracker.remember("tab", "https://news.example/a")
        assertTrue(tracker.isLooping("tab", "https://news.example/a"))
        assertFalse(tracker.isLooping("tab", "https://news.example/b"))
        now += AutoDeAmpTracker.LOOP_GUARD_MILLIS + 1
        assertFalse(tracker.isLooping("tab", "https://news.example/a"))
    }

    @Test
    fun `a closed tab is forgotten`() {
        val generation = tracker.nextRequest("tab")
        tracker.remember("tab", "https://news.example/a")
        tracker.forget("tab")
        assertFalse(tracker.isLatestRequest("tab", generation))
        assertFalse(tracker.isLooping("tab", "https://news.example/a"))
    }
}
