package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEvent
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType
import dev.sk2andy.materialbrowser.sync.SyncDeviceIconDescriptor
import dev.sk2andy.materialbrowser.sync.SyncPendingMutation
import dev.sk2andy.materialbrowser.sync.SyncProfile
import dev.sk2andy.materialbrowser.sync.SyncRepositoryState
import dev.sk2andy.materialbrowser.sync.SyncStatus
import dev.sk2andy.materialbrowser.sync.SyncTab
import java.util.concurrent.CompletableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTabOutboxTest {
    private val sent = mutableListOf<SyncPendingMutation>()
    private val delayed = mutableListOf<Runnable>()
    private val posted = mutableListOf<Runnable>()
    private val loads = mutableListOf<Pair<String, String>>()
    private val writes = mutableListOf<CompletableFuture<Unit>>()
    private var state = state(remoteTabs = emptyList())
    private var tabs = listOf(
        BrowserTab(id = "a", lastAccessedAt = 1, profileId = "work", url = "https://news.example/", syncCandyId = "candy-a"),
        BrowserTab(id = "b", lastAccessedAt = 2, profileId = "work", url = "https://mail.example/", syncCandyId = "candy-b"),
        BrowserTab(id = "c", lastAccessedAt = 3, profileId = "home", url = "https://other.example/", syncCandyId = "candy-c"),
    )

    private val outbox = SyncTabOutbox(
        send = { mutation -> sent += mutation; CompletableFuture<Unit>().also(writes::add) },
        mainThread = object : SyncTabOutbox.Scheduler {
            override fun post(task: Runnable) {
                posted += task
            }

            override fun postDelayed(task: Runnable, delayMillis: Long) {
                assertEquals(SyncTabOutbox.NAVIGATION_DEBOUNCE_MILLIS, delayMillis)
                delayed += task
            }

            override fun cancel(task: Runnable) {
                delayed -= task
            }
        },
        host = object : SyncTabOutbox.Host {
            override val tabs get() = this@SyncTabOutboxTest.tabs
            override val selectedTabId = "a"
            override val syncState get() = state
            override val isDestroyed = false
            override fun isSessionEphemeralTab(tabId: String) = false
            override fun syncTargetDeviceId(profileId: String) = "laptop".takeIf { profileId == "work" }
            override fun loadRemoteNavigation(tabId: String, url: String) {
                loads += tabId to url
            }
        },
    )

    @Test
    fun aTabTheSyncedDeviceLacksOpensThereAndStaysPendingUntilItArrives() {
        outbox.enqueueTab("a")

        val open = sent.single() as SyncPendingMutation.Open
        assertEquals("laptop", open.targetDeviceId)
        assertEquals("candy-a", open.tab.candyId)
        assertTrue("candy-a" in outbox.locallyPendingCandyIds)
    }

    @Test
    fun aTabTheSyncedDeviceHasNavigatesAndAnUnchangedOneSendsNothing() {
        state = state(remoteTabs = listOf(remoteTab("candy-a", "https://old.example/")))
        outbox.enqueueTab("a")
        assertEquals("https://news.example/", (sent.single() as SyncPendingMutation.Navigate).url)

        state = state(remoteTabs = listOf(remoteTab("candy-a", "https://news.example/")))
        outbox.enqueueTab("a")
        assertEquals(1, sent.size)
    }

    @Test
    fun aProfileThatIsNotSyncedSendsNothing() {
        outbox.enqueueTab("c")
        outbox.enqueueOrder("home")
        outbox.markLocalNavigationPending("c", "https://other.example/next")

        assertTrue(sent.isEmpty())
        assertTrue(outbox.pendingLocalNavigationUrls.isEmpty())
    }

    @Test
    fun navigationsAreDebouncedAndALaterOneReplacesTheEarlier() {
        outbox.scheduleNavigation("a")
        outbox.scheduleNavigation("a")
        assertEquals(1, delayed.size)

        delayed.single().run()
        assertTrue(sent.single() is SyncPendingMutation.Open)
    }

    @Test
    fun aRemoteNavigationIsLoadedAndItsOwnLoadIsNotSentBack() {
        outbox.applyNavigation(SyncedTabNavigation(runtimeTabId = "a", url = "https://remote.example/"))
        assertEquals(listOf("a" to "https://remote.example/"), loads)

        outbox.markLocalNavigationPending("a", "https://remote.example/")
        assertTrue(outbox.pendingLocalNavigationUrls.isEmpty())
        outbox.markLocalNavigationPending("a", "https://typed.example/")
        assertEquals("https://typed.example/", outbox.pendingLocalNavigationUrls["a"])
    }

    @Test
    fun aSupersededRemoteLoadIsIgnoredAndTheLatestOneLoadedAgain() {
        outbox.applyNavigation(SyncedTabNavigation(runtimeTabId = "a", url = "https://first.example/"))
        outbox.applyNavigation(SyncedTabNavigation(runtimeTabId = "a", url = "https://second.example/"))
        loads.clear()

        val stale = BrowserEngineEvent(
            tabId = "a",
            type = BrowserEngineEventType.NavigationCommitted,
            address = "https://first.example/",
            title = null,
            canGoBack = false,
            canGoForward = false,
            failureDescription = null,
        )
        assertTrue(outbox.ignoreSupersededRemoteNavigationEvent(stale))
        assertEquals(listOf("a" to "https://second.example/"), loads)
        assertFalse(outbox.ignoreSupersededRemoteNavigationEvent(stale.copy(address = "https://second.example/")))
    }

    @Test
    fun closingATabSendsItsCloseAndForgetsWhatWasPending() {
        outbox.enqueueTab("a")
        outbox.scheduleNavigation("a")
        outbox.enqueueClose(tabs.first())

        assertTrue(sent.last() is SyncPendingMutation.Close)
        assertTrue(delayed.isEmpty())
        assertFalse("candy-a" in outbox.locallyPendingCandyIds)
    }

    @Test
    fun aReopenStaysPendingUntilTheDeviceHasTheTabAgain() {
        outbox.enqueueTab("a", forceOpen = true)
        assertEquals(1, outbox.pendingReopenMutationIds.size)

        writes.single().complete(Unit)
        state = state(remoteTabs = listOf(remoteTab("candy-a", "https://news.example/")))
        posted.single().run()

        assertTrue(outbox.pendingReopenMutationIds.isEmpty())
        assertFalse("candy-a" in outbox.locallyPendingCandyIds)
    }

    @Test
    fun forgettingATabCancelsItsNavigationAndClearsItsPendingState() {
        outbox.enqueueTab("a")
        outbox.scheduleNavigation("a")
        outbox.markLocalNavigationPending("a", "https://typed.example/")

        outbox.forgetTab("a", "candy-a")

        assertTrue(delayed.isEmpty())
        assertTrue(outbox.pendingLocalNavigationUrls.isEmpty())
        assertFalse("candy-a" in outbox.locallyPendingCandyIds)
    }

    @Test
    fun orderAndPinSendTheProfilesTabs() {
        outbox.enqueueOrder("work")
        outbox.enqueuePinned("b", pinned = true)

        assertEquals(listOf("candy-a", "candy-b"), (sent[0] as SyncPendingMutation.Reorder).orderedCandyIds)
        assertEquals("candy-b", (sent[1] as SyncPendingMutation.SetPinned).candyId)
    }

    private fun remoteTab(candyId: String, url: String) = SyncTab(
        candyId = candyId,
        windowId = 0,
        index = 0,
        groupId = null,
        active = false,
        pinned = false,
        title = "",
        url = url,
    )

    private fun state(remoteTabs: List<SyncTab>) = SyncRepositoryState(
        settings = null,
        status = SyncStatus.Ready,
        profiles = listOf(
            SyncProfile(
                deviceId = "laptop",
                displayName = "Laptop",
                icon = SyncDeviceIconDescriptor("computer", 100),
                revision = 1,
                tabs = remoteTabs,
                lastSeenAt = "",
            ),
        ),
        pendingCount = 0,
        lastCursor = null,
        lastSuccessAt = null,
        currentDeviceId = "phone",
    )
}
