package dev.sk2andy.materialbrowser.diagnostics

import android.app.Application

/**
 * Vola's application: it only adds the crash journal's last step before Android's own handler, in
 * every process of the app. Keep it this light; the engine's content processes start here too.
 */
class VolaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashRecorder()
    }

    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val journal = CrashJournal(this)
        val process = CrashReportRules.processLabel(Application.getProcessName(), packageName)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val time = System.currentTimeMillis()
                journal.record(
                    CrashReportFile(
                        timeMillis = time,
                        source = CrashSource.App,
                        fatal = process == CrashReportRules.MAIN_PROCESS_LABEL,
                    ),
                    CrashReportRules.forThrowable(
                        header = CrashJournal.currentHeader(),
                        timeMillis = time,
                        processLabel = process,
                        threadName = thread.name,
                        stackTrace = throwable.stackTraceToString(),
                    ),
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
