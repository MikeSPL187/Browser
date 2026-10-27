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
    fun everyFailedScanHasAWayOn() {
        listOf(V5SyncScanStatus.CodeExpired, V5SyncScanStatus.NotFound, V5SyncScanStatus.Failed).forEach { status ->
            assertEquals(status.name, V5SyncScanAction.Retry, V5SyncRules.scanAction(status))
        }
        assertEquals(V5SyncScanAction.OpenSettings, V5SyncRules.scanAction(V5SyncScanStatus.NoCamera))
        assertEquals(null, V5SyncRules.scanAction(V5SyncScanStatus.Scanning))
        assertEquals(null, V5SyncRules.scanAction(V5SyncScanStatus.Receiving))
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

    @Test
    fun confirmNamesOnlyWhatTheTransferBrings() {
        assertEquals("2 tabs, 3 bookmarks", V5SyncRules.confirmMessage("2 tabs", "3 bookmarks", true, true, "none"))
        assertEquals("3 bookmarks", V5SyncRules.confirmMessage("0 tabs", "3 bookmarks", false, true, "none"))
        assertEquals("none", V5SyncRules.confirmMessage("0 tabs", "0 bookmarks", false, false, "none"))
    }

    @Test
    fun `the countdown reads minutes and seconds and stops at zero`() {
        assertEquals("0:48", V5SyncRules.countdown(48))
        assertEquals("1:05", V5SyncRules.countdown(65))
        assertEquals("0:00", V5SyncRules.countdown(-3))
    }
}
