package dev.sk2andy.materialbrowser

import android.app.Activity
import android.os.Handler
import android.os.Process
import dev.sk2andy.materialbrowser.browser.BrowserMediaPlaybackService
import dev.sk2andy.materialbrowser.browser.ProtectionReportController
import dev.sk2andy.materialbrowser.data.AppDataArchiveRestore
import dev.sk2andy.materialbrowser.data.AppDataTransferLock
import java.io.File
import java.util.concurrent.ExecutorService

/** Coordinates browser startup and flushes persistent writers for app data transfer. */
internal object AppDataTransferHandover {
    fun flushWriters(
        protectionReport: ProtectionReportController,
        executor: ExecutorService,
        mainHandler: Handler,
        vararg writers: () -> Boolean,
        onReady: (Boolean) -> Unit,
    ) {
        protectionReport.flushForAppDataTransfer { reportReady ->
            if (!reportReady || executor.isShutdown) {
                onReady(false)
                return@flushForAppDataTransfer
            }
            executor.execute {
                val ready = runCatching { writers.map { it() }.all { it } }.getOrDefault(false)
                mainHandler.post { onReady(ready) }
            }
        }
    }

    /**
     * A restore the last run left half done, or a data transfer running in its own process, owns
     * the app data: the browser hands over (resuming the restore) instead of opening on top of it.
     * [onTransferStarted] runs once the browser took the transfer lock for the resumed restore.
     */
    fun yieldsTo(activity: Activity, restoreRecoveryMarker: File, onTransferStarted: () -> Unit): Boolean {
        if (AppDataArchiveRestore.hasInterruptedRestore(restoreRecoveryMarker)) {
            val lockToken = AppDataTransferLock.activate(activity, Process.myPid())
            if (lockToken != null) {
                onTransferStarted()
                BrowserMediaPlaybackService.clearForAppDataTransfer(activity.applicationContext)
                val started = runCatching {
                    activity.startActivity(
                        AppDataTransferContract.recoveryIntent(
                            context = activity,
                            mainProcessId = Process.myPid(),
                            lockToken = lockToken,
                        ),
                    )
                }.isSuccess
                if (!started) AppDataTransferLock.release(activity, lockToken)
            }
            return true
        }
        return AppDataTransferLock.isActive(activity)
    }
}
