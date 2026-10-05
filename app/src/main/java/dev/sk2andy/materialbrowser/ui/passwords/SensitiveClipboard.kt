package dev.sk2andy.materialbrowser.ui.passwords

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * Copies a login or password marked as sensitive (Android hides it from the clipboard preview and
 * keyboard suggestions) and clears it a minute later. While Vola is in the background Android does
 * not say what the clipboard holds; the clip is then cleared anyway, since within that minute it is
 * most likely still the password.
 */
internal object SensitiveClipboard {
    private const val LABEL = "Vola password"
    private const val CLEAR_AFTER_MILLIS = 60_000L

    /** `ClipDescription.EXTRA_IS_SENSITIVE`, spelled out so it also reaches Android 12. */
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
    private val handler = Handler(Looper.getMainLooper())
    private var pendingClear: Runnable? = null

    fun copy(context: Context, text: String) {
        val app = context.applicationContext
        val clipboard = app.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(LABEL, text)
        clip.description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
        clipboard.setPrimaryClip(clip)
        pendingClear?.let(handler::removeCallbacks)
        val clear = Runnable {
            pendingClear = null
            val description = runCatching { clipboard.primaryClipDescription }.getOrNull()
            if (description == null || description.label == LABEL) runCatching { clipboard.clearPrimaryClip() }
        }
        pendingClear = clear
        handler.postDelayed(clear, CLEAR_AFTER_MILLIS)
    }
}
