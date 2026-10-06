package dev.sk2andy.materialbrowser.diagnostics

import java.time.Instant

/** Where a report came from: Vola's own uncaught exception, or Android's record of a process exit. */
internal enum class CrashSource(val fileTag: String) {
    App("app"),
    Exit("exit"),
}

/** The process exits worth a report; Android records many more (swipes, updates, restarts). */
internal enum class ExitKind {
    JavaCrash,
    NativeCrash,
    NotResponding,
    LowMemory,
}

/** One process exit from Android's history (`ApplicationExitInfo`), without Android types. */
internal data class ProcessExit(
    val timeMillis: Long,
    val kind: ExitKind,
    val processName: String,
    val packageName: String,
    val foreground: Boolean,
    val status: Int,
    val description: String?,
)

/** The build and device a report was written on; nothing about the user or their pages. */
internal data class CrashReportHeader(
    val versionName: String,
    val versionCode: Long,
    val buildType: String,
    val flavor: String,
    val androidRelease: String,
    val sdkInt: Int,
    val device: String,
)

/**
 * The text of a local crash report (#123, H1). Reports stay on the device until the person copies
 * or shares them; they hold no page content or logs, and every web address is cut out in case an
 * exception or abort message carried one (a private tab's, say).
 */
internal object CrashReportRules {
    const val MAX_REPORTS = 5
    const val MAIN_PROCESS_LABEL = "main"
    const val MAX_TEXT_CHARS = 32_000

    /** On the first scan, exits older than this are left out: they predate the journal. */
    const val FIRST_SCAN_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000

    /** How long before its process exit Vola's own handler may have written the same crash. */
    private const val SAME_CRASH_WINDOW_MILLIS = 30_000L
    private const val ANR_THREAD_LINES = 60

    private val address = Regex(
        """(?i)\b(?:https?|wss?|ftp|file|content|blob|data|about|moz-extension|resource|chrome|intent|javascript):[^\s"'<>()\[\]{}]*[^\s"'<>()\[\]{},.;:!?]""",
    )

    fun redact(text: String): String = address.replace(text) { match ->
        match.value.substringBefore(':').lowercase() + ":<address removed>"
    }

    fun forThrowable(
        header: CrashReportHeader,
        timeMillis: Long,
        processLabel: String,
        threadName: String,
        stackTrace: String,
    ): String = compose(
        header = header,
        timeMillis = timeMillis,
        title = "Process $processLabel stopped: uncaught exception on thread \"$threadName\"",
        body = stackTrace,
    )

    fun forExit(
        header: CrashReportHeader,
        exit: ProcessExit,
        tombstone: TombstoneSummary? = null,
        anrTrace: String? = null,
    ): String {
        val process = processLabel(exit)
        val title = when (exit.kind) {
            ExitKind.JavaCrash -> "Process $process stopped: uncaught exception"
            ExitKind.NativeCrash -> "Process $process stopped: native crash (signal ${exit.status})"
            ExitKind.NotResponding -> "Process $process was not responding and was closed"
            ExitKind.LowMemory -> "Process $process was closed by Android to free memory"
        }
        val body = buildString {
            exit.description?.takeIf(String::isNotBlank)?.let { appendLine("Description: $it") }
            appendLine(if (exit.foreground) "In the foreground" else "In the background")
            tombstone?.let { append(it.render()) }
            anrTrace?.let(::anrMainThread)?.let { thread ->
                appendLine()
                append(thread)
            }
        }.trimEnd()
        return compose(header, exit.timeMillis, title, body)
    }

    /** `main` for Vola's own process, the `:suffix` for the others (engine content, GPU…). */
    fun processLabel(processName: String, packageName: String): String = when {
        processName == packageName -> MAIN_PROCESS_LABEL
        processName.startsWith("$packageName:") -> processName.removePrefix(packageName)
        else -> processName
    }

    private fun processLabel(exit: ProcessExit): String =
        processLabel(exit.processName, exit.packageName)

    /** A crash of Vola's own process closed the app; any other process left it running. */
    fun isFatal(exit: ProcessExit): Boolean = exit.processName == exit.packageName

    /**
     * Whether [exit] needs a report of its own. An uncaught exception already has Vola's report
     * with the full stack ([appReportTimes]), and a background process freed for memory is routine.
     */
    fun shouldRecord(exit: ProcessExit, appReportTimes: List<Long>): Boolean = when (exit.kind) {
        ExitKind.JavaCrash -> appReportTimes.none { time ->
            time in (exit.timeMillis - SAME_CRASH_WINDOW_MILLIS)..exit.timeMillis + 1_000
        }
        ExitKind.LowMemory -> exit.foreground
        ExitKind.NativeCrash, ExitKind.NotResponding -> true
    }

    /** The earliest exit time to read: after the last scan, or a week back on the first one. */
    fun scanSince(lastScanMillis: Long?, nowMillis: Long): Long =
        lastScanMillis ?: (nowMillis - FIRST_SCAN_WINDOW_MILLIS)

    /** The main thread's stack from an ANR trace; the other threads add length, not cause. */
    fun anrMainThread(trace: String): String? {
        val lines = trace.lineSequence().dropWhile { !it.startsWith("\"main\"") }.toList()
        if (lines.isEmpty()) return null
        return lines.takeWhile(String::isNotBlank).take(ANR_THREAD_LINES).joinToString("\n")
    }

    /** Every stored report in one text, newest first, as copied or shared. */
    fun combine(reports: List<String>): String {
        val text = reports.joinToString("\n\n----\n\n")
        return if (text.length <= MAX_TEXT_CHARS) text else text.take(MAX_TEXT_CHARS) + "\n…"
    }

    /** File names of reports to delete so that at most [MAX_REPORTS] remain, oldest first. */
    fun overflow(fileNames: List<String>): List<String> = fileNames
        .filter { CrashReportFile.parse(it) != null }
        .sortedByDescending { CrashReportFile.parse(it)?.timeMillis ?: 0L }
        .drop(MAX_REPORTS)

    private fun compose(
        header: CrashReportHeader,
        timeMillis: Long,
        title: String,
        body: String,
    ): String {
        val text = buildString {
            appendLine(title)
            appendLine("Time: ${Instant.ofEpochMilli(timeMillis)}")
            appendLine(
                "Vola ${header.versionName} (${header.versionCode}, ${header.flavor} ${header.buildType}) · " +
                    "Android ${header.androidRelease} (API ${header.sdkInt}) · ${header.device}",
            )
            appendLine()
            append(redact(body))
        }
        return if (text.length <= MAX_TEXT_CHARS) text else text.take(MAX_TEXT_CHARS) + "\n…"
    }
}

/** A report's file name: `<time>-<source>-<fatal|kept>.txt`, so the journal needs no index. */
internal data class CrashReportFile(
    val timeMillis: Long,
    val source: CrashSource,
    val fatal: Boolean,
) {
    val fileName: String
        get() = "$timeMillis-${source.fileTag}-${if (fatal) FATAL else KEPT}.txt"

    companion object {
        private const val FATAL = "fatal"
        private const val KEPT = "kept"
        private val pattern = Regex("""(\d+)-(app|exit)-(fatal|kept)\.txt""")

        fun parse(fileName: String): CrashReportFile? {
            val match = pattern.matchEntire(fileName) ?: return null
            val (time, source, fatal) = match.destructured
            return CrashReportFile(
                timeMillis = time.toLongOrNull() ?: return null,
                source = CrashSource.entries.first { it.fileTag == source },
                fatal = fatal == FATAL,
            )
        }
    }
}
