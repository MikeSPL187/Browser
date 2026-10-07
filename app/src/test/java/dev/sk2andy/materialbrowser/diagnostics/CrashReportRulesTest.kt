package dev.sk2andy.materialbrowser.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportRulesTest {
    private val header = CrashReportHeader(
        versionName = "0.2.0",
        versionCode = 20,
        buildType = "release",
        flavor = "full",
        androidRelease = "15",
        sdkInt = 35,
        device = "Google Pixel 8",
    )

    @Test
    fun redactsEveryWebAddress() {
        val text = "Failed https://accounts.google.com/signin?x=1 and about:blank, " +
            "moz-extension://abc/page.html (data:text/html,hi)"

        assertEquals(
            "Failed https:<address removed> and about:<address removed>, " +
                "moz-extension:<address removed> (data:<address removed>)",
            CrashReportRules.redact(text),
        )
    }

    @Test
    fun keepsStackFramesAndPlainWords() {
        val text = "at dev.Foo.bar(MainActivity.kt:774)\nmetadata: none\ncontent: empty"

        assertEquals(text, CrashReportRules.redact(text))
    }

    @Test
    fun throwableReportHasHeaderTimeAndRedactedStack() {
        val report = CrashReportRules.forThrowable(
            header = header,
            timeMillis = 0,
            processLabel = "main",
            threadName = "main",
            stackTrace = "java.lang.IllegalStateException: https://example.test/secret\n\tat a.B.c(B.kt:1)",
        )

        assertEquals(
            "Process main stopped: uncaught exception on thread \"main\"\n" +
                "Time: 1970-01-01T00:00:00Z\n" +
                "Vola 0.2.0 (20, full release) · Android 15 (API 35) · Google Pixel 8\n" +
                "\n" +
                "java.lang.IllegalStateException: https:<address removed>\n" +
                "\tat a.B.c(B.kt:1)",
            report,
        )
    }

    @Test
    fun nativeCrashReportNamesProcessSignalAndBacktrace() {
        val exit = exit(ExitKind.NativeCrash, processName = "$PACKAGE:tab3", status = 11)
        val tombstone = TombstoneSummary(
            signalName = "SIGSEGV",
            signalCode = "SEGV_MAPERR",
            faultAddress = 0x10,
            abortMessage = null,
            threadName = "Gecko",
            frames = listOf("#00 pc 0000abcd  libxul.so"),
        )

        val report = CrashReportRules.forExit(header, exit, tombstone = tombstone)

        assertTrue(report.startsWith("Process :tab3 stopped: native crash (signal 11)\n"))
        assertTrue(report.contains("Signal: SIGSEGV / SEGV_MAPERR, fault address 0x10\n"))
        assertTrue(report.contains("Backtrace (thread Gecko):\n#00 pc 0000abcd  libxul.so"))
        assertTrue(report.contains("In the foreground"))
    }

    @Test
    fun onlyTheMainProcessIsFatal() {
        assertTrue(CrashReportRules.isFatal(exit(ExitKind.NativeCrash, processName = PACKAGE)))
        assertFalse(CrashReportRules.isFatal(exit(ExitKind.NativeCrash, processName = "$PACKAGE:gpu")))
        assertEquals("main", CrashReportRules.processLabel(PACKAGE, PACKAGE))
        assertEquals(":tab0", CrashReportRules.processLabel("$PACKAGE:tab0", PACKAGE))
    }

    @Test
    fun javaCrashAlreadyRecordedByTheAppIsSkipped() {
        val exit = exit(ExitKind.JavaCrash, time = 100_000)

        assertFalse(CrashReportRules.shouldRecord(exit, appReportTimes = listOf(99_000)))
        assertTrue(CrashReportRules.shouldRecord(exit, appReportTimes = listOf(10_000)))
        assertTrue(CrashReportRules.shouldRecord(exit, appReportTimes = emptyList()))
    }

    @Test
    fun onlyForegroundLowMemoryClosesAreRecorded() {
        assertTrue(CrashReportRules.shouldRecord(exit(ExitKind.LowMemory, foreground = true), emptyList()))
        assertFalse(CrashReportRules.shouldRecord(exit(ExitKind.LowMemory, foreground = false), emptyList()))
    }

    @Test
    fun firstScanLooksOneWeekBack() {
        val now = 10 * CrashReportRules.FIRST_SCAN_WINDOW_MILLIS

        assertEquals(now - CrashReportRules.FIRST_SCAN_WINDOW_MILLIS, CrashReportRules.scanSince(null, now))
        assertEquals(42L, CrashReportRules.scanSince(42L, now))
    }

    @Test
    fun anrTraceKeepsOnlyTheMainThread() {
        val trace = """
            ----- pid 123 -----
            "Signal Catcher" daemon prio=10
              at x.Y

            "main" prio=5 tid=1 Blocked
              at dev.Foo.wait(Foo.kt:3)
              at dev.Foo.run(Foo.kt:1)

            "Gecko" prio=5 tid=20 Native
              at z.Z
        """.trimIndent()

        assertEquals(
            "\"main\" prio=5 tid=1 Blocked\n  at dev.Foo.wait(Foo.kt:3)\n  at dev.Foo.run(Foo.kt:1)",
            CrashReportRules.anrMainThread(trace),
        )
        assertNull(CrashReportRules.anrMainThread("no threads here"))
    }

    @Test
    fun keepsTheFiveNewestReports() {
        val names = (1L..7L).map { CrashReportFile(it, CrashSource.App, fatal = true).fileName } +
            "notes.txt"

        assertEquals(
            listOf("2-app-fatal.txt", "1-app-fatal.txt"),
            CrashReportRules.overflow(names),
        )
    }

    @Test
    fun reportFileNameRoundTrips() {
        val file = CrashReportFile(1_700_000_000_000, CrashSource.Exit, fatal = false)

        assertEquals("1700000000000-exit-kept.txt", file.fileName)
        assertEquals(file, CrashReportFile.parse(file.fileName))
        assertNull(CrashReportFile.parse("1700-engine-fatal.txt"))
    }

    @Test
    fun combinedTextIsCapped() {
        val long = "x".repeat(CrashReportRules.MAX_TEXT_CHARS)

        val text = CrashReportRules.combine(listOf("first", long))

        assertTrue(text.startsWith("first\n\n----\n\n"))
        assertEquals(CrashReportRules.MAX_TEXT_CHARS + 2, text.length)
    }

    private fun exit(
        kind: ExitKind,
        processName: String = PACKAGE,
        time: Long = 1_000,
        foreground: Boolean = true,
        status: Int = 0,
    ) = ProcessExit(
        timeMillis = time,
        kind = kind,
        processName = processName,
        packageName = PACKAGE,
        foreground = foreground,
        status = status,
        description = null,
    )

    private companion object {
        const val PACKAGE = "io.github.mikespl187.vola"
    }
}
