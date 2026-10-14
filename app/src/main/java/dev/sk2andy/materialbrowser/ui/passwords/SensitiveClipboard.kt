package dev.sk2andy.materialbrowser.ui.passwords

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import dev.sk2andy.materialbrowser.browser.integration.markedSensitive

/**
 * Copies a login or password marked as sensitive (Android hides it from the clipboard preview and
 * keyboard suggestions) and clears it a minute later. While Vola is in the background Android does
 * not say what the clipboard holds; the clip is then cleared anyway, since within that minute it is
 * most likely still the password.
 *
 * The minute is real time: the timer itself stops while the phone sleeps, so the Passwords screen
 * also clears an expired clip when it comes back ([clearIfExpired]).
 */
internal object SensitiveClipboard {
    private const val LABEL = "Vola password"
    private const val CLEAR_AFTER_MILLIS = 60_000L

    private val handler = Handler(Looper.getMainLooper())
    private var pendingClear: Runnable? = null

    /** When the copied clip expires, on the elapsed clock; null when nothing waits to be cleared. */
    private var deadline: Long? = null

    fun copy(context: Context, text: String) {
        val app = context.applicationContext
        val clipboard = app.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(LABEL, text).markedSensitive())
        pendingClear?.let(handler::removeCallbacks)
        deadline = SystemClock.elapsedRealtime() + CLEAR_AFTER_MILLIS
        val clear = Runnable { clear(clipboard) }
        pendingClear = clear
        handler.postDelayed(clear, CLEAR_AFTER_MILLIS)
    }

    /** Clears Vola's clip now if its minute ran out while the timer slept. */
    fun clearIfExpired(context: Context) {
        val due = deadline ?: return
        if (SystemClock.elapsedRealtime() < due) return
        val clipboard = context.applicationContext.getSystemService(ClipboardManager::class.java) ?: return
        clear(clipboard)
    }

    private fun clear(clipboard: ClipboardManager) {
        pendingClear?.let(handler::removeCallbacks)
        pendingClear = null
        deadline = null
        val description = runCatching { clipboard.primaryClipDescription }.getOrNull()
        if (description == null || description.label == LABEL) runCatching { clipboard.clearPrimaryClip() }
    }
}
