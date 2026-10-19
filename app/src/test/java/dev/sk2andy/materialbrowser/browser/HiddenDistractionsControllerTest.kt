package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.data.HiddenDistractions
import dev.sk2andy.materialbrowser.data.HiddenDistractionsRules
import dev.sk2andy.materialbrowser.data.HiddenDistractionsStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenDistractionsControllerTest {
    private class FakeStorage : HiddenDistractionsStorage {
        val lists = mutableMapOf<String, HiddenDistractions>()
        val deleted = mutableListOf<String>()
        var loads = 0
        var clears = 0

        override fun load(profileId: String): HiddenDistractions {
            loads++
            return lists[profileId] ?: HiddenDistractions()
        }

        override fun save(profileId: String, state: HiddenDistractions) {
            lists[profileId] = state
        }

        override fun delete(profileId: String) {
            deleted += profileId
            lists.remove(profileId)
        }

        override fun clearAll() {
            clears++
            lists.clear()
        }
    }

    /** Runs storage work only when [runBackground] is called, as the storage thread would later. */
    private class QueueHost : HiddenDistractionsController.Host {
        val background = ArrayDeque<() -> Unit>()

        override fun runInBackground(task: () -> Unit) {
            background += task
        }

        fun runBackground() {
            while (background.isNotEmpty()) background.removeFirst()()
        }
    }

    private class FakePage(private val accepts: Boolean = true) : PageDistractionsPort {
        var listener: PageDistractionsListener? = null
        var highlight: Int? = null
        var stops = 0

        override fun startPicking(highlightArgb: Int, listener: PageDistractionsListener): Boolean {
            if (!accepts) return false
            this.listener = listener
            highlight = highlightArgb
            return true
        }

        override fun stopPicking() {
            stops++
            listener = null
        }
    }

    private val storage = FakeStorage()
    private val host = QueueHost()
    private val pages = mutableMapOf<String, PageDistractionsPort>()
    private var refreshes = 0
    private val controller = HiddenDistractionsController(storage, host, pages::get) { refreshes++ }

    private val tab = BrowserTab(id = "t1", lastAccessedAt = 0, profileId = "work", url = "https://www.news.example/story")
    private val privateTab = tab.copy(id = "p1", isIncognito = true)

    private fun storedFor(profileId: String, host: String, vararg selectors: String) {
        storage.lists[profileId] = selectors.fold(HiddenDistractions()) { state, selector ->
            HiddenDistractionsRules.hide(state, host, selector)
        }
    }

    @Test
    fun `a profile's list is read off the main thread and its pages are refreshed once it is`() {
        storedFor("work", "news.example", "#promo")

        assertEquals(emptyList<String>(), controller.selectorsFor(tab, tab.url))
        host.runBackground()

        assertEquals(listOf("#promo"), controller.selectorsFor(tab, tab.url))
        assertEquals(1, refreshes)
        assertEquals(1, storage.loads)
        assertEquals(1, controller.hiddenCount(tab))
    }

    @Test
    fun `a profile with nothing hidden refreshes no page`() {
        controller.selectorsFor(tab, tab.url)
        host.runBackground()

        assertEquals(0, refreshes)
        assertEquals(emptyList<String>(), controller.selectorsFor(tab.copy(profileId = "home"), tab.url))
    }

    @Test
    fun `a block hidden on a page is kept for the site in the tab's profile`() {
        val page = FakePage()
        pages[tab.id] = page
        controller.selectorsFor(tab, tab.url)
        host.runBackground()

        assertTrue(controller.startPicking(tab, highlightArgb = 0x336699))
        assertEquals("t1", controller.pickingTabId)
        page.listener!!.onHidden("www.news.example", "#promo")
        host.runBackground()

        assertEquals(listOf("#promo"), controller.selectorsFor(tab, "https://news.example/other"))
        assertEquals(listOf("#promo"), storage.lists["work"]?.selectorsFor("news.example"))
        assertEquals(emptyList<String>(), controller.selectorsFor(tab.copy(profileId = "home"), tab.url))
        assertEquals(1, refreshes)
    }

    @Test
    fun `blocks hidden before the list is read join it and are saved`() {
        storedFor("work", "news.example", "#old")
        val page = FakePage()
        pages[tab.id] = page
        controller.startPicking(tab, highlightArgb = 0)

        page.listener!!.onHidden("news.example", "#new")
        assertEquals(listOf("#new"), controller.selectorsFor(tab, tab.url))
        host.runBackground()

        assertEquals(listOf("#old", "#new"), controller.selectorsFor(tab, tab.url))
        assertEquals(listOf("#old", "#new"), storage.lists["work"]?.selectorsFor("news.example"))
    }

    @Test
    fun `a private tab hides in memory only, for private tabs, until the private session ends`() {
        storedFor("work", "news.example", "#saved")
        val page = FakePage()
        pages[privateTab.id] = page
        controller.selectorsFor(tab, tab.url)
        host.runBackground()
        controller.startPicking(privateTab, highlightArgb = 0)

        page.listener!!.onHidden("news.example", "#private")
        host.runBackground()

        assertEquals(listOf("#saved", "#private"), controller.selectorsFor(privateTab, privateTab.url))
        assertEquals(listOf("#saved"), controller.selectorsFor(tab, tab.url))
        assertEquals(listOf("#saved"), storage.lists["work"]?.selectorsFor("news.example"))

        controller.endPrivateSession()

        assertEquals(listOf("#saved"), controller.selectorsFor(privateTab, privateTab.url))
    }

    @Test
    fun `undo shows again only the block hidden last, once, and ends with choosing`() {
        storedFor("work", "news.example", "#old")
        val page = FakePage()
        pages[tab.id] = page
        controller.selectorsFor(tab, tab.url)
        host.runBackground()
        assertFalse(controller.canUndo)

        controller.startPicking(tab, highlightArgb = 0)
        page.listener!!.onHidden("news.example", "#a")
        page.listener!!.onHidden("news.example", "#nav")
        assertTrue(controller.canUndo)

        controller.undoLastHide()
        host.runBackground()
        assertEquals(listOf("#old", "#a"), controller.selectorsFor(tab, tab.url))
        assertEquals(listOf("#old", "#a"), storage.lists["work"]?.selectorsFor("news.example"))
        assertFalse(controller.canUndo)

        page.listener!!.onHidden("news.example", "#b")
        controller.stopPicking()
        assertFalse(controller.canUndo)
        controller.undoLastHide()
        assertEquals(listOf("#old", "#a", "#b"), controller.selectorsFor(tab, tab.url))
    }

    @Test
    fun `undo in a private tab touches the private session only`() {
        storedFor("work", "news.example", "#saved")
        val page = FakePage()
        pages[privateTab.id] = page
        controller.selectorsFor(tab, tab.url)
        host.runBackground()
        controller.startPicking(privateTab, highlightArgb = 0)

        page.listener!!.onHidden("news.example", "#private")
        controller.undoLastHide()
        host.runBackground()

        assertEquals(listOf("#saved"), controller.selectorsFor(privateTab, privateTab.url))
        assertEquals(listOf("#saved"), storage.lists["work"]?.selectorsFor("news.example"))
    }

    @Test
    fun `show all brings back every block of the site and saves that`() {
        storedFor("work", "news.example", "#a", "#b")
        controller.selectorsFor(tab, tab.url)
        host.runBackground()
        val before = refreshes

        controller.showAll(tab)
        host.runBackground()

        assertEquals(emptyList<String>(), controller.selectorsFor(tab, tab.url))
        assertEquals(HiddenDistractions(), storage.lists["work"])
        assertEquals(before + 1, refreshes)
    }

    @Test
    fun `show all in a private tab holds for private tabs in memory and leaves the disk alone`() {
        storedFor("work", "news.example", "#saved")
        controller.selectorsFor(tab, tab.url)
        host.runBackground()

        controller.showAll(privateTab)
        host.runBackground()

        assertEquals(emptyList<String>(), controller.selectorsFor(privateTab, privateTab.url))
        assertEquals(listOf("#saved"), controller.selectorsFor(tab, tab.url))
        assertEquals(listOf("#saved"), storage.lists["work"]?.selectorsFor("news.example"))

        controller.endPrivateSession()

        assertEquals(listOf("#saved"), controller.selectorsFor(privateTab, privateTab.url))
    }

    @Test
    fun `clearing browsing data forgets every hidden block, saved and private, even while read`() {
        storedFor("work", "news.example", "#saved")
        storedFor("home", "mail.example", "#ad")
        val page = FakePage()
        pages[privateTab.id] = page
        controller.selectorsFor(tab, tab.url)
        host.runBackground()
        controller.startPicking(privateTab, highlightArgb = 0)
        page.listener!!.onHidden("news.example", "#private")
        controller.selectorsFor(tab.copy(profileId = "home", url = "https://mail.example/"), "https://mail.example/")

        controller.clearAll()
        host.runBackground()

        assertEquals(1, storage.clears)
        assertTrue(storage.lists.isEmpty())
        assertEquals(emptyList<String>(), controller.selectorsFor(tab, tab.url))
        assertEquals(emptyList<String>(), controller.selectorsFor(privateTab, privateTab.url))
    }

    @Test
    fun `a deleted profile's list is deleted, even while it is being read`() {
        storedFor("work", "news.example", "#a")
        controller.selectorsFor(tab, tab.url)

        controller.forgetProfile("work")
        host.runBackground()

        assertEquals(listOf("work"), storage.deleted)
        assertNull(storage.lists["work"])
        assertEquals(0, refreshes)
    }

    @Test
    fun `choosing stops on request, on navigation, and never starts on a page that cannot`() {
        val page = FakePage()
        pages[tab.id] = page
        pages["t2"] = FakePage(accepts = false)

        assertTrue(controller.startPicking(tab, highlightArgb = 0x0a84ff))
        assertEquals(0x0a84ff, page.highlight)
        controller.stopPicking()
        assertNull(controller.pickingTabId)
        assertEquals(1, page.stops)

        controller.startPicking(tab, highlightArgb = 0)
        page.listener!!.onPickingEnded()
        assertNull(controller.pickingTabId)

        assertFalse(controller.startPicking(tab.copy(id = "t2"), highlightArgb = 0))
        assertFalse(controller.startPicking(tab.copy(id = "gone"), highlightArgb = 0))
        assertNull(controller.pickingTabId)
    }

    @Test
    fun `choosing on another tab stops the first`() {
        val first = FakePage()
        val second = FakePage()
        pages[tab.id] = first
        pages["t2"] = second

        controller.startPicking(tab, highlightArgb = 0)
        controller.startPicking(tab.copy(id = "t2"), highlightArgb = 0)

        assertEquals(1, first.stops)
        assertEquals("t2", controller.pickingTabId)
    }
}
