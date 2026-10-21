package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The biometric lock of protected workspaces (#500): which ones are locked now, when they lock
 * (leaving the app, closing it, after the cooldown) and the one authentication that may run at a
 * time. Whether a workspace is unlocked lives in [ProfileProtectionSession], process memory only.
 *
 * The controller lends what a lock does to the engine, shared with [PrivateTabsLock]:
 * [Host.suspendTabs] pauses the hidden tabs' media and detaches the selected page;
 * [Host.resumeSelectedTab] brings that page back.
 */
internal class ProfileLock(
    private val supported: () -> Boolean,
    private val askBiometric: (ProfileAuthenticationPurpose, (Boolean) -> Unit) -> Unit,
    private val host: Host,
) {
    /** The controller state a lock depends on. */
    interface Host {
        val profiles: List<BrowserProfile>
        val tabs: List<BrowserTab>
        val activeProfileId: String
        val isDestroyed: Boolean

        fun suspendTabs(tabIds: Set<String>, hidesSelectedTab: Boolean)

        fun resumeSelectedTab()

        /** A workspace in the background was unlocked: views showing it redraw. */
        fun refreshEngineViews()
    }

    var isSupported by mutableStateOf(supported())
        private set
    var lockedIds by mutableStateOf<Set<String>>(emptySet())
        private set
    private var authenticationInFlight = false

    /** Leaving the app voids an authentication still running: its answer comes too late. */
    private var generation = 0L

    /** Whether an unlock can be asked for now: there is a biometric and no prompt is showing. */
    val canAuthenticate: Boolean
        get() = isSupported && !authenticationInFlight

    /** After the workspaces load: every protected one this process has not unlocked is locked. */
    fun restore() {
        lockedIds = host.profiles.asSequence()
            .filter { profile ->
                profile.protection != null && !ProfileProtectionSession.isUnlocked(profile.id)
            }
            .mapTo(linkedSetOf(), BrowserProfile::id)
    }

    fun authenticate(purpose: ProfileAuthenticationPurpose, onComplete: (Boolean) -> Unit) {
        if (!isSupported || authenticationInFlight || host.isDestroyed) {
            onComplete(false)
            return
        }
        authenticationInFlight = true
        val startedAt = generation
        askBiometric(purpose) { authenticated ->
            authenticationInFlight = false
            onComplete(authenticated && !host.isDestroyed && startedAt == generation)
        }
    }

    fun unlock(profileId: String) {
        val isProtected = host.profiles.any { profile ->
            profile.id == profileId && profile.protection != null
        }
        if (!isProtected) return
        ProfileProtectionSession.unlock(profileId)
        lockedIds -= profileId
        if (profileId == host.activeProfileId) host.resumeSelectedTab() else host.refreshEngineViews()
    }

    /** [profileId] lost its protection or was deleted: nothing of it stays locked. */
    fun forget(profileId: String) {
        lockedIds -= profileId
        ProfileProtectionSession.forget(profileId)
    }

    fun onAppBackgrounded(nowElapsedRealtime: Long) {
        generation++
        ProfileProtectionSession.markBackgrounded(nowElapsedRealtime)
        lockWhere { protection -> protection.lockTrigger == ProfileLockTrigger.AppBackgrounded }
    }

    fun onAppForegrounded(nowElapsedRealtime: Long) {
        isSupported = supported()
        val elapsed = ProfileProtectionSession.consumeBackgroundElapsed(nowElapsedRealtime) ?: return
        lockWhere { protection ->
            ProfileProtectionRules.shouldLockAfterBackground(
                protection = protection,
                elapsedBackgroundMillis = elapsed,
            )
        }
    }

    fun onAppClosed() {
        lockWhere { protection -> protection.lockTrigger == ProfileLockTrigger.AppClosed }
    }

    private fun lockWhere(shouldLock: (ProfileProtection) -> Boolean) {
        val profileIds = host.profiles.asSequence()
            .filter { profile -> profile.protection?.let(shouldLock) == true }
            .mapTo(linkedSetOf(), BrowserProfile::id)
        if (profileIds.isEmpty()) return
        ProfileProtectionSession.lock(profileIds)
        lockedIds += profileIds
        host.suspendTabs(
            tabIds = host.tabs.filter { tab -> tab.profileId in profileIds }
                .mapTo(hashSetOf(), BrowserTab::id),
            hidesSelectedTab = host.activeProfileId in profileIds,
        )
    }
}
