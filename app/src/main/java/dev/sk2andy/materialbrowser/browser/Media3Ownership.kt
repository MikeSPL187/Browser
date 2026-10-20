package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.engine.GeckoMediaSessionState

/**
 * Which page owns the system media controls (#500): what [publication] hands Media3, which owner a
 * transport command may still reach, and when a navigation or a lock voids the owner. Memory only.
 *
 * A captured background owner stays published until it ends or is invalidated; a picture-in-picture
 * owner may preempt it and hands it back when the window closes.
 */
internal class Media3Ownership(private val host: Host) {
    /** The controller state an owner depends on. */
    interface Host {
        /** The tab in picture-in-picture or on its way there, or null. */
        val pictureInPictureTabId: String?
        val selectedTabId: String
        val lockedProfileIds: Set<String>
        val isAppDataTransferActive: Boolean

        fun tab(tabId: String): BrowserTab?

        /** The owner [tabId]'s current engine session and navigation would publish as. */
        fun ownerFor(tabId: String): GeckoMediaPlaybackOwner?

        /** [tabId]'s media while it plays or is paused, or null once it ended. */
        fun activeMediaState(tabId: String): GeckoMediaSessionState?

        /** [state] as the system may show it, or null when it must stay hidden. */
        fun systemMediaState(tabId: String, state: GeckoMediaSessionState): BrowserMediaState?

        fun notifyMediaStateChanged()
    }

    private var capturedOwner: GeckoMediaPlaybackOwner? = null
    private var backgroundOwner: GeckoMediaPlaybackOwner? = null
    private var pictureInPictureActive = false
    private var pictureInPictureRestorePending = false
    private var navigationReplacePending = false
    private var invalidationPending = false

    fun publication(traceSource: String): GeckoMediaPlaybackPublication? {
        if (invalidationPending) {
            invalidationPending = false
            BrowserMediaLifecycleTrace.record(
                source = traceSource,
                action = "publication-null:invalidation-pending",
            )
            return null
        }
        val pictureInPictureTabId = host.pictureInPictureTabId
        if (pictureInPictureTabId == null && pictureInPictureActive) {
            pictureInPictureActive = false
            capturedOwner = backgroundOwner?.takeIf(::isCurrent)
            backgroundOwner = null
            pictureInPictureRestorePending = true
        }
        val owner = when {
            pictureInPictureTabId != null -> {
                val pictureInPictureOwner = host.ownerFor(pictureInPictureTabId)
                if (pictureInPictureOwner != null && pictureInPictureOwner != capturedOwner) {
                    backgroundOwner = capturedOwner?.takeIf(::isCurrent)
                }
                pictureInPictureActive = true
                pictureInPictureOwner
            }
            capturedOwner?.let(::isCurrent) == true -> capturedOwner
            else -> host.ownerFor(host.selectedTabId)
        } ?: run {
            capturedOwner = null
            BrowserMediaLifecycleTrace.record(
                source = traceSource,
                action = "publication-null:no-current-owner",
            )
            return null
        }
        val tab = host.tab(owner.tabId) ?: run {
            BrowserMediaLifecycleTrace.record(
                source = traceSource,
                action = "publication-null:owner-tab-missing",
                owner = owner,
            )
            return null
        }
        val state = host.activeMediaState(owner.tabId) ?: run {
            if (capturedOwner == owner) capturedOwner = null
            BrowserMediaLifecycleTrace.record(
                source = traceSource,
                action = "publication-null:media-inactive",
                owner = owner,
            )
            return null
        }
        val systemState = host.systemMediaState(owner.tabId, state) ?: run {
            BrowserMediaLifecycleTrace.record(
                source = traceSource,
                action = "publication-null:system-state-rejected",
                owner = owner,
            )
            return null
        }
        val snapshot = GeckoMediaPlaybackRules.snapshot(
            state = systemState,
            owner = owner,
        ) ?: run {
            BrowserMediaLifecycleTrace.record(
                source = traceSource,
                action = "publication-null:snapshot-rejected",
                owner = owner,
            )
            return null
        }
        capturedOwner = owner
        return GeckoMediaPlaybackPublication(
            snapshot = snapshot,
            isPrivate = tab.isIncognito,
            isProfileLocked = tab.profileId in host.lockedProfileIds,
            isAppDataTransferActive = host.isAppDataTransferActive,
            isPictureInPictureOwner = pictureInPictureTabId == owner.tabId,
        ).also { publication ->
            BrowserMediaLifecycleTrace.record(
                source = traceSource,
                action = "publication",
                publication = publication,
            )
        }
    }

    fun consumePictureInPictureRestore(): Boolean =
        pictureInPictureRestorePending.also { pictureInPictureRestorePending = false }

    fun consumeNavigationReplace(): Boolean =
        navigationReplacePending.also { navigationReplacePending = false }

    /** Whether a transport command from [owner] may still reach its page. */
    fun acceptsCommand(owner: GeckoMediaPlaybackOwner): Boolean =
        isCurrent(owner) &&
            BrowserMedia3CommandOwnerRules.accepts(
                owner = owner,
                capturedOwner = capturedOwner,
                pictureInPictureOwner = host.pictureInPictureTabId?.let(host::ownerFor),
            )

    private fun isCurrent(owner: GeckoMediaPlaybackOwner): Boolean =
        host.ownerFor(owner.tabId) == owner &&
            host.tab(owner.tabId)?.let { tab ->
                !tab.isIncognito && tab.profileId !in host.lockedProfileIds
            } == true &&
            !host.isAppDataTransferActive

    /**
     * [tabId]'s page went away, navigated or locked: no owner on it may publish or receive a command.
     * A same-document navigation with media still playing replaces the publication instead.
     */
    fun invalidate(tabId: String, replaceForNavigation: Boolean = false) {
        if (!BrowserMedia3OwnerInvalidationRules.matches(
                capturedOwner = capturedOwner,
                backgroundOwner = backgroundOwner,
                tabId = tabId,
            )
        ) {
            BrowserMediaLifecycleTrace.record(
                source = "BrowserController.invalidateMedia3OwnerFor",
                action = "ignored:no-matching-owner",
            )
            return
        }
        val invalidatedOwner = capturedOwner ?: backgroundOwner
        // A PiP publication can retain a second, preempted background owner. Once either owner
        // becomes stale, discard the complete handoff so neither can receive a later transport.
        capturedOwner = null
        backgroundOwner = null
        pictureInPictureActive = false
        pictureInPictureRestorePending = false
        if (replaceForNavigation && host.activeMediaState(tabId) != null) {
            invalidationPending = false
            navigationReplacePending = true
            BrowserMediaLifecycleTrace.record(
                source = "BrowserController.invalidateMedia3OwnerFor",
                action = "replace:same-document-navigation",
                owner = invalidatedOwner,
            )
        } else {
            navigationReplacePending = false
            invalidationPending = true
            BrowserMediaLifecycleTrace.record(
                source = "BrowserController.invalidateMedia3OwnerFor",
                action = "clear:owner-invalidated",
                owner = invalidatedOwner,
            )
        }
        host.notifyMediaStateChanged()
    }
}
