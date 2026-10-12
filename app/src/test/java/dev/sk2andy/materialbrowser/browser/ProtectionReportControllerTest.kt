package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.data.ProtectionDay
import dev.sk2andy.materialbrowser.data.ProtectionReportPersistence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionReportControllerTest {
    private var today = 20_000L
    private val pending = mutableListOf<Runnable>()

    @Test
    fun `restore drops days that left the week`() {
        val store = FakeStore(days = listOf(ProtectionDay(today - 10, mapOf("old.com" to 5)), ProtectionDay(today, mapOf("a.com" to 2))))
        val controller = controller(store)

        controller.restore()

        assertEquals(2, controller.week.total)
        assertEquals(1, controller.week.siteCount)
    }

    @Test
    fun `restore removes expired days from the store as well`() {
        val store = FakeStore(days = listOf(ProtectionDay(today - 30, mapOf("old.example.com" to 5))))
        val controller = controller(store)

        controller.restore()
        controller.flush()

        assertEquals(1, store.saves)
        assertTrue(store.days.isEmpty())
    }

    @Test
    fun `restore does not rewrite a report that is still current`() {
        val store = FakeStore(days = listOf(ProtectionDay(today, mapOf("a.com" to 2))))
        val controller = controller(store)

        controller.restore()
        controller.refresh()

        assertEquals(0, store.saves)
    }

    @Test
    fun `a new day drops the days the report no longer keeps from the store`() {
        val store = FakeStore(
            days = listOf(ProtectionDay(today - 29, mapOf("old.com" to 4)), ProtectionDay(today, mapOf("a.com" to 2))),
        )
        val controller = controller(store)
        controller.restore()

        today += 1
        controller.refresh()

        assertEquals(1, store.saves)
        assertEquals(listOf(ProtectionDay(today - 1, mapOf("a.com" to 2))), store.days)
        assertEquals(2, controller.week.total)
    }

    @Test
    fun `record counts web pages only and saves once per burst`() {
        val store = FakeStore()
        val controller = controller(store)
        controller.restore()

        controller.record("https://www.news.example.com/article", 3)
        controller.record("https://news.example.com/other", 2)
        controller.record("about:blank", 9)

        assertEquals(5, controller.week.total)
        assertEquals(1, pending.size)
        assertEquals(0, store.saves)
        pending.single().run()
        assertEquals(1, store.saves)
        assertEquals(mapOf("news.example.com" to 5), store.days.single().blockedBySite)
    }

    @Test
    fun `blocked requests of a tab reach the report with their trackers`() {
        val store = FakeStore()
        val controller = controller(store)
        controller.restore()

        controller.noteBlocked("tab", "https://cdn.ads.example.net/pixel.gif")
        controller.noteBlocked("tab", "https://static.ads.example.net/a.js")
        controller.noteBlocked("tab", "https://tracker.io/t")
        controller.record("tab", "https://news.example.com/story", 3, isPrivate = false, workspaceProtected = false)

        val report = controller.summary(30)
        assertEquals(3, report.total)
        assertEquals(listOf("example.net" to 2, "tracker.io" to 1), report.topTrackers.map { it.host to it.blocked })
        assertEquals(2, report.sitesWithMostTrackers.single().trackerCount)
    }

    @Test
    fun `private tabs, locked workspaces and forgotten tabs leave no trackers behind`() {
        val controller = controller(FakeStore())
        controller.restore()

        controller.noteBlocked("private", "https://ads.example.net/a.js")
        controller.record("private", "https://news.example.com/", 1, isPrivate = true, workspaceProtected = false)
        controller.noteBlocked("closed", "https://ads.example.net/a.js")
        controller.forgetTab("closed")
        controller.noteBlocked("locked", "https://ads.example.net/a.js")
        controller.record("locked", "https://news.example.com/", 1, isPrivate = false, workspaceProtected = true)
        controller.record("closed", "https://news.example.com/", 1, isPrivate = false, workspaceProtected = false)

        val report = controller.summary(30)
        assertEquals(1, report.total)
        assertEquals(0, report.trackerCount)
    }

    @Test
    fun `refresh moves the week on to a new day`() {
        val controller = controller(FakeStore(days = listOf(ProtectionDay(today, mapOf("a.com" to 2)))))
        controller.restore()

        today += 7
        controller.refresh()

        assertEquals(0, controller.week.total)
    }

    @Test
    fun `clear empties the report and the store`() {
        val store = FakeStore(days = listOf(ProtectionDay(today, mapOf("a.com" to 2))))
        val controller = controller(store)
        controller.restore()

        controller.clear()

        assertEquals(0, controller.week.total)
        assertTrue(store.days.isEmpty())
    }

    @Test
    fun `turning a workspace lock on clears the report, changing or removing it does not`() {
        val store = FakeStore(days = listOf(ProtectionDay(today, mapOf("locked.example.com" to 2))))
        val controller = controller(store)
        controller.restore()

        controller.onWorkspaceProtectionChanged(wasProtected = true, isProtected = true)
        controller.onWorkspaceProtectionChanged(wasProtected = true, isProtected = false)
        assertEquals(2, controller.week.total)

        controller.onWorkspaceProtectionChanged(wasProtected = false, isProtected = true)
        assertEquals(0, controller.week.total)
        assertTrue(store.days.isEmpty())
    }

    @Test
    fun `hiding the card is saved`() {
        val store = FakeStore()
        val controller = controller(store)
        controller.restore()

        controller.updateCardVisible(false)

        assertFalse(controller.isCardVisible)
        assertFalse(store.cardVisible)
    }

    @Test
    fun `flush writes pending counts at once`() {
        val store = FakeStore()
        val controller = controller(store)
        controller.restore()
        controller.record("https://a.com/", 1)

        controller.flush()
        controller.flush()

        assertEquals(1, store.saves)
    }

    private fun controller(store: FakeStore) = ProtectionReportController(
        store = store,
        host = object : ProtectionReportController.Host {
            override fun today(): Long = today

            override fun postDelayed(action: Runnable, delayMillis: Long) {
                pending += action
            }

            override fun removeCallbacks(action: Runnable) {
                pending.remove(action)
            }
        },
    )

    private class FakeStore(
        var days: List<ProtectionDay> = emptyList(),
        var cardVisible: Boolean = true,
    ) : ProtectionReportPersistence {
        var saves = 0

        override fun loadDays(): List<ProtectionDay> = days

        override fun saveDays(days: List<ProtectionDay>) {
            saves++
            this.days = days
        }

        override fun loadCardVisible(): Boolean = cardVisible

        override fun saveCardVisible(visible: Boolean) {
            cardVisible = visible
        }
    }
}
