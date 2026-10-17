package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V5SyncRulesTest {
    @Test
    fun theCameraRunsOnlyWhileThereIsACodeToFind() {
        assertTrue(V5SyncRules.showsViewfinder(V5SyncScanStatus.Scanning))
        assertTrue(V5SyncRules.showsViewfinder(V5SyncScanStatus.Receiving))
        listOf(
            V5SyncScanStatus.CodeExpired,
            V5SyncScanStatus.NotFound,
            V5SyncScanStatus.Failed,
            V5SyncScanStatus.NoCamera,
        ).forEach { status -> assertFalse(status.name, V5SyncRules.showsViewfinder(status)) }
    }

    @Test
    fun everyStatusHasItsOwnTitle() {
        val titles = V5SyncScanStatus.entries.map(V5SyncRules::scanTitle)
        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun errorsSayWhatToDoNext() {
        assertEquals(R.string.v5_sync_error_expired_hint, V5SyncRules.scanMessage(V5SyncScanStatus.CodeExpired))
        assertEquals(R.string.v5_sync_error_not_found_hint, V5SyncRules.scanMessage(V5SyncScanStatus.NotFound))
        assertEquals(R.string.v5_sync_error_camera_hint, V5SyncRules.scanMessage(V5SyncScanStatus.NoCamera))
        assertNull(V5SyncRules.scanMessage(V5SyncScanStatus.Receiving))
    }
}
