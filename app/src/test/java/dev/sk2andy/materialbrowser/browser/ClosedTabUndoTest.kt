package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.data.ClosedTabUndoRules
import dev.sk2andy.materialbrowser.data.ClosedTabUndoToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ClosedTabUndoTest {
    private val scheduled = mutableListOf<Pair<Runnable, Long>>()
    private var expired = 0
    private val undo = ClosedTabUndo<String>(
        postDelayed = { task, delayMillis -> scheduled += task to delayMillis },
        removeCallbacks = { task -> scheduled.removeAll { it.first === task } },
        onExpired = { expired++ },
    )

    private fun token(tabId: String) = ClosedTabUndoToken(
        tab = BrowserTab(id = tabId, lastAccessedAt = 1, profileId = "home"),
        originalIndex = 0,
        wasSelected = true,
        selectedTabIdAfterClose = "other",
        replacementTabId = null,
        closedAtMillis = 1,
    )

    private val snapshot = ClosedTabUndo.Snapshot<String>(
        trail = null,
        preview = "preview",
        favicon = "favicon",
        stack = null,
        stackAfterClose = null,
    )

    @Test
    fun aKeptTabIsOfferedUntilItsTimeRunsOut() {
        val token = token("a")

        undo.keep(token, snapshot)

        assertEquals(token, undo.offer)
        assertSame(snapshot, undo.snapshotFor(token))
        assertEquals(ClosedTabUndoRules.DURATION_MILLIS, scheduled.single().second)

        scheduled.single().first.run()
        assertEquals(1, expired)
    }

    @Test
    fun anotherTokenGetsNothing() {
        undo.keep(token("a"), snapshot)

        assertNull(undo.snapshotFor(token("b")))
    }

    @Test
    fun clearingDropsTheOfferAndItsTimer() {
        val token = token("a")
        undo.keep(token, snapshot)

        undo.clear()

        assertNull(undo.offer)
        assertNull(undo.snapshotFor(token))
        assertTrue(scheduled.isEmpty())
    }
}
