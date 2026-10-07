package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureOnboardingStoreTest {
    @Test
    fun emptySessionIsNewInstall() {
        assertFalse(GestureOnboardingStore.isExistingInstall(emptySet()))
    }

    @Test
    fun historySessionFlagAloneIsNewInstall() {
        assertFalse(
            GestureOnboardingStore.isExistingInstall(setOf(BrowserSessionStore.KEY_HISTORY_SESSION_ACTIVE)),
        )
    }

    @Test
    fun savedTabsMeanAnUpdate() {
        assertTrue(
            GestureOnboardingStore.isExistingInstall(
                setOf(BrowserSessionStore.KEY_HISTORY_SESSION_ACTIVE, BrowserSessionStore.KEY_TABS),
            ),
        )
    }
}
