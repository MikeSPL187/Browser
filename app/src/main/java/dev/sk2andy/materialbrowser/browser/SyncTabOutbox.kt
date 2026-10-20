package dev.sk2andy.materialbrowser.browser

import androidx.annotation.VisibleForTesting
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEvent
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType
import dev.sk2andy.materialbrowser.sync.SyncPendingMutation
import dev.sk2andy.materialbrowser.sync.SyncRepositoryState
import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * What this device sends to its synced devices (#500): tabs opened, navigated, moved, pinned and
 * closed here, and the navigations that arrived from another device, so a remote load is not sent
 * back as a local one. The controller keeps the tabs; this keeps what is in flight about them.
 */
internal class SyncTabOutbox(
    /** Hands a mutation to the sync repository; completes once it is written. */
    private val send: (SyncPendingMutation) -> CompletableFuture<*>,
    private val mainThread: Scheduler,
    private val host: Host,
) {
    /** The main thread's queue. */
    interface Scheduler {
        fun post(task: Runnable)

        fun postDelayed(task: Runnable, delayMillis: Long)

        fun cancel(task: Runnable)
    }

    /** The controller state the outbox reads. */
    interface Host {
        val tabs: List<BrowserTab>
        val selectedTabId: String
        val syncState: SyncRepositoryState
        val isDestroyed: Boolean

        fun isSessionEphemeralTab(tabId: String): Boolean

        fun syncTargetDeviceId(profileId: String): String?

        /** Loads [url] in the tab's live engine session, if it has one. */
        fun loadRemoteNavigation(tabId: String, url: String)
    }

    /** Tabs opened here whose open has not reached the synced device yet. */
    val locallyPendingCandyIds = mutableSetOf<String>()

    /** A reopen in flight per tab, by its mutation id. */
    val pendingReopenMutationIds = mutableMapOf<String, String>()

    /** Local navigations not yet reported back by the synced device. */
    val pendingLocalNavigationUrls = mutableMapOf<String, String>()

    private val pendingNavigationRunnables = mutableMapOf<String, Runnable>()
    private val remoteNavigationUrls = mutableMapOf<String, String>()
    private val supersededRemoteNavigationUrls = mutableMapOf<String, MutableSet<String>>()

    @VisibleForTesting
    var mutationObserverForTesting: ((SyncPendingMutation) -> Unit)? = null

    private fun isSyncTargetProfile(profileId: String): Boolean = host.syncTargetDeviceId(profileId) != null

    fun markTabPending(tab: BrowserTab) {
        if (host.isSessionEphemeralTab(tab.id)) return
        tab.syncCandyId?.let(locallyPendingCandyIds::add)
    }

    fun markLocalNavigationPending(tabId: String, url: String?) {
        val tab = host.tabs.firstOrNull { candidate -> candidate.id == tabId } ?: return
        if (
            tab.isIncognito ||
            host.isSessionEphemeralTab(tabId) ||
            !isSyncTargetProfile(tab.profileId)
        ) {
            return
        }
        val safeUrl = url?.let(BrowserUriPolicy::normalizeHttpUrl) ?: return
        if (remoteNavigationUrls[tabId] == safeUrl) return
        pendingLocalNavigationUrls[tabId] = safeUrl
    }

    fun prepareHydration(navigation: SyncedTabNavigation): String? {
        val safeUrl = BrowserUriPolicy.normalizeHttpUrl(navigation.url) ?: return null
        if (host.tabs.none { tab -> tab.id == navigation.runtimeTabId }) return null
        pendingNavigationRunnables.remove(navigation.runtimeTabId)
            ?.let(mainThread::cancel)
        remoteNavigationUrls.put(navigation.runtimeTabId, safeUrl)
            ?.takeIf { previousUrl -> previousUrl != safeUrl }
            ?.let { previousUrl ->
                supersededRemoteNavigationUrls
                    .getOrPut(navigation.runtimeTabId) { mutableSetOf() }
                    .add(previousUrl)
            }
        return safeUrl
    }

    fun ignoreSupersededRemoteNavigationEvent(event: BrowserEngineEvent): Boolean {
        val safeUrl = event.address?.let(BrowserUriPolicy::normalizeHttpUrl) ?: return false
        if (safeUrl !in supersededRemoteNavigationUrls[event.tabId].orEmpty()) return false
        if (
            event.type == BrowserEngineEventType.NavigationCommitted ||
            event.type == BrowserEngineEventType.NavigationFailed ||
            event.isLoading == false
        ) {
            val expectedUrl = remoteNavigationUrls[event.tabId] ?: return true
            host.loadRemoteNavigation(event.tabId, expectedUrl)
        }
        return true
    }

    fun clearRemoteNavigationTracking(tabId: String) {
        remoteNavigationUrls.remove(tabId)
        supersededRemoteNavigationUrls.remove(tabId)
    }

    /** A tab's navigation ended or failed: nothing about it is in flight any more. */
    fun forgetNavigation(tabId: String) {
        pendingLocalNavigationUrls.remove(tabId)
        clearRemoteNavigationTracking(tabId)
    }

    /** A tab is gone: drop everything pending about it. */
    fun forgetTab(tabId: String, syncCandyId: String?) {
        pendingNavigationRunnables.remove(tabId)?.let(mainThread::cancel)
        forgetNavigation(tabId)
        syncCandyId?.let { candyId ->
            locallyPendingCandyIds.remove(candyId)
            pendingReopenMutationIds.remove(candyId)
        }
    }

    fun applyNavigation(navigation: SyncedTabNavigation) {
        val safeUrl = prepareHydration(navigation) ?: return
        host.loadRemoteNavigation(navigation.runtimeTabId, safeUrl)
    }

    fun scheduleNavigation(
        tabId: String,
        remoteNavigationFinished: Boolean = false,
    ) {
        if (host.isSessionEphemeralTab(tabId)) {
            pendingNavigationRunnables.remove(tabId)?.let(mainThread::cancel)
            return
        }
        val tab = host.tabs.firstOrNull { it.id == tabId } ?: return
        if (!isSyncTargetProfile(tab.profileId) || tab.isIncognito) return
        remoteNavigationUrls[tabId]?.let { expectedUrl ->
            if (BrowserUriPolicy.normalizeHttpUrl(tab.url) == expectedUrl) {
                if (remoteNavigationFinished) clearRemoteNavigationTracking(tabId)
                return
            }
            clearRemoteNavigationTracking(tabId)
        }
        pendingNavigationRunnables.remove(tabId)?.let(mainThread::cancel)
        val runnable = Runnable {
            pendingNavigationRunnables.remove(tabId)
            enqueueTab(tabId)
        }
        pendingNavigationRunnables[tabId] = runnable
        mainThread.postDelayed(runnable, NAVIGATION_DEBOUNCE_MILLIS)
    }

    fun enqueueTab(tabId: String, forceOpen: Boolean = false) {
        if (host.isSessionEphemeralTab(tabId)) return
        val tab = host.tabs.firstOrNull { it.id == tabId } ?: return
        val targetDeviceId = host.syncTargetDeviceId(tab.profileId) ?: return
        val tabIndex = host.tabs.filter { candidate ->
            candidate.profileId == tab.profileId && !host.isSessionEphemeralTab(candidate.id)
        }.indexOfFirst { it.id == tabId }
        if (tabIndex < 0) return
        val outbound = SyncedProfileRuntimeRules.outboundTab(tab, tabIndex, host.selectedTabId) ?: return
        val remote = host.syncState.profiles.firstOrNull { it.deviceId == targetDeviceId }
        val remoteTab = remote?.tabs?.firstOrNull { it.candyId == outbound.candyId }
        if (!forceOpen && remoteTab?.url == outbound.url && remoteTab.title == outbound.title) return
        val mutationId = UUID.randomUUID().toString()
        val mutation = if (remoteTab != null && !forceOpen) {
            SyncPendingMutation.Navigate(
                mutationId = mutationId,
                targetDeviceId = targetDeviceId,
                candyId = outbound.candyId,
                title = outbound.title,
                url = outbound.url,
            )
        } else {
            locallyPendingCandyIds += outbound.candyId
            SyncPendingMutation.Open(
                mutationId = mutationId,
                targetDeviceId = targetDeviceId,
                tab = outbound,
            )
        }
        if (forceOpen) {
            val candyId = outbound.candyId
            pendingReopenMutationIds[candyId] = mutationId
            mutate(mutation) {
                if (pendingReopenMutationIds[candyId] != mutationId) return@mutate
                pendingReopenMutationIds.remove(candyId)
                val reopened = host.syncState.profiles.firstOrNull { it.deviceId == targetDeviceId }
                    ?.tabs?.any { it.candyId == candyId } == true
                if (reopened) locallyPendingCandyIds.remove(candyId)
            }
        } else {
            mutate(mutation)
        }
    }

    fun enqueueClose(tab: BrowserTab) {
        if (host.isSessionEphemeralTab(tab.id)) return
        val candyId = tab.syncCandyId ?: return
        val targetDeviceId = host.syncTargetDeviceId(tab.profileId) ?: return
        pendingNavigationRunnables.remove(tab.id)?.let(mainThread::cancel)
        locallyPendingCandyIds.remove(candyId)
        pendingReopenMutationIds.remove(candyId)
        mutate(
            SyncPendingMutation.Close(
                mutationId = UUID.randomUUID().toString(),
                targetDeviceId = targetDeviceId,
                candyId = candyId,
            ),
        )
    }

    fun enqueueOrder(profileId: String) {
        val targetDeviceId = host.syncTargetDeviceId(profileId) ?: return
        val orderedCandyIds = host.tabs.filter { tab ->
            tab.profileId == profileId && !host.isSessionEphemeralTab(tab.id)
        }
            .mapNotNull(BrowserTab::syncCandyId)
        mutate(
            SyncPendingMutation.Reorder(
                mutationId = UUID.randomUUID().toString(),
                targetDeviceId = targetDeviceId,
                orderedCandyIds = orderedCandyIds,
            ),
        )
    }

    fun enqueuePinned(tabId: String, pinned: Boolean) {
        if (host.isSessionEphemeralTab(tabId)) return
        val tab = host.tabs.firstOrNull { it.id == tabId } ?: return
        val candyId = tab.syncCandyId ?: return
        val targetDeviceId = host.syncTargetDeviceId(tab.profileId) ?: return
        mutate(
            SyncPendingMutation.SetPinned(
                mutationId = UUID.randomUUID().toString(),
                targetDeviceId = targetDeviceId,
                candyId = candyId,
                pinned = pinned,
            ),
        )
    }

    fun mutate(
        mutation: SyncPendingMutation,
        onComplete: (() -> Unit)? = null,
    ) {
        mutationObserverForTesting?.invoke(mutation)
        val future = send(mutation)
        if (onComplete != null) {
            future.whenComplete { _, _ ->
                // Repository listener posts precede completion, so stale close states keep the guard.
                mainThread.post { if (!host.isDestroyed) onComplete() }
            }
        }
    }

    /** The controller is destroyed: nothing scheduled may run. */
    fun close() {
        pendingNavigationRunnables.values.forEach(mainThread::cancel)
        pendingNavigationRunnables.clear()
        pendingReopenMutationIds.clear()
        remoteNavigationUrls.clear()
        supersededRemoteNavigationUrls.clear()
    }

    companion object {
        const val NAVIGATION_DEBOUNCE_MILLIS = 450L
    }
}
