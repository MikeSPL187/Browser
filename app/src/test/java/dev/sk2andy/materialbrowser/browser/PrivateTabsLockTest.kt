package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateTabsLockRulesTest {
    @Test
    fun `leaving locks only when on, with a private tab and a biometric to come back with`() {
        assertTrue(PrivateTabsLockRules.locksOnLeave(enabled = true, privateTabCount = 1, canAuthenticate = true))
        assertFalse(PrivateTabsLockRules.locksOnLeave(enabled = false, privateTabCount = 1, canAuthenticate = true))
        assertFalse(PrivateTabsLockRules.locksOnLeave(enabled = true, privateTabCount = 0, canAuthenticate = true))
        assertFalse(PrivateTabsLockRules.locksOnLeave(enabled = true, privateTabCount = 2, canAuthenticate = false))
    }

    @Test
    fun `a lock hides private tabs only`() {
        val private = BrowserTab("private", 1L, isIncognito = true)
        val regular = BrowserTab("regular", 1L)
        assertTrue(PrivateTabsLockRules.hides(locked = true, tab = private))
        assertFalse(PrivateTabsLockRules.hides(locked = true, tab = regular))
        assertFalse(PrivateTabsLockRules.hides(locked = false, tab = private))
        assertFalse(PrivateTabsLockRules.hides(locked = true, tab = null))
    }

    @Test
    fun `closing the last private tab ends the lock`() {
        assertTrue(PrivateTabsLockRules.staysLocked(locked = true, privateTabCount = 1))
        assertFalse(PrivateTabsLockRules.staysLocked(locked = true, privateTabCount = 0))
        assertFalse(PrivateTabsLockRules.staysLocked(locked = false, privateTabCount = 3))
    }

    @Test
    fun `the fingerprint is asked for by itself only once, locked and in front`() {
        assertTrue(PrivateTabsLockRules.promptsAutomatically(pending = true, locked = true, resumed = true))
        assertFalse(PrivateTabsLockRules.promptsAutomatically(pending = false, locked = true, resumed = true))
        assertFalse(PrivateTabsLockRules.promptsAutomatically(pending = true, locked = false, resumed = true))
        assertFalse(PrivateTabsLockRules.promptsAutomatically(pending = true, locked = true, resumed = false))
    }

    @Test
    fun `regular tabs step back to the last regular tab used, never a private one`() {
        val tabs = listOf(
            BrowserTab("old", 10L),
            BrowserTab("private", 99L, isIncognito = true),
            BrowserTab("recent", 50L),
        )
        assertEquals("recent", PrivateTabsLockRules.regularTabToShow(tabs)?.id)
        assertNull(PrivateTabsLockRules.regularTabToShow(listOf(BrowserTab("p", 1L, isIncognito = true))))
    }
}

class PrivateTabsLockTest {
    private val tabs = mutableListOf(
        BrowserTab("regular", 1L),
        BrowserTab("private", 2L, isIncognito = true),
    )
    private var selectedId = "private"
    private var canAuthenticate = true
    private var saved: Boolean? = null
    private val prompts = mutableListOf<ProfileAuthenticationPurpose>()
    private var answer = true
    private val suspended = mutableListOf<Pair<Set<String>, Boolean>>()
    private var resumed = 0

    private fun lock(enabled: Boolean = true) = PrivateTabsLock(
        loadEnabled = { enabled },
        saveEnabled = { saved = it },
        tabs = { tabs },
        selectedTab = { tabs.firstOrNull { it.id == selectedId } },
        canAuthenticate = { canAuthenticate },
        authenticate = { purpose, onResult ->
            prompts += purpose
            onResult(answer)
        },
        suspendTabs = { ids, hidesSelected -> suspended += ids to hidesSelected },
        resumeSelectedTab = { resumed++ },
    )

    @Test
    fun `leaving the app locks and suspends the private tabs`() {
        val lock = lock()
        lock.onAppBackgrounded()
        assertTrue(lock.isLocked)
        assertEquals(listOf(setOf("private") to true), suspended)
        assertTrue(lock.hides(tabs[1]))
        assertFalse(lock.hides(tabs[0]))
    }

    @Test
    fun `a regular selected tab stays attached while private tabs lock`() {
        selectedId = "regular"
        val lock = lock()
        lock.onAppBackgrounded()
        assertEquals(listOf(setOf("private") to false), suspended)
    }

    @Test
    fun `nothing locks when off, without private tabs or without a biometric`() {
        lock(enabled = false).apply { onAppBackgrounded() }.also { assertFalse(it.isLocked) }
        canAuthenticate = false
        lock().apply { onAppBackgrounded() }.also { assertFalse(it.isLocked) }
        canAuthenticate = true
        tabs.removeAll(BrowserTab::isIncognito)
        lock().apply { onAppBackgrounded() }.also { assertFalse(it.isLocked) }
        assertTrue(suspended.isEmpty())
    }

    @Test
    fun `unlocking takes the fingerprint and brings the page back`() {
        val lock = lock()
        lock.onAppBackgrounded()
        answer = false
        lock.unlock()
        assertTrue(lock.isLocked)
        answer = true
        lock.unlock()
        assertFalse(lock.isLocked)
        assertEquals(1, resumed)
        assertEquals(
            listOf(ProfileAuthenticationPurpose.UnlockPrivateTabs, ProfileAuthenticationPurpose.UnlockPrivateTabs),
            prompts,
        )
    }

    @Test
    fun `the lock screen asks once by itself, and waits for the app to come to the front`() {
        val lock = lock()
        lock.onAppBackgrounded()
        answer = false
        lock.onLockScreenShown(resumed = false)
        assertTrue(prompts.isEmpty())
        lock.promptIfPending(resumed = true)
        assertEquals(1, prompts.size)
        // Cancelled: resuming again does not ask again, the button does.
        lock.promptIfPending(resumed = true)
        assertEquals(1, prompts.size)
    }

    @Test
    fun `closing every private tab releases the lock without a fingerprint`() {
        val lock = lock()
        lock.onAppBackgrounded()
        lock.onPrivateTabCountChanged(1)
        assertTrue(lock.isLocked)
        lock.onPrivateTabCountChanged(0)
        assertFalse(lock.isLocked)
        assertTrue(prompts.isEmpty())
    }

    @Test
    fun `turning the lock on or off asks for the fingerprint, and off also unlocks`() {
        val lock = lock(enabled = false)
        answer = false
        lock.requestEnabled(true)
        assertFalse(lock.enabled)
        assertNull(saved)
        answer = true
        lock.requestEnabled(true)
        assertTrue(lock.enabled)
        assertEquals(true, saved)
        lock.onAppBackgrounded()
        lock.requestEnabled(false)
        assertFalse(lock.enabled)
        assertFalse(lock.isLocked)
        assertEquals(1, resumed)
        assertTrue(prompts.all { it == ProfileAuthenticationPurpose.ConfigurePrivateTabsLock })
    }

    @Test
    fun `the setting cannot change without a biometric`() {
        canAuthenticate = false
        val lock = lock(enabled = false)
        lock.requestEnabled(true)
        assertFalse(lock.enabled)
        assertTrue(prompts.isEmpty())
    }
}
