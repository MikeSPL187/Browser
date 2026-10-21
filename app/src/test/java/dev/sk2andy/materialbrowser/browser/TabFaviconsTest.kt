package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabFaviconsTest {
    private class Icon(val name: String) {
        var recycled = false
    }

    private val page = "https://news.example/story"
    private val tabs = mutableListOf(
        BrowserTab(id = "a", lastAccessedAt = 1, profileId = "home", url = page),
        BrowserTab(id = "p", lastAccessedAt = 2, profileId = "home", url = page, isIncognito = true),
    )
    private val sessions = mutableMapOf("a" to "session-a", "p" to "session-p")
    private val generations = mutableMapOf("a" to 1, "p" to 1)
    private val mainThread = ArrayDeque<() -> Unit>()
    private val saved = mutableMapOf<String, Icon>()
    private val deleted = mutableListOf<String>()
    private val pendingRestores = mutableListOf<(String, Icon) -> Unit>()
    private val pendingFetches = mutableListOf<Pair<() -> Boolean, (Icon?) -> Unit>>()
    private val images = mutableMapOf<String, Icon>()

    private val favicons = TabFavicons(
        images = images,
        store = object : TabFavicons.Store<Icon> {
            override fun restore(tabIds: Set<String>, onLoaded: (String, Icon) -> Unit) {
                pendingRestores += onLoaded
            }
            override fun save(tabId: String, icon: Icon) {
                saved[tabId] = icon
            }
            override fun delete(tabId: String) {
                deleted += tabId
            }
            override fun clear() = saved.clear()
            override fun fetch(pageUrl: String, shouldFetch: () -> Boolean, onLoaded: (Icon?) -> Unit): Boolean {
                pendingFetches += shouldFetch to onLoaded
                return true
            }
        },
        isRecycled = Icon::recycled,
        recycle = { it.recycled = true },
        host = object : TabFavicons.Host<String> {
            override val isDestroyed = false
            override val fetchesIcons = true
            override fun tab(tabId: String) = tabs.firstOrNull { it.id == tabId }
            override fun isSessionOnly(tabId: String) = false
            override fun session(tabId: String) = sessions[tabId]
            override fun navigationGeneration(tabId: String) = generations.getOrDefault(tabId, 0)
            override fun postToMainThread(task: () -> Unit) {
                mainThread.addLast(task)
            }
        },
    )

    private fun runMainThread() {
        while (mainThread.isNotEmpty()) mainThread.removeFirst()()
    }

    @Test
    fun anEngineIconForTheShownPageIsKeptAndSavedOnlyForARegularTab() {
        val icon = Icon("a")
        favicons.onEngineIcon("a", "session-a", page, icon)
        favicons.onEngineIcon("p", "session-p", page, Icon("p"))

        assertEquals(icon, images["a"])
        assertEquals(icon, saved["a"])
        assertTrue("p" in images)
        assertTrue("p" !in saved)
    }

    @Test
    fun anIconFromAnotherPageOrSessionIsIgnored() {
        favicons.onEngineIcon("a", "session-a", "https://news.example/other", Icon("x"))
        favicons.onEngineIcon("a", "old-session", page, Icon("y"))

        assertTrue(images.isEmpty())
    }

    @Test
    fun aKeptIconLandsUnlessTheTabChangedMeanwhile() {
        favicons.restore(setOf("a"))
        favicons.invalidate("a")
        val stale = Icon("stale")
        pendingRestores.single()("a", stale)
        runMainThread()

        assertNull(images["a"])
        assertTrue(stale.recycled)
        assertEquals(listOf("a"), deleted)
    }

    @Test
    fun aFetchedIconLandsOnlyForTheSameLoad() {
        favicons.scheduleFetch("a", page)
        favicons.scheduleFetch("a", page)
        assertEquals(1, pendingFetches.size)

        val icon = Icon("fetched")
        pendingFetches.single().second(icon)
        runMainThread()
        assertEquals(icon, images["a"])
    }

    @Test
    fun aFetchForAPreviousLoadIsCancelledAndRecycled() {
        favicons.scheduleFetch("a", page)
        val (shouldFetch, onLoaded) = pendingFetches.single()
        generations["a"] = 2
        favicons.scheduleFetch("a", page)

        assertTrue(!shouldFetch())
        val late = Icon("late")
        onLoaded(late)
        runMainThread()
        assertNull(images["a"])
        assertTrue(late.recycled)
    }

    @Test
    fun privateTabsAreNeverFetched() {
        favicons.scheduleFetch("p", page)

        assertTrue(pendingFetches.isEmpty())
    }

    @Test
    fun clearingDataDropsEverythingAndLateArrivalsToo() {
        favicons.onEngineIcon("a", "session-a", page, Icon("a"))
        favicons.scheduleFetch("a", "https://news.example/story")
        favicons.restore(setOf("a"))

        favicons.clearAll()
        val late = Icon("late")
        pendingRestores.single()("a", late)
        runMainThread()

        assertTrue(images.isEmpty())
        assertTrue(saved.isEmpty())
        assertTrue(late.recycled)
    }
}
