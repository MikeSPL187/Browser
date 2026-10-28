package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncFailure
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncState
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileSync
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserSettingsProfileRulesTest {
    private val ago: (Long) -> String = { "$it ago" }
    private val connected = RemoteSyncState(server = "cloud.example")

    @Test
    fun `the card tells how the own server's sync stands`() {
        assertEquals(V5ProfileSync.Unavailable, BrowserSettingsProfileRules.sync(false, connected, ago))
        assertEquals(V5ProfileSync.Off, BrowserSettingsProfileRules.sync(true, RemoteSyncState(), ago))
        assertEquals(V5ProfileSync.Syncing, BrowserSettingsProfileRules.sync(true, connected.copy(syncing = true), ago))
        assertEquals(
            V5ProfileSync.Failed,
            BrowserSettingsProfileRules.sync(true, connected.copy(lastError = RemoteSyncFailure.Unreachable), ago),
        )
        assertEquals(V5ProfileSync.Synced("5 ago"), BrowserSettingsProfileRules.sync(true, connected.copy(lastSyncedAtMillis = 5), ago))
        assertEquals(V5ProfileSync.Synced(null), BrowserSettingsProfileRules.sync(true, connected, ago))
    }
}
