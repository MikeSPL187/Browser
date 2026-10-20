package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Media3OwnershipTest {
    private val tabs = mutableListOf(
        BrowserTab(id = "music", lastAccessedAt = 1, profileId = "home"),
        BrowserTab(id = "video", lastAccessedAt = 2, profileId = "home"),
        BrowserTab(id = "private", lastAccessedAt = 3, profileId = "home", isIncognito = true),
    )
    private var selected = "music"
    private var pictureInPicture: String? = null
    private var locked = emptySet<String>()
    private var transferActive = false
    private val generations = mutableMapOf("music" to 1, "video" to 1, "private" to 1)
    private val playing = mutableSetOf("music", "video", "private")
    private var notifications = 0

    private val ownership = Media3Ownership(
        object : Media3Ownership.Host {
            override val pictureInPictureTabId get() = pictureInPicture
            override val selectedTabId get() = selected
            override val lockedProfileIds get() = locked
            override val isAppDataTransferActive get() = transferActive
            override fun tab(tabId: String) = tabs.firstOrNull { it.id == tabId }
            override fun ownerFor(tabId: String) =
                generations[tabId]?.let { GeckoMediaPlaybackOwner(tabId, engineSessionId = 7, navigationGeneration = it) }
            override fun activeMediaState(tabId: String) =
                GeckoMediaSessionState(isActive = true, isPlaying = true).takeIf { tabId in playing }
            override fun systemMediaState(tabId: String, state: GeckoMediaSessionState) = BrowserMediaState(
                tabId = tabId,
                title = "Song",
                origin = "music.example",
                kind = BrowserMediaKind.entries.first(),
                isPlaying = state.isPlaying,
                currentPositionMillis = 0,
                durationMillis = 60_000,
                playbackRate = 1f,
                sourceUrl = null,
                contentType = null,
                posterUrl = null,
            )
            override fun notifyMediaStateChanged() {
                notifications++
            }
        },
    )

    private fun publishedTab(): String? = ownership.publication("test")?.snapshot?.owner?.tabId

    @Test
    fun theSelectedTabsMediaIsPublishedAndKeepsTheControlsInTheBackground() {
        assertEquals("music", publishedTab())

        selected = "video"

        assertEquals("music", publishedTab())
    }

    @Test
    fun aNavigationVoidsTheOwnerAndTheNextPublicationIsEmptyOnce() {
        val owner = ownership.publication("test")!!.snapshot.owner
        generations["music"] = 2

        assertFalse(ownership.acceptsCommand(owner))
        ownership.invalidate("music")

        assertNull(ownership.publication("test"))
        assertEquals(GeckoMediaPlaybackOwner("music", 7, 2), ownership.publication("test")?.snapshot?.owner)
        assertEquals(1, notifications)
    }

    @Test
    fun aSameDocumentNavigationWithMediaPlayingReplacesThePublication() {
        ownership.publication("test")

        ownership.invalidate("music", replaceForNavigation = true)

        assertTrue(ownership.consumeNavigationReplace())
        assertFalse(ownership.consumeNavigationReplace())
        assertEquals("music", publishedTab())
    }

    @Test
    fun invalidatingAnotherTabChangesNothing() {
        ownership.publication("test")

        ownership.invalidate("video")

        assertEquals("music", publishedTab())
        assertEquals(0, notifications)
    }

    @Test
    fun pictureInPictureTakesTheControlsAndHandsThemBack() {
        ownership.publication("test")
        pictureInPicture = "video"

        val inWindow = ownership.publication("test")!!
        assertEquals("video", inWindow.snapshot.owner.tabId)
        assertTrue(inWindow.isPictureInPictureOwner)
        assertFalse(ownership.acceptsCommand(GeckoMediaPlaybackOwner("music", 7, 1)))

        pictureInPicture = null
        assertEquals("music", publishedTab())
        assertTrue(ownership.consumePictureInPictureRestore())
    }

    @Test
    fun onlyThePublishedOwnerReceivesCommands() {
        val owner = ownership.publication("test")!!.snapshot.owner

        assertTrue(ownership.acceptsCommand(owner))
        assertFalse(ownership.acceptsCommand(GeckoMediaPlaybackOwner("video", 7, 1)))
    }

    @Test
    fun aLockedWorkspaceOrADataTransferStopsCommands() {
        val owner = ownership.publication("test")!!.snapshot.owner

        locked = setOf("home")
        assertFalse(ownership.acceptsCommand(owner))

        locked = emptySet()
        transferActive = true
        assertFalse(ownership.acceptsCommand(owner))
    }

    @Test
    fun privateMediaIsPublishedAsPrivateButNeverCommanded() {
        selected = "private"

        val publication = ownership.publication("test")!!

        assertTrue(publication.isPrivate)
        assertFalse(ownership.acceptsCommand(publication.snapshot.owner))
    }

    @Test
    fun mediaThatEndedPublishesNothing() {
        playing.clear()

        assertNull(ownership.publication("test"))
    }
}
