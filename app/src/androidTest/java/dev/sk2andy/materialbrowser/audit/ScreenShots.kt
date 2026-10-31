package dev.sk2andy.materialbrowser.audit

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

/**
 * Screenshots of the screens the instrumented walk and the component shots visit, for the
 * Screenshots workflow (`screenshots.yml`), which publishes them to the «screenshots» branch.
 *
 * Off unless the run passes `walkScreenshots=true`. Each picture is taken by the shell's own
 * `screencap` into [DIRECTORY]: the shell may write there without any permission, and the files
 * outlive the app data the orchestrator clears between tests, so the workflow pulls them all at
 * the end.
 */
internal object ScreenShots {
    /** Where the pictures go on the device; the workflow pulls the whole directory. */
    const val DIRECTORY = "/data/local/tmp/vola-shots"

    private const val ARGUMENT = "walkScreenshots"
    private const val CONFIGS_ARGUMENT = "walkConfigs"

    /** The walk configurations photographed when the run names none: the owner's phone, and its opposite. */
    private val DEFAULT_CONFIGS = listOf(AuditConfig.RussianDark, AuditConfig.EnglishLight)

    val enabled: Boolean
        get() = InstrumentationRegistry.getArguments().getString(ARGUMENT) == "true"

    /**
     * The configurations to walk while taking pictures: those named in `walkConfigs`
     * (comma-separated `AuditConfig` field names, such as `RussianDark,EnglishLight`), or [DEFAULT_CONFIGS].
     */
    fun configs(all: Map<String, AuditConfig>): List<AuditConfig> {
        val named = InstrumentationRegistry.getArguments().getString(CONFIGS_ARGUMENT)
            ?.split(',')
            ?.mapNotNull { name -> all[name.trim()] }
            .orEmpty()
        return named.ifEmpty { DEFAULT_CONFIGS }
    }

    /** Takes the whole screen as `<prefix>-<name>.png`; nothing when pictures are off. */
    fun capture(prefix: String, name: String) {
        if (!enabled) return
        shell("mkdir -p $DIRECTORY")
        shell("screencap -p $DIRECTORY/${fileName(prefix, name)}")
    }

    /** A name the publishing step and a Markdown link both take: letters, digits and dashes. */
    fun fileName(prefix: String, name: String): String =
        "$prefix-$name".lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
            .trim('-')
            .take(MAX_NAME_LENGTH) + ".png"

    /** Runs [command] as the shell and waits for it: the descriptor ends when the command does. */
    private fun shell(command: String) {
        val descriptor: ParcelFileDescriptor =
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        FileInputStream(descriptor.fileDescriptor).use { input -> input.readBytes() }
        descriptor.close()
    }

    private const val MAX_NAME_LENGTH = 120
}
