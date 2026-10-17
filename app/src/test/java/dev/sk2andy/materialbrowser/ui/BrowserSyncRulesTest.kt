package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferFailure
import dev.sk2andy.materialbrowser.ui.v5.V5SyncScanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserSyncRulesTest {
    @Test
    fun anExpiredOrAttackedCodeIsReplacedOtherFailuresMeanNoWifi() {
        assertTrue(BrowserSyncRules.offersAgain(LocalTransferFailure.Expired))
        assertTrue(BrowserSyncRules.offersAgain(LocalTransferFailure.TooManyAttempts))
        assertFalse(BrowserSyncRules.offersAgain(LocalTransferFailure.Unavailable))
        assertFalse(BrowserSyncRules.offersAgain(LocalTransferFailure.Unreachable))
    }

    @Test
    fun scanFailuresSayWhatHappened() {
        assertEquals(V5SyncScanStatus.CodeExpired, BrowserSyncRules.scanStatus(LocalTransferFailure.Expired))
        assertEquals(V5SyncScanStatus.NotFound, BrowserSyncRules.scanStatus(LocalTransferFailure.Unreachable))
        assertEquals(V5SyncScanStatus.NotFound, BrowserSyncRules.scanStatus(LocalTransferFailure.Unavailable))
        listOf(
            LocalTransferFailure.InvalidCode,
            LocalTransferFailure.TooManyAttempts,
            LocalTransferFailure.Corrupt,
            LocalTransferFailure.TooLarge,
        ).forEach { reason -> assertEquals(reason.name, V5SyncScanStatus.Failed, BrowserSyncRules.scanStatus(reason)) }
    }
}
