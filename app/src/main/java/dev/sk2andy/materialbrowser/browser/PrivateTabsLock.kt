package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The decisions of «Lock on exit» for private tabs, kept apart so each one has a test. */
internal object PrivateTabsLockRules {
    /**
     * Leaving the app locks private tabs whenever the lock is on and there is a private tab to hide,
     * even with no strong biometric at hand: a lock that silently stops locking would show private
     * pages to whoever picks the phone up. The biometric only gates unlocking and turning the lock
     * on; without one the lock screen still leaves for regular tabs or closes every private tab.
     */
    fun locksOnLeave(enabled: Boolean, privateTabCount: Int): Boolean =
        enabled && privateTabCount > 0

    /**
     * Without a biometric the lock can still be turned off once no private tab is left: there is
     * nothing for it to reveal, and a lock that can never be turned off would outlive the sensor.
     */
    fun turnsOffWithoutBiometric(enabled: Boolean, privateTabCount: Int): Boolean =
        enabled && privateTabCount == 0

    /** While locked, every private tab is hidden: its page, its title, its address and its icon. */
    fun hides(locked: Boolean, tab: BrowserTab?): Boolean = locked && tab?.isIncognito == true

    /** Once the last private tab is closed there is nothing left to guard. */
    fun staysLocked(locked: Boolean, privateTabCount: Int): Boolean = locked && privateTabCount > 0

    /**
     * The fingerprint is asked for by itself once per lock screen, and only with the app in front;
     * after a cancel the owner asks with the button, so the prompt never loops.
     */
    fun promptsAutomatically(pending: Boolean, locked: Boolean, resumed: Boolean): Boolean =
        pending && locked && resumed

    /** The regular tab «Regular tabs» steps back to: the last one used, or none (a new tab opens). */
    fun regularTabToShow(tabs: List<BrowserTab>): BrowserTab? =
        tabs.filterNot(BrowserTab::isIncognito).maxByOrNull(BrowserTab::lastAccessedAt)
}

/**
 * «Lock on exit» for private tabs (proposal П9, board PrivateTab): after the app leaves the
 * screen, private tabs open again only with a strong biometric. Whether it is on is kept; whether
 * private tabs are locked lives in memory only, as private tabs themselves do.
 *
 * The controller lends what a lock does to the engine, shared with the workspace lock:
 * [suspendTabs] pauses the media of the given tabs and, when asked, detaches the selected page;
 * [resumeSelectedTab] brings that page back.
 */
class PrivateTabsLock internal constructor(
    loadEnabled: () -> Boolean,
    private val saveEnabled: (Boolean) -> Unit,
    private val tabs: () -> List<BrowserTab>,
    private val selectedTab: () -> BrowserTab?,
    private val canAuthenticate: () -> Boolean,
    private val authenticate: (ProfileAuthenticationPurpose, (Boolean) -> Unit) -> Unit,
    private val suspendTabs: (tabIds: Set<String>, hidesSelectedTab: Boolean) -> Unit,
    private val resumeSelectedTab: () -> Unit,
) {
    var enabled by mutableStateOf(loadEnabled())
        private set

    var isLocked by mutableStateOf(false)
        private set

    private var promptPending = false

    fun hides(tab: BrowserTab?): Boolean = PrivateTabsLockRules.hides(isLocked, tab)

    val canTurnOffWithoutBiometric: Boolean
        get() = PrivateTabsLockRules.turnsOffWithoutBiometric(enabled, tabs().count(BrowserTab::isIncognito))

    /**
     * Turning the lock on or off asks for the fingerprint first: on, to prove it works before it can
     * shut the owner out; off, so whoever holds an unlocked phone cannot simply drop it.
     */
    fun requestEnabled(value: Boolean) {
        if (value == enabled) return
        if (!value && !canAuthenticate() && canTurnOffWithoutBiometric) {
            enabled = false
            saveEnabled(false)
            return
        }
        if (!canAuthenticate()) return
        authenticate(ProfileAuthenticationPurpose.ConfigurePrivateTabsLock) { authenticated ->
            if (!authenticated) return@authenticate
            enabled = value
            saveEnabled(value)
            if (!value) release(resume = true)
        }
    }

    fun onAppBackgrounded() {
        if (isLocked) return
        val privateTabIds = tabs().filter(BrowserTab::isIncognito).mapTo(hashSetOf(), BrowserTab::id)
        if (!PrivateTabsLockRules.locksOnLeave(enabled, privateTabIds.size)) return
        isLocked = true
        promptPending = true
        suspendTabs(privateTabIds, selectedTab()?.isIncognito == true)
    }

    fun onPrivateTabCountChanged(count: Int) {
        if (!PrivateTabsLockRules.staysLocked(isLocked, count)) release(resume = false)
    }

    /** The lock screen came up: ask for the fingerprint once, now or when the app resumes. */
    fun onLockScreenShown(resumed: Boolean) {
        promptPending = true
        promptIfPending(resumed)
    }

    fun promptIfPending(resumed: Boolean) {
        if (!PrivateTabsLockRules.promptsAutomatically(promptPending, isLocked, resumed)) return
        unlock()
    }

    fun unlock() {
        if (!isLocked) return
        promptPending = false
        authenticate(ProfileAuthenticationPurpose.UnlockPrivateTabs) { authenticated ->
            if (authenticated) release(resume = true)
        }
    }

    private fun release(resume: Boolean) {
        promptPending = false
        if (!isLocked) return
        isLocked = false
        if (resume) resumeSelectedTab()
    }
}
