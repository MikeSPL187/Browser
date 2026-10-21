package dev.sk2andy.materialbrowser

import android.app.Activity
import android.os.Process
import dev.sk2andy.materialbrowser.browser.BrowserMediaPlaybackService
import dev.sk2andy.materialbrowser.data.AppDataArchiveRestore
import dev.sk2andy.materialbrowser.data.AppDataTransferLock
import java.io.File

/** What the browser does at start when an app data transfer owns the data (#500, item 5). */
internal object AppDataTransferHandover {
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
