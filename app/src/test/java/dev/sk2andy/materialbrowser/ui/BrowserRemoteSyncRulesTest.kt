package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncDevice
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncFailure
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncState
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavDevice
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserRemoteSyncRulesTest {
    private val time = { millis: Long -> "t$millis" }

    @Test
    fun theStatusCardShowsTheServerTimesAndDevices() {
        val state = RemoteSyncState(
            server = "cloud.example.org",
            lastSyncedAtMillis = 42,
            syncing = true,
            lastError = RemoteSyncFailure.Unreachable,
            devices = listOf(RemoteSyncDevice("tablet", "Tablet", tabCount = 8, updatedAtMillis = 7)),
        )
        val status = BrowserRemoteSyncRules.status(state, time)
        assertEquals("cloud.example.org", status.host)
        assertEquals("t42", status.lastSynced)
        assertEquals(true, status.syncing)
        assertEquals(V5WebDavError.Unreachable, status.error)
        assertEquals(listOf(V5WebDavDevice("tablet", "Tablet", 8, "t7")), status.devices)
    }

    @Test
    fun neverSyncedAndNoErrorStayEmpty() {
        val status = BrowserRemoteSyncRules.status(RemoteSyncState(server = "s"), time)
        assertNull(status.lastSynced)
        assertNull(status.error)
    }

    @Test
    fun everyFailureHasItsScreenError() {
        val errors = RemoteSyncFailure.entries.map(BrowserRemoteSyncRules::error)
        assertEquals(errors.size, errors.toSet().size)
        assertEquals(V5WebDavError.PhraseMismatch, BrowserRemoteSyncRules.error(RemoteSyncFailure.PhraseMismatch))
        // A newer format must not reach the mismatch page, whose «Start over» deletes the data.
        assertEquals(V5WebDavError.NewerFormat, BrowserRemoteSyncRules.error(RemoteSyncFailure.NewerFormat))
    }

    @Test
    fun theMismatchPageNamesTheHost() {
        assertEquals("cloud.example.org", BrowserRemoteSyncRules.host(" https://anna@cloud.example.org/remote.php/dav "))
        assertEquals("cloud.example.org", BrowserRemoteSyncRules.host("cloud.example.org"))
    }
}
