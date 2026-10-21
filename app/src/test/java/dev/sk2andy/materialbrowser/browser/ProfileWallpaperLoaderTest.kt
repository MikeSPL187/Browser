package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileWallpaperLoaderTest {
    private val wallpaper = ProfileWallpaper(zoom = 1.5f)
    private var profiles = mutableListOf(
        BrowserProfile(id = "home", emoji = "🏠", newTabWallpaper = wallpaper, tabSwitcherWallpaper = wallpaper),
        BrowserProfile(id = "work", emoji = "💼"),
    )
    private var active = "home"
    private var destroyed = false
    private val files = mutableMapOf(
        ("home" to ProfileWallpaperTarget.NewTab) to "home-new-tab",
        ("home" to ProfileWallpaperTarget.TabSwitcher) to "home-tabs",
    )
    private val mainThread = ArrayDeque<Runnable>()
    private val recycled = mutableListOf<String>()
    private val forgotten = mutableListOf<Pair<String, ProfileWallpaperTarget>>()

    private val loader = ProfileWallpaperLoader(
        background = { it.run() },
        postToMainThread = { mainThread.addLast(it) },
        load = { profileId, target -> files[profileId to target] },
        recycle = { recycled += it },
        host = object : ProfileWallpaperLoader.Host {
            override val activeLocalProfile get() = profiles.firstOrNull { it.id == active }
            override val activeProfileId get() = active
            override val isDestroyed get() = destroyed
            override fun forgetMissing(profileId: String, target: ProfileWallpaperTarget, wallpaper: ProfileWallpaper) {
                forgotten += profileId to target
            }
        },
    )

    private fun runMainThread() {
        while (mainThread.isNotEmpty()) mainThread.removeFirst().run()
    }

    @Test
    fun theNewTabWallpaperShowsOnceLoaded() {
        loader.refreshNewTab()
        assertNull(loader.newTab)

        runMainThread()

        assertEquals("home-new-tab", loader.newTab)
    }

    @Test
    fun aProfileWithoutAWallpaperShowsNothingAndReadsNoFile() {
        active = "work"

        loader.refreshNewTab()

        assertTrue(mainThread.isEmpty())
        assertNull(loader.newTab)
    }

    @Test
    fun aLoadThatEndsAfterTheProfileChangedIsRecycled() {
        loader.refreshNewTab()
        active = "work"

        runMainThread()

        assertNull(loader.newTab)
        assertEquals(listOf("home-new-tab"), recycled)
    }

    @Test
    fun onlyTheLatestLoadLands() {
        loader.refreshNewTab()
        loader.refreshNewTab()

        runMainThread()

        assertEquals("home-new-tab", loader.newTab)
        assertEquals(listOf("home-new-tab"), recycled)
    }

    @Test
    fun aMissingFileIsForgottenAndTheTabSwitcherStillGetsReady() {
        files.remove("home" to ProfileWallpaperTarget.TabSwitcher)
        var ready = 0

        loader.loadTabSwitcher { ready++ }
        runMainThread()

        assertNull(loader.tabSwitcher)
        assertEquals(listOf("home" to ProfileWallpaperTarget.TabSwitcher), forgotten)
        assertEquals(1, ready)
    }

    @Test
    fun theTabSwitcherIsReadyAtOnceWithoutAWallpaper() {
        active = "work"
        var ready = 0

        loader.loadTabSwitcher { ready++ }

        assertEquals(1, ready)
    }

    @Test
    fun editingReleasesBothAndDropsLoadsInFlight() {
        loader.refreshNewTab()
        loader.loadTabSwitcher()
        runMainThread()
        loader.refreshNewTab()

        loader.releaseForEditing("home")
        runMainThread()

        assertNull(loader.newTab)
        assertNull(loader.tabSwitcher)
        assertEquals(listOf("home-new-tab"), recycled)

        loader.restoreAfterEditing()
        runMainThread()
        assertEquals("home-new-tab", loader.newTab)
        assertEquals("home-tabs", loader.tabSwitcher)
    }

    @Test
    fun editingAnotherProfileLeavesTheShownWallpapers() {
        loader.refreshNewTab()
        runMainThread()

        loader.releaseForEditing("work")

        assertEquals("home-new-tab", loader.newTab)
    }

    @Test
    fun closingDropsWhatIsStillLoading() {
        loader.refreshNewTab()
        loader.loadTabSwitcher()

        loader.close()
        runMainThread()

        assertNull(loader.newTab)
        assertNull(loader.tabSwitcher)
        assertEquals(listOf("home-new-tab", "home-tabs"), recycled)
    }
}
