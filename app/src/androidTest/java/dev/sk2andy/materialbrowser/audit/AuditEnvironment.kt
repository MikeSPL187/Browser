package dev.sk2andy.materialbrowser.audit

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.StartupAddressFocusMode
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.data.ReleaseNotesStore

/** The device's shape: the window size class every screen adapts to. */
enum class AuditLayout(val label: String) {
    PhonePortrait("phone"),

    /** The phone turned on its side: a short, wide window with the bars on a side edge. */
    PhoneLandscape("phone landscape"),

    /** A tablet held upright: a window wide enough for the expanded size class. */
    Tablet("tablet"),
}

/** The settings a real phone may have that change how every screen is laid out. */
data class AuditConfig(
    val languageTag: String,
    val dark: Boolean,
    val fontScale: Float,
    val layout: AuditLayout = AuditLayout.PhonePortrait,
) {
    val name: String =
        "$languageTag, ${if (dark) "dark" else "light"}, ${(fontScale * 100).toInt()}% font, " +
            layout.label

    companion object {
        val EnglishLight = AuditConfig("en-US", dark = false, fontScale = 1f)
        val RussianDark = AuditConfig("ru-RU", dark = true, fontScale = 1f)
        val EnglishDarkLargeFont = AuditConfig("en-US", dark = true, fontScale = 2f)
        val RussianLightLargeFont = AuditConfig("ru-RU", dark = false, fontScale = 2f)
        val RussianLightLandscape =
            AuditConfig("ru-RU", dark = false, fontScale = 1f, layout = AuditLayout.PhoneLandscape)
        val EnglishDarkTablet =
            AuditConfig("en-US", dark = true, fontScale = 1f, layout = AuditLayout.Tablet)

        /** Every screen is checked in each of these. */
        val All = listOf(
            EnglishLight,
            RussianDark,
            EnglishDarkLargeFont,
            RussianLightLargeFont,
            RussianLightLandscape,
            EnglishDarkTablet,
        )
    }
}

/** Puts the device and the app into an [AuditConfig] before launch, and back afterwards. */
internal object AuditEnvironment {
    /** [firstRun]: a fresh install that still shows the welcome and setup screens. */
    fun enter(context: Context, config: AuditConfig, firstRun: Boolean = false) {
        context.getSharedPreferences("browser_session", Context.MODE_PRIVATE).edit().clear().commit()
        setFontScale(config.fontScale)
        setLayout(config.layout)
        setAppLanguage(context, config.languageTag)
        if (firstRun) {
            // A fresh install has written nothing yet: any saved setting reads as an update and
            // skips the welcome. The test runner marks the welcome done for every test, so undo
            // that the way the onboarding tests do. The theme comes from the system.
            listOf(GestureOnboardingStore.PREFERENCES_NAME, ReleaseNotesStore.PREFERENCES_NAME)
                .forEach { name ->
                    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
                }
            context.getSharedPreferences(GestureOnboardingStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(GestureOnboardingStore.KEY_HAS_STARTED, true)
                .commit()
            shell("cmd uimode night ${if (config.dark) "yes" else "no"}")
            return
        }
        GestureOnboardingStore(context).markCompleted()
        BrowserSessionStore(context).apply {
            saveAppearanceSettings(
                AppearanceSettings(
                    appearanceMode = if (config.dark) BrowserAppearanceMode.Dark else BrowserAppearanceMode.Light,
                ),
            )
            // The walk opens the address editor itself, as a tap would.
            saveStartupAddressFocusMode(StartupAddressFocusMode.Never)
        }
    }

    fun reset(context: Context) {
        shell("cmd uimode night no")
        setFontScale(1f)
        setLayout(AuditLayout.PhonePortrait)
        setAppLanguage(context, null)
        context.getSharedPreferences("browser_session", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun setFontScale(scale: Float) {
        shell("settings put system font_scale $scale")
    }

    private fun setLayout(layout: AuditLayout) {
        // The rotation holds only with auto-rotate off; a fixed one is what a test needs anyway.
        shell("settings put system accelerometer_rotation 0")
        shell("settings put system user_rotation ${if (layout == AuditLayout.PhoneLandscape) 1 else 0}")
        if (layout == AuditLayout.Tablet) {
            shell("wm size $TABLET_SIZE")
            shell("wm density $TABLET_DENSITY")
        } else {
            shell("wm size reset")
            shell("wm density reset")
        }
    }

    private fun setAppLanguage(context: Context, languageTag: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val locales = languageTag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            context.getSystemService(LocaleManager::class.java).applicationLocales = locales
        }
    }

    /** 900 by 1440 dp: wider than the 840 dp where the expanded window size class starts. */
    private const val TABLET_SIZE = "1800x2880"
    private const val TABLET_DENSITY = 320

    private fun shell(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
