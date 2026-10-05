package dev.sk2andy.materialbrowser.browser.credentials.vault

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVault
import dev.sk2andy.materialbrowser.ui.passwords.VaultAutoLock

/**
 * Keeps the open vault open only while it is in use: every use restarts a five-minute timer, and
 * the vault locks when it runs out. The timer cannot run in deep sleep, so screens also check on
 * resume ([lockIfExpired]) against the monotonic clock.
 */
internal object CredentialVaultSession {
    private val handler = Handler(Looper.getMainLooper())
    private val autoLock = VaultAutoLock()
    private var vault: CredentialVault? = null
    private val lockTask = Runnable { lockNow() }

    /** Changes every time the vault locks, so open screens fall back to the lock screen. */
    var lockGeneration by mutableIntStateOf(0)
        private set

    @MainThread
    fun touch(vault: CredentialVault) {
        this.vault = vault
        autoLock.touch(SystemClock.elapsedRealtime())
        handler.removeCallbacks(lockTask)
        handler.postDelayed(lockTask, VaultAutoLock.DEFAULT_TIMEOUT_MILLIS)
    }

    @MainThread
    fun lockIfExpired() {
        if (vault?.isUnlocked == true && autoLock.isExpired(SystemClock.elapsedRealtime())) lockNow()
    }

    @MainThread
    fun lockNow() {
        handler.removeCallbacks(lockTask)
        vault?.lock()
        autoLock.reset()
        lockGeneration++
    }
}
