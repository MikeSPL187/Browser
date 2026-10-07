package dev.sk2andy.materialbrowser.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import dev.sk2andy.materialbrowser.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * Vola's local crash journal (#123, H1): at most [CrashReportRules.MAX_REPORTS] text reports in
 * `no_backup`, outside backups and device transfer. Nothing is sent anywhere; the person copies or
 * shares the reports from the prompt after a crash.
 */
internal class CrashJournal(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.noBackupFilesDir, DIRECTORY_NAME)
    private val preferences by lazy {
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    /** Writes a report now, on the crashing thread: the process ends right after. */
    fun record(file: CrashReportFile, text: String) {
        runCatching {
            directory.mkdirs()
            File(directory, file.fileName).writeText(text)
            trim()
        }
    }

    /** Records the crashes, freezes and low-memory closes Android saw since the last scan. */
    @WorkerThread
    fun recordProcessExits() {
        val now = System.currentTimeMillis()
        val since = CrashReportRules.scanSince(
            lastScanMillis = preferences.getLong(KEY_LAST_EXIT_SCAN, -1L).takeIf { it >= 0 },
            nowMillis = now,
        )
        val exits = runCatching {
            appContext.getSystemService(ActivityManager::class.java)
                ?.getHistoricalProcessExitReasons(null, 0, 0)
                .orEmpty()
        }.getOrDefault(emptyList())
        val appReportTimes = reportFiles()
            .filter { it.source == CrashSource.App }
            .map(CrashReportFile::timeMillis)
        val header = currentHeader()
        exits
            .filter { it.timestamp > since }
            .forEach { info ->
                val exit = info.toProcessExit() ?: return@forEach
                if (!CrashReportRules.shouldRecord(exit, appReportTimes)) return@forEach
                val text = CrashReportRules.forExit(
                    header = header,
                    exit = exit,
                    tombstone = if (exit.kind == ExitKind.NativeCrash) info.readTombstone() else null,
                    anrTrace = if (exit.kind == ExitKind.NotResponding) info.readAnrTrace() else null,
                )
                record(
                    CrashReportFile(
                        timeMillis = exit.timeMillis,
                        source = CrashSource.Exit,
                        fatal = CrashReportRules.isFatal(exit),
                    ),
                    text,
                )
            }
        preferences.edit { putLong(KEY_LAST_EXIT_SCAN, maxOf(now, exits.maxOfOrNull { it.timestamp } ?: 0L)) }
    }

    /** Whether a crash closed Vola since the person last saw the prompt. */
    @WorkerThread
    fun hasUnseenFatalReport(): Boolean {
        val lastSeen = preferences.getLong(KEY_LAST_SEEN, 0L)
        return reportFiles().any { it.fatal && it.timeMillis > lastSeen }
    }

    /** Every stored report, newest first, as one text. */
    @WorkerThread
    fun combinedText(): String = CrashReportRules.combine(
        reportFiles().mapNotNull { file ->
            runCatching { File(directory, file.fileName).readText() }.getOrNull()
        },
    )

    fun markSeen() {
        val newest = reportFiles().maxOfOrNull(CrashReportFile::timeMillis) ?: return
        preferences.edit { putLong(KEY_LAST_SEEN, newest) }
    }

    private fun reportFiles(): List<CrashReportFile> = directory.list().orEmpty()
        .mapNotNull(CrashReportFile::parse)
        .sortedByDescending(CrashReportFile::timeMillis)

    private fun trim() {
        CrashReportRules.overflow(directory.list().orEmpty().toList())
            .forEach { File(directory, it).delete() }
    }

    private fun ApplicationExitInfo.toProcessExit(): ProcessExit? {
        val kind = when (reason) {
            ApplicationExitInfo.REASON_CRASH -> ExitKind.JavaCrash
            ApplicationExitInfo.REASON_CRASH_NATIVE -> ExitKind.NativeCrash
            ApplicationExitInfo.REASON_ANR -> ExitKind.NotResponding
            ApplicationExitInfo.REASON_LOW_MEMORY -> ExitKind.LowMemory
            else -> return null
        }
        return ProcessExit(
            timeMillis = timestamp,
            kind = kind,
            processName = processName.orEmpty(),
            packageName = appContext.packageName,
            foreground = importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE,
            status = status,
            description = description,
        )
    }

    private fun ApplicationExitInfo.readTombstone(): TombstoneSummary? = runCatching {
        traceInputStream?.use { stream -> stream.readAtMost(TombstoneSummary.MAX_BYTES) }
            ?.let(TombstoneSummary::parse)
    }.getOrNull()

    private fun ApplicationExitInfo.readAnrTrace(): String? = runCatching {
        traceInputStream?.use { stream ->
            stream.readAtMost(MAX_ANR_TRACE_BYTES).toString(Charsets.UTF_8)
        }
    }.getOrNull()

    /** `InputStream.readNBytes` arrived in API 33; this does the same on Android 12. */
    private fun InputStream.readAtMost(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (output.size() < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (read < 0) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        private const val BUFFER_BYTES = 64 * 1024
        private const val DIRECTORY_NAME = "crash_reports"
        private const val PREFERENCES_NAME = "crash_journal"
        private const val KEY_LAST_EXIT_SCAN = "last_exit_scan"
        private const val KEY_LAST_SEEN = "last_seen_report"
        private const val MAX_ANR_TRACE_BYTES = 2 * 1024 * 1024

        fun currentHeader(): CrashReportHeader = CrashReportHeader(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            buildType = BuildConfig.BUILD_TYPE,
            flavor = BuildConfig.FLAVOR,
            androidRelease = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
        )
    }
}
