package dev.sk2andy.materialbrowser.browser

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLockTest {
    private val home = BrowserProfile(id = "lock-test-home", emoji = "🏠")
    private val work = BrowserProfile(
        id = "lock-test-work",
        emoji = "💼",
        protection = ProfileProtection(ProfileLockTrigger.AppBackgrounded),
    )
    private val bank = BrowserProfile(
        id = "lock-test-bank",
        emoji = "🏦",
        protection = ProfileProtection(ProfileLockTrigger.Cooldown, cooldownMinutes = 5),
    )
    private val profiles = mutableListOf(home, work, bank)
    private val tabs = listOf(
        BrowserTab(id = "t-home", lastAccessedAt = 1, profileId = home.id),
        BrowserTab(id = "t-work", lastAccessedAt = 2, profileId = work.id),
    )
    private var active = work.id
    private var supported = true
    private var destroyed = false
    private val prompts = mutableListOf<(Boolean) -> Unit>()
    private val suspended = mutableListOf<Pair<Set<String>, Boolean>>()
    private var resumed = 0
    private var refreshed = 0

    private val lock = ProfileLock(
        supported = { supported },
        askBiometric = { _, onResult -> prompts += onResult },
        host = object : ProfileLock.Host {
            override val profiles get() = this@ProfileLockTest.profiles
            override val tabs get() = this@ProfileLockTest.tabs
            override val activeProfileId get() = active
            override val isDestroyed get() = destroyed
            override fun suspendTabs(tabIds: Set<String>, hidesSelectedTab: Boolean) {
                suspended += tabIds to hidesSelectedTab
            }
            override fun resumeSelectedTab() {
                resumed++
            }
            override fun refreshEngineViews() {
                refreshed++
            }
        },
    )

    @After
    fun forgetProcessState() {
        profiles.forEach { ProfileProtectionSession.forget(it.id) }
        ProfileProtectionSession.consumeBackgroundElapsed(0L)
    }

    @Test
    fun aFreshProcessStartsWithEveryProtectedWorkspaceLocked() {
        lock.restore()

        assertEquals(setOf(work.id, bank.id), lock.lockedIds)
    }

    @Test
    fun unlockingTheActiveWorkspaceBringsItsPageBack() {
        lock.restore()

        lock.unlock(work.id)

        assertEquals(setOf(bank.id), lock.lockedIds)
        assertTrue(ProfileProtectionSession.isUnlocked(work.id))
        assertEquals(1, resumed)
        assertEquals(0, refreshed)
    }

    @Test
    fun unlockingABackgroundWorkspaceOnlyRedraws() {
        lock.restore()

        lock.unlock(bank.id)

        assertEquals(0, resumed)
        assertEquals(1, refreshed)
    }

    @Test
    fun anUnprotectedWorkspaceIsNeverUnlocked() {
        lock.unlock(home.id)

        assertFalse(ProfileProtectionSession.isUnlocked(home.id))
        assertEquals(0, resumed + refreshed)
    }

    @Test
    fun leavingTheAppLocksWhatLocksOnLeaveAndHidesTheSelectedPage() {
        lock.unlock(work.id)
        lock.unlock(bank.id)

        lock.onAppBackgrounded(nowElapsedRealtime = 1_000L)

        assertEquals(setOf(work.id), lock.lockedIds)
        assertEquals(listOf(setOf("t-work") to true), suspended)
    }

    @Test
    fun theCooldownLocksOnlyOnceItHasPassed() {
        lock.unlock(bank.id)

        lock.onAppBackgrounded(nowElapsedRealtime = 0L)
        lock.onAppForegrounded(nowElapsedRealtime = 60_000L)
        assertFalse(bank.id in lock.lockedIds)

        lock.onAppBackgrounded(nowElapsedRealtime = 100_000L)
        lock.onAppForegrounded(nowElapsedRealtime = 100_000L + 5 * 60_000L)
        assertTrue(bank.id in lock.lockedIds)
    }

    @Test
    fun onlyOnePromptRunsAndAnAnswerAfterLeavingTheAppIsRefused() {
        val answers = mutableListOf<Boolean>()

        lock.authenticate(ProfileAuthenticationPurpose.Unlock) { answers += it }
        assertFalse(lock.canAuthenticate)
        lock.authenticate(ProfileAuthenticationPurpose.Unlock) { answers += it }
        assertEquals(listOf(false), answers)

        lock.onAppBackgrounded(nowElapsedRealtime = 0L)
        prompts.single()(true)

        assertEquals(listOf(false, false), answers)
        assertTrue(lock.canAuthenticate)
    }

    @Test
    fun withoutABiometricNothingIsAsked() {
        supported = false
        lock.onAppForegrounded(nowElapsedRealtime = 0L)
        var answer: Boolean? = null

        lock.authenticate(ProfileAuthenticationPurpose.Configure) { answer = it }

        assertEquals(false, answer)
        assertTrue(prompts.isEmpty())
        assertFalse(lock.isSupported)
    }

    @Test
    fun forgettingAWorkspaceUnlocksNothingButDropsItFromTheLockedSet() {
        lock.restore()

        lock.forget(work.id)

        assertEquals(setOf(bank.id), lock.lockedIds)
        assertFalse(ProfileProtectionSession.isUnlocked(work.id))
    }

    @Test
    fun closingTheAppLocksOnlyWhatLocksOnClose() {
        profiles[0] = home.copy(protection = ProfileProtection(ProfileLockTrigger.AppClosed))
        active = home.id

        lock.onAppClosed()

        assertEquals(setOf(home.id), lock.lockedIds)
        assertEquals(listOf(setOf("t-home") to true), suspended)
    }
}
