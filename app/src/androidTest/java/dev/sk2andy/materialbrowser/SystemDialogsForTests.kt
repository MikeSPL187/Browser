package dev.sk2andy.materialbrowser

import android.app.Activity
import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Closes a system "isn't responding" dialog that holds the focus, and answers whether one was
 * there. On the loaded CI emulator System UI now and then stops answering; its dialog then covers
 * the screen and every injected tap or swipe lands in it instead of the app ("Page did not react
 * to 3 taps", focus on `Application Not Responding: com.android.systemui`, 16.10). Back answers
 * the dialog with "Wait". Call it before injecting input through the system.
 */
internal fun dismissNotRespondingDialogForTests(): Boolean {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    if (!automation.notRespondingDialogHasFocus()) return false
    repeat(DISMISS_ATTEMPTS) {
        automation.shell("input keyevent 4")
        val deadline = SystemClock.elapsedRealtime() + DISMISS_WAIT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (!automation.notRespondingDialogHasFocus()) return true
            SystemClock.sleep(100)
        }
    }
    return true
}

/**
 * Waits for [activity]'s window to hold the focus: a text field takes focus, and the keyboard
 * shows, only then. On the loaded CI emulator a hung System UI "isn't responding" dialog can hold
 * it instead (16.10), so the wait closes such a dialog; a timeout names the window that kept it.
 */
internal fun awaitWindowFocusForTests(activity: Activity, timeoutMillis: Long = WINDOW_FOCUS_TIMEOUT_MILLIS) {
    val deadline = SystemClock.elapsedRealtime() + timeoutMillis
    while (!activity.window.decorView.hasWindowFocus()) {
        if (SystemClock.elapsedRealtime() >= deadline) {
            throw AssertionError("The activity window never got focus; focus is on ${focusedWindowForTests()}")
        }
        dismissNotRespondingDialogForTests()
        SystemClock.sleep(100)
    }
}

/** Names the window that holds the input focus, as `dumpsys window` reports it. */
internal fun focusedWindowForTests(): String =
    InstrumentationRegistry.getInstrumentation().uiAutomation.currentFocusLine()?.trim() ?: "unknown"

private fun UiAutomation.notRespondingDialogHasFocus(): Boolean =
    currentFocusLine()?.contains(NOT_RESPONDING_WINDOW) == true

private fun UiAutomation.currentFocusLine(): String? =
    shell("dumpsys window")
        .lineSequence()
        .firstOrNull { line -> line.contains("mCurrentFocus") }

private fun UiAutomation.shell(command: String): String =
    ParcelFileDescriptor.AutoCloseInputStream(executeShellCommand(command))
        .bufferedReader()
        .use { reader -> reader.readText() }

private const val NOT_RESPONDING_WINDOW = "Application Not Responding"
private const val DISMISS_ATTEMPTS = 3
private const val DISMISS_WAIT_MILLIS = 2_000L
private const val WINDOW_FOCUS_TIMEOUT_MILLIS = 10_000L
