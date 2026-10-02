package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.gecko.BrowserEngineFindPort
import dev.sk2andy.materialbrowser.browser.gecko.GeckoFindResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FindInPageControllerTest {
    private val counted = GeckoFindResult(activeMatchOrdinal = 0, matchCount = 541, isDoneCounting = true)
    private val uncounted = GeckoFindResult(
        activeMatchOrdinal = 0,
        matchCount = 0,
        isDoneCounting = true,
        found = true,
    )

    @Test
    fun `an uncounted match is asked again, back and forward, until the count arrives`() {
        val engine = FakeFindPort(uncounted, uncounted, counted)
        val scheduled = mutableListOf<Runnable>()
        val controller = controller(engine, scheduled)

        controller.updateQuery("Zen")
        assertFalse(controller.state!!.isDoneCounting)
        assertEquals(1, scheduled.size)

        scheduled.removeAt(0).run()

        assertEquals(listOf(true, false, true), engine.directions)
        assertEquals(541, controller.state!!.matchCount)
        assertTrue(controller.state!!.isDoneCounting)
        assertTrue(scheduled.isEmpty())
    }

    @Test
    fun `after the last recount the match shows without a total`() {
        val engine = FakeFindPort(uncounted)
        val scheduled = mutableListOf<Runnable>()
        val controller = controller(engine, scheduled)

        controller.updateQuery("Zen")
        repeat(FindInPageRules.MAX_RECOUNTS) { scheduled.removeAt(0).run() }

        val position = FindInPageRules.displayPosition(controller.state!!)
        assertTrue(controller.state!!.isDoneCounting)
        assertFalse(position.isCountKnown)
        assertEquals(1, position.activeMatchNumber)
        assertTrue(scheduled.isEmpty())
    }

    @Test
    fun `typing again drops the pending recount and stale results`() {
        val engine = FakeFindPort(uncounted, counted)
        val scheduled = mutableListOf<Runnable>()
        val removed = mutableListOf<Runnable>()
        val controller = controller(engine, scheduled, removed)

        controller.updateQuery("Ze")
        controller.updateQuery("Zen")

        assertEquals(1, removed.size)
        assertEquals("Zen", controller.state!!.query)
        assertEquals(541, controller.state!!.matchCount)
    }

    @Test
    fun `a page that is no longer on screen gets no result`() {
        val engine = FakeFindPort(counted)
        val controller = FindInPageController(
            host = { false },
            postDelayed = { _, _ -> },
            removeCallbacks = {},
        )
        controller.open("tab", engine, navigationGeneration = 0, resetOptions = true)

        controller.updateQuery("Zen")

        assertEquals(0, controller.state!!.matchCount)
        controller.close()
        assertNull(controller.state)
        assertTrue(engine.cleared)
    }

    private fun controller(
        engine: FakeFindPort,
        scheduled: MutableList<Runnable>,
        removed: MutableList<Runnable> = mutableListOf(),
    ) = FindInPageController(
        host = { true },
        postDelayed = { runnable, _ -> scheduled += runnable },
        removeCallbacks = { runnable -> removed += runnable },
    ).apply { open("tab", engine, navigationGeneration = 0, resetOptions = true) }

    /** Answers each search with the next result; the last one repeats. */
    private class FakeFindPort(private vararg val results: GeckoFindResult) : BrowserEngineFindPort {
        val directions = mutableListOf<Boolean>()
        var cleared = false

        override fun findInPage(
            query: String,
            forward: Boolean,
            onComplete: (GeckoFindResult?) -> Unit,
        ) {
            directions += forward
            onComplete(results[minOf(directions.size, results.size) - 1])
        }

        override fun clearFindInPage() {
            cleared = true
        }
    }
}
