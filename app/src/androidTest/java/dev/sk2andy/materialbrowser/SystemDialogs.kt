package dev.sk2andy.materialbrowser

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import java.util.regex.Pattern

/**
 * On a loaded emulator the system's own UI can stop answering, and Android puts up its
 * «isn't responding» dialog over everything: every tap and back then goes to that dialog. It is
 * not the app's; a user taps «Wait», and so do the tests.
 */
internal fun UiDevice.dismissSystemNotResponding() {
    if ("Not Responding" !in focusedWindow()) return
    val wait = findObject(By.res("android", "aerr_wait"))
        ?: findObject(By.text(Pattern.compile("(?i)wait|подождать")))
    if (wait != null) {
        wait.click()
    } else {
        executeShellCommand("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
    }
    waitForIdle()
}

/** The window holding focus, as the window manager names it: a system dialog shows here. */
internal fun UiDevice.focusedWindow(): String = runCatching {
    executeShellCommand("dumpsys window")
        .lineSequence()
        .firstOrNull { line -> "mCurrentFocus" in line }
        ?.trim()
        ?: currentPackageName
}.getOrDefault("?")
