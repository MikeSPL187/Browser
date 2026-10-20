package dev.sk2andy.materialbrowser.data

import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import java.io.File

/**
 * The browsing history in a file of its own (N-02, V-07): out of the shared preferences, which
 * Android reads whole at start-up and rewrote whole on every visit. The file is written atomically,
 * so a crash leaves the old history or the new one, never half. The history moves here from the
 * preferences once, the file first and the old copy after. A file that exists but cannot be read
 * refuses every write but a clear, so one read error never replaces the history with nothing.
 */
internal class HistoryFile private constructor(private val file: AtomicFile) {
    private var unreadable = false

    /** Writes made by this process, so a reader can tell its copy is old even within a second. */
    private var generation = 0L

    /** Changes whenever the saved history may have: a write here, or the file replaced from outside. */
    @Synchronized
    fun stamp(): String = "$generation:${file.baseFile.lastModified()}:${file.baseFile.length()}"

    /** The history's text; null when nothing was saved yet. */
    @Synchronized
    fun read(preferences: SharedPreferences, legacyKey: String): String? {
        if (!file.baseFile.exists()) {
            val legacy = preferences.getString(legacyKey, null) ?: return null
            if (writeText(legacy)) preferences.edit().remove(legacyKey).commit()
            return legacy
        }
        return runCatching { file.readFully().decodeToString() }
            .onSuccess { unreadable = false }
            .getOrElse {
                unreadable = true
                null
            }
    }

    /** Saves [text]; [clearing] (an empty history) is the one write allowed after a failed read. */
    @Synchronized
    fun write(text: String, clearing: Boolean): Boolean {
        if (unreadable && !clearing) return false
        return writeText(text).also { written -> if (written) unreadable = false }
    }

    private fun writeText(text: String): Boolean {
        val stream = runCatching { file.startWrite() }.getOrNull() ?: return false
        return runCatching {
            stream.write(text.encodeToByteArray())
            file.finishWrite(stream)
            generation++
            true
        }.getOrElse {
            file.failWrite(stream)
            false
        }
    }

    internal companion object {
        const val FILE_NAME = "browsing_history_v1.json"

        @Volatile
        private var instance: HistoryFile? = null

        /** One per app: every [BrowserSessionStore] shares its lock and its read state. */
        fun forContext(context: Context): HistoryFile = instance ?: synchronized(this) {
            instance ?: HistoryFile(AtomicFile(File(context.applicationContext.filesDir, FILE_NAME)))
                .also { instance = it }
        }

        /** A file at [path], for tests that need a fresh one. */
        internal fun forTesting(path: File): HistoryFile = HistoryFile(AtomicFile(path))
    }
}
